package com.xxj.insurance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.FeeAddDTO;
import com.xxj.insurance.domain.po.Fee;
import com.xxj.insurance.domain.vo.FeeVO;
import com.xxj.insurance.mapper.FeeMapper;
import com.xxj.insurance.service.IFeeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeServiceImpl extends ServiceImpl<FeeMapper, Fee> implements IFeeService {

    private final RedissonClient redissonClient;

    // ==================== 批量添加费用 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result batchAdd(List<FeeAddDTO> dtoList) {
        if (dtoList.isEmpty()) {
            return Result.fail("费用明细不能为空");
        }

        // 取第一个的 visitId 加锁
        Long visitId = dtoList.get(0).getVisitId();
        String lockKey = "lock:fee:" + visitId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 锁 10 秒，防止重复添加费用
            if (!lock.tryLock(10, -1, TimeUnit.SECONDS)) {
                return Result.fail("正在处理费用，请稍后");
            }

            // 转换 & 校验
            List<Fee> feeList = dtoList.stream().map(dto -> {
                // 校验
                if (dto.getVisitId() == null) throw new RuntimeException("就诊ID不能为空");
                if (dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("单价必须大于0");
                if (dto.getNum() <= 0) throw new RuntimeException("数量必须大于0");

                // 计算总价
                BigDecimal total = dto.getPrice().multiply(BigDecimal.valueOf(dto.getNum()));

                Fee fee = new Fee();
                BeanUtils.copyProperties(dto, fee);
                fee.setTotal(total);
                return fee;
            }).collect(Collectors.toList());

            // 批量插入
            saveBatch(feeList);

            // 插入后重新查询（解决ID不返回问题）
            List<Fee> newList = lambdaQuery().eq(Fee::getVisitId, visitId).list();

            // 转VO
            List<FeeVO> voList = newList.stream().map(fee -> {
                FeeVO vo = new FeeVO();
                BeanUtils.copyProperties(fee, vo);
                return vo;
            }).collect(Collectors.toList());

            return Result.ok(voList);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.fail("操作中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ==================== 根据就诊ID查费用（标准版） ====================
    @Override
    public Result listByVisitId(Long visitId) {
        if (visitId == null) {
            return Result.fail("就诊ID不能为空");
        }

        List<Fee> feeList = lambdaQuery()
                .eq(Fee::getVisitId, visitId)
                .list();

        List<FeeVO> voList = feeList.stream().map(fee -> {
            FeeVO vo = new FeeVO();
            BeanUtils.copyProperties(fee, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(voList);
    }
}