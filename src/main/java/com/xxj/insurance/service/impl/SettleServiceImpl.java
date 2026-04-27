package com.xxj.insurance.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxj.insurance.common.constants.RedisConstants;
import com.xxj.insurance.common.constants.ReimburseConstants;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Fee;
import com.xxj.insurance.domain.po.Settle;
import com.xxj.insurance.domain.po.Visit;
import com.xxj.insurance.mapper.SettleMapper;
import com.xxj.insurance.service.IFeeService;
import com.xxj.insurance.service.ISettleService;
import com.xxj.insurance.service.IVisitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 结算服务实现类
 * 核心功能：保险费用结算、防止重复结算、高并发安全处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettleServiceImpl extends ServiceImpl<SettleMapper, Settle> implements ISettleService {

    // 费用服务
    private final IFeeService feeService;
    // 就诊服务
    private final IVisitService visitService;
    // Redisson 客户端（用于分布式锁、Redis缓存、幂等控制）
    private final RedissonClient redissonClient;

    /**
     * 结算入口（外层方法：处理锁 + 幂等）
     * @param visitId 就诊ID
     * @return 结算结果
     */
    @Override
    public Result calculate(Long visitId) {
        // 分布式锁Key：保证同一个就诊ID同一时间只能一个线程结算
        String lockKey = "lock:settle:" + visitId;
        RLock lock = redissonClient.getLock(lockKey);

        // 幂等Key：防止重复提交/重复结算
        String idempotentKey = "idempotent:settle:visit:" + visitId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        try {
            // ===================== 第一层拦截：Redis快速判断是否已结算 =====================
            // 如果幂等标记已存在，直接返回，不抢锁，提升性能
            if (idempotentBucket.isExists()) {
                return Result.fail("该就诊已结算，请勿重复提交");
            }

            // ===================== 抢分布式锁 =====================
            // 等待锁最多10秒，拿到锁后自动续期（-1=看门狗机制）
            if (!lock.tryLock(10, -1, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // ===================== 锁内执行业务（带事务） =====================
            return executeCalculateWithTransaction(visitId, idempotentBucket);

        } catch (InterruptedException e) {
            // 线程中断异常处理
            Thread.currentThread().interrupt();
            log.error("结算操作被中断，就诊ID:{}", visitId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            // ===================== 释放锁（必须判断是否当前线程持有） =====================
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 真正的结算逻辑（带事务，在锁内执行）
     * @param visitId 就诊ID
     * @param idempotentBucket 幂等标记对象
     * @return 结算结果
     */
    @Transactional(rollbackFor = Exception.class) // 事务：任何异常都回滚数据库
    public Result executeCalculateWithTransaction(Long visitId, RBucket<String> idempotentBucket) {
        log.info("开始结算，就诊ID:{}", visitId);

        // ===================== 第二层拦截：锁内再次校验幂等（双重检查，绝对安全） =====================
        if (idempotentBucket.isExists()) {
            return Result.fail("该就诊已结算，请勿重复提交");
        }

        // ===================== 第三层拦截：查询数据库是否已结算 =====================
        LambdaQueryWrapper<Settle> settleWrapper = new LambdaQueryWrapper<>();
        settleWrapper.eq(Settle::getVisitId, visitId);
        Settle existSettle = this.getOne(settleWrapper);
        if (existSettle != null) {
            // 已结算 → 设置幂等标记，返回失败
            idempotentBucket.trySet("1", 3600, TimeUnit.SECONDS);
            return Result.fail("该就诊已结算，请勿重复结算");
        }

        // ===================== 查询就诊信息（带缓存） =====================
        Visit visit = getVisitWithCache(visitId);
        if (visit == null) {
            return Result.fail("就诊记录不存在");
        }

        // ===================== 查询该就诊下所有费用 =====================
        LambdaQueryWrapper<Fee> feeWrapper = new LambdaQueryWrapper<>();
        feeWrapper.eq(Fee::getVisitId, visitId);
        List<Fee> feeList = feeService.list(feeWrapper);

        if (feeList == null || feeList.isEmpty()) {
            return Result.fail("该就诊暂无费用明细");
        }

        // ===================== 费用计算（高精度，避免精度丢失） =====================
        BigDecimal totalAmount = BigDecimal.ZERO;        // 总费用
        BigDecimal reimburseAmountRaw = BigDecimal.ZERO;  // 报销金额（原始值，不保留小数）

        for (Fee fee : feeList) {
            // 累加总费用
            totalAmount = totalAmount.add(fee.getTotal());
            // 根据费用类型获取报销比例
            BigDecimal rate = getReimburseRate(fee.getType());
            // 计算单项报销金额
            BigDecimal reimburse = fee.getTotal().multiply(rate);
            // 累加总报销金额
            reimburseAmountRaw = reimburseAmountRaw.add(reimburse);
        }

        // 统一保留2位小数（四舍五入）
        BigDecimal reimburseAmount = reimburseAmountRaw.setScale(2, RoundingMode.HALF_UP);
        // 个人自付 = 总费用 - 报销金额
        BigDecimal selfPayAmount = totalAmount.subtract(reimburseAmount).setScale(2, RoundingMode.HALF_UP);

        // ===================== 保存结算记录 =====================
        Settle settle = new Settle();
        settle.setVisitId(visitId);
        settle.setHospitalId(visit.getHospitalId());
        settle.setTotal(totalAmount);            // 总金额
        settle.setReimburse(reimburseAmount);    // 报销金额
        settle.setSelfPay(selfPayAmount);        // 自付金额
        settle.setStatus(ReimburseConstants.SETTLE_STATUS_UNDECLARED); // 未申报状态
        settle.setCreateTime(LocalDateTime.now());
        this.save(settle);

        // ===================== 更新就诊状态为「已结算」 =====================
        visit.setStatus(ReimburseConstants.VISIT_STATUS_SETTLED);
        visitService.updateById(visit);

        // ===================== 设置幂等标记（核心：只有不存在才设置，绝对防重复） =====================
        idempotentBucket.trySet("1", 3600, TimeUnit.SECONDS);

        log.info("结算成功，就诊ID:{}, 结算ID:{}", visitId, settle.getId());
        return Result.ok(settle);
    }

    /**
     * 查询结算详情（带缓存）
     * @param visitId 就诊ID
     * @return 结算详情
     */
    @Override
    public Result getSettleDetail(Long visitId) {
        LambdaQueryWrapper<Settle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Settle::getVisitId, visitId);
        Settle settle = this.getOne(wrapper);

        if (settle == null) {
            return Result.fail("该就诊尚未结算");
        }

        // 直接返回，不搞缓存
        return Result.ok(settle);
    }

    /**
     * 根据费用类型获取报销比例
     * @param type 1甲类 2乙类 3自费
     * @return 比例
     */
    private BigDecimal getReimburseRate(Integer type) {
        if (type == null) return ReimburseConstants.CATEGORY_C_RATE;
        switch (type) {
            case 1: return ReimburseConstants.CATEGORY_A_RATE;  // 甲类
            case 2: return ReimburseConstants.CATEGORY_B_RATE;  // 乙类
            default: return ReimburseConstants.CATEGORY_C_RATE; // 自费/其他
        }
    }

    /**
     * 获取就诊信息（带Redis缓存）
     * @param visitId 就诊ID
     * @return 就诊对象
     */
    private Visit getVisitWithCache(Long visitId) {
        String cacheKey = RedisConstants.CACHE_VISIT_KEY + visitId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cacheJson = cacheBucket.get();

        // 缓存命中
        if (StrUtil.isNotBlank(cacheJson)) {
            log.info("从缓存获取就诊信息，visitId:{}", visitId);
            return JSON.parseObject(cacheJson, Visit.class);
        }

        // 缓存未命中 → 查询数据库
        Visit visit = visitService.getById(visitId);
        // 查询到则写入缓存
        if (visit != null) {
            cacheBucket.set(JSON.toJSONString(visit), RedisConstants.CACHE_VISIT_TTL, TimeUnit.MINUTES);
        }
        return visit;
    }
}