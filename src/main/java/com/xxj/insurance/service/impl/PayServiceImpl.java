package com.xxj.insurance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxj.insurance.common.constants.ReimburseConstants;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Batch;
import com.xxj.insurance.domain.po.Pay;
import com.xxj.insurance.mapper.PayMapper;
import com.xxj.insurance.service.IBatchService;
import com.xxj.insurance.service.IPayService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl extends ServiceImpl<PayMapper, Pay> implements IPayService {

    private final IBatchService batchService;
    private final RedissonClient redissonClient;

    /**
     * 拨付批次款项
     * 【双层幂等 + 分布式锁】金钱操作最高级别安全
     */
    @Override
    public Result payBatch(Long batchId) {
        // 1. 参数验证
        if (batchId == null) {
            return Result.fail("批次 ID 不能为空");
        }

        // 分布式锁
        String lockKey = "lock:pay:batch:" + batchId;
        RLock lock = redissonClient.getLock(lockKey);

        // ====================== 幂等KEY（拨款只能一次） ======================
        String idempotentKey = "idempotent:pay:batch:" + batchId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        try {
            if (!lock.tryLock(10, -1, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // ====================== 【第一次幂等：事务外】 ======================
            if (idempotentBucket.isExists()) {
                return Result.fail("该批次已拨付，请勿重复操作");
            }

            // 进入事务
            return executePayBatchWithTransaction(batchId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("拨付操作被中断，批次 ID: {}", batchId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行拨付业务逻辑（事务内）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result executePayBatchWithTransaction(Long batchId) {
        String idempotentKey = "idempotent:pay:batch:" + batchId;
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);

        // ====================== 【第二次幂等：事务内】 ======================
        if (idempotentBucket.isExists()) {
            return Result.fail("该批次已拨付，请勿重复拨付");
        }

        log.info("开始拨付批次，批次 ID: {}", batchId);

        // 数据库兜底校验
        LambdaQueryWrapper<Pay> payWrapper = new LambdaQueryWrapper<>();
        payWrapper.eq(Pay::getBatchId, batchId);
        Pay existPay = this.getOne(payWrapper);
        if (existPay != null) {
            idempotentBucket.set("1", 3600, TimeUnit.SECONDS);
            return Result.fail("该批次已拨付，请勿重复拨付");
        }

        // 查询批次
        Batch batch = batchService.getById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }

        // 创建拨付记录
        Pay pay = new Pay();
        pay.setBatchId(batchId);
        pay.setHospitalId(batch.getHospitalId());
        pay.setAmount(batch.getTotalAmt());
        pay.setStatus(ReimburseConstants.PAY_STATUS_PAID);
        pay.setPayTime(LocalDateTime.now());
        this.save(pay);

        // 更新批次状态为已完成
        batch.setStatus(ReimburseConstants.BATCH_STATUS_COMPLETED);
        batchService.updateById(batch);

        // 写入幂等标记
        idempotentBucket.set("1", 3600, TimeUnit.SECONDS);

        log.info("拨付成功，批次 ID: {}, 拨付 ID: {}", batchId, pay.getId());
        return Result.ok(pay);
    }

    /**
     * 根据批次 ID 查询拨付信息
     */
    @Override
    public Result getPayByBatchId(Long batchId) {
        if (batchId == null) {
            return Result.fail("批次 ID 不能为空");
        }

        LambdaQueryWrapper<Pay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pay::getBatchId, batchId);
        Pay pay = this.getOne(wrapper);

        if (pay == null) {
            return Result.fail("该批次尚未拨付");
        }

        return Result.ok(pay);
    }
}