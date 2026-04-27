package com.xxj.insurance.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxj.insurance.common.constants.ReimburseConstants;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Batch;
import com.xxj.insurance.domain.po.BatchItem;
import com.xxj.insurance.domain.po.Settle;
import com.xxj.insurance.domain.vo.BatchVO;
import com.xxj.insurance.mapper.BatchMapper;
import com.xxj.insurance.service.IBatchItemService;
import com.xxj.insurance.service.IBatchService;
import com.xxj.insurance.service.ISettleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchServiceImpl extends ServiceImpl<BatchMapper, Batch> implements IBatchService {

    private final IBatchItemService batchItemService;
    private final ISettleService settleService;
    private final RedissonClient redissonClient;

    /**
     * 创建申报批次
     * 加了：分布式锁 + 双层幂等（事务外+事务内）
     */
    @Override
    public Result createBatch(Long hospitalId) {
        // 参数校验
        if (hospitalId == null) {
            return Result.fail("医院 ID 不能为空");
        }

        // 分布式锁 key：按医院ID加锁，防止同一医院并发创建批次
        String lockKey = "lock:batch:create:" + hospitalId;
        RLock lock = redissonClient.getLock(lockKey);

        // 幂等 key：防止重复提交创建
        String idempotentKey = "idempotent:batch:create:" + hospitalId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        try {
            // 尝试获取锁，等待10秒，锁自动续期
            if (!lock.tryLock(10, -1, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // ====================== 【第一次幂等校验：事务外】 ======================
            // 快速拦截重复请求，避免进入事务浪费资源
            if (idempotentBucket.isExists()) {
                return Result.fail("请勿重复创建批次");
            }

            // 进入事务执行真正的创建逻辑
            return executeCreateBatchWithTransaction(hospitalId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("创建批次被中断", e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            // 确保锁一定释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 事务内：创建批次
     * 第二次幂等校验 + 数据库操作
     */
    @Transactional(rollbackFor = Exception.class)
    public Result executeCreateBatchWithTransaction(Long hospitalId) {
        String idempotentKey = "idempotent:batch:create:" + hospitalId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        // ====================== 【第二次幂等校验：事务内】 ======================
        // 极端并发兜底，绝对防止重复插入
        if (idempotentBucket.isExists()) {
            return Result.fail("请勿重复创建批次");
        }

        // 生成唯一批次号：时间 + 医院ID后四位 + 随机数
        String batchNo = DateUtil.format(new Date(), "yyyyMMddHHmmss")
                + String.format("%04d", hospitalId % 10000)
                + String.format("%06d", (int) (Math.random() * 1000000));

        // 构建批次对象
        Batch batch = new Batch();
        batch.setHospitalId(hospitalId);
        batch.setBatchNo(batchNo);
        batch.setSettleCnt(0);
        batch.setTotalAmt(BigDecimal.ZERO);
        batch.setStatus(ReimburseConstants.BATCH_STATUS_PENDING);
        batch.setCreateTime(LocalDateTime.now());

        // 保存批次
        this.save(batch);

        // 写入幂等标记，2小时内不能重复创建
        idempotentBucket.set("1", 2, TimeUnit.HOURS);

        log.info("创建批次成功，批次号：{}", batchNo);
        return Result.ok(batch);
    }

    /**
     * 添加结算单到申报批次
     * 双层幂等 + 分布式锁 + 事务
     */
    @Override
    public Result addSettleToBatch(Long batchId, Long settleId) {
        // 参数校验
        if (batchId == null || settleId == null) {
            return Result.fail("批次 ID 和结算单 ID 不能为空");
        }

        // 分布式锁：按批次加锁，防止同一批次并发添加
        String lockKey = "lock:batch:add:" + batchId;
        RLock lock = redissonClient.getLock(lockKey);

        // 幂等key：一个结算单只能添加一次
        String idempotentKey = "idempotent:batch:item:" + settleId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        try {
            // 获取分布式锁
            if (!lock.tryLock(10, -1, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // ====================== 【第一次幂等校验：事务外】 ======================
            if (idempotentBucket.isExists()) {
                return Result.fail("该结算单已添加到批次，请勿重复添加");
            }

            // 进入事务执行
            return executeAddSettleToBatchWithTransaction(batchId, settleId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("添加结算单被中断", e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 事务内：添加结算单到批次
     * 第二次幂等 + 数据校验 + 状态更新
     */
    @Transactional(rollbackFor = Exception.class)
    public Result executeAddSettleToBatchWithTransaction(Long batchId, Long settleId) {
        // 幂等key
        String idempotentKey = "idempotent:batch:item:" + settleId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        // ====================== 【第二次幂等校验：事务内】 ======================
        if (idempotentBucket.isExists()) {
            return Result.fail("该结算单已添加到批次，请勿重复添加");
        }

        // 查询批次是否存在
        Batch batch = this.getById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }

        // 只有待申报状态才能添加
        if (!ReimburseConstants.BATCH_STATUS_PENDING.equals(batch.getStatus())) {
            return Result.fail("批次状态不允许添加结算单");
        }

        // 查询结算单是否存在
        Settle settle = settleService.getById(settleId);
        if (settle == null) {
            return Result.fail("结算单不存在");
        }

        // 数据库兜底校验：该结算单是否已被添加到任何批次
        LambdaQueryWrapper<BatchItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(BatchItem::getSettleId, settleId);
        BatchItem existItem = batchItemService.getOne(itemWrapper);
        if (existItem != null) {
            // 补写幂等标记
            idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
            return Result.fail("该结算单已添加到其他批次");
        }

        // 生成批次明细
        BatchItem batchItem = new BatchItem();
        batchItem.setBatchId(batchId);
        batchItem.setSettleId(settleId);
        batchItem.setAudit(ReimburseConstants.AUDIT_PASS);
        batchItemService.save(batchItem);

        // 写入幂等标记
        idempotentBucket.set("1", 3600, TimeUnit.SECONDS);

        // 更新批次：数量+1，总金额累加
        int newSettleCnt = batch.getSettleCnt() + 1;
        BigDecimal newTotalAmt = batch.getTotalAmt().add(settle.getTotal());

        batch.setSettleCnt(newSettleCnt);
        batch.setTotalAmt(newTotalAmt);
        this.updateById(batch);

        // 更新结算单状态为：已申报
        settle.setStatus(ReimburseConstants.SETTLE_STATUS_DECLARED);
        settleService.updateById(settle);

        log.info("添加结算单到批次成功，batchId:{} settleId:{}", batchId, settleId);
        return Result.ok("添加成功");
    }

    /**
     * 查询批次详情（基本信息 + 明细列表）
     */
    @Override
    public Result getBatchDetail(Long batchId) {
        if (batchId == null) {
            return Result.fail("批次 ID 不能为空");
        }

        // 1. 查询数据库 PO
        Batch batch = this.getById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }

        // 2. 查询明细
        LambdaQueryWrapper<BatchItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BatchItem::getBatchId, batchId);
        List<BatchItem> items = batchItemService.list(wrapper);

        // 3. 封装成 VO（标准！）
        BatchVO vo = new BatchVO();
        BeanUtils.copyProperties(batch, vo); // 复制属性
        vo.setBatchItems(items);            // 放入明细

        // 4. 返回 VO
        return Result.ok(vo);
    }
}