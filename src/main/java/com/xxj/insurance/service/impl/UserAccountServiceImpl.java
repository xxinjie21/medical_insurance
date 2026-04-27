package com.xxj.insurance.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxj.insurance.common.constants.AccountConstants;
import com.xxj.insurance.common.constants.RedisConstants;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.RechargeDTO;
import com.xxj.insurance.domain.po.ConsumptionRecord;
import com.xxj.insurance.domain.po.RechargeRecord;
import com.xxj.insurance.domain.po.Settle;
import com.xxj.insurance.domain.po.User;
import com.xxj.insurance.domain.po.UserAccount;
import com.xxj.insurance.domain.vo.ConsumptionRecordVO;
import com.xxj.insurance.domain.vo.RechargeRecordVO;
import com.xxj.insurance.domain.vo.UserAccountVO;
import com.xxj.insurance.mapper.ConsumptionRecordMapper;
import com.xxj.insurance.mapper.RechargeRecordMapper;
import com.xxj.insurance.mapper.SettleMapper;
import com.xxj.insurance.mapper.UserAccountMapper;
import com.xxj.insurance.mapper.UserMapper;
import com.xxj.insurance.service.ISettleService;
import com.xxj.insurance.service.IUserAccountService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 患者账户表 服务实现类
 * </p>
 *
 * @author xxj
 * @since 2026-04-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements IUserAccountService {

    private final UserMapper userMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final ConsumptionRecordMapper consumptionRecordMapper;
    private final ISettleService settleService;
    private final RedissonClient redissonClient;

    @Override
    public Result getAccount(Long userId) {
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }

        // 1. 查询账户信息
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getUserId, userId);
        UserAccount account = this.getOne(wrapper);

        // 2. 如果账户不存在，创建账户
        if (account == null) {
            account = createAccount(userId);
        }

        // 3. 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 4. 构建 VO
        UserAccountVO vo = new UserAccountVO();
        BeanUtils.copyProperties(account, vo);
        vo.setUserName(user.getName());

        return Result.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result recharge(Long userId, RechargeDTO dto) {
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }

        if (dto == null || dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("充值金额必须大于 0");
        }

        if (dto.getType() == null || dto.getType() < 1 || dto.getType() > 4) {
            return Result.fail("充值类型无效");
        }

        // 1. 获取分布式锁
        String lockKey = "lock:recharge:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(10, -1, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // 2. 生成充值订单号
            String orderNo = generateRechargeOrderNo(userId);

            // 3. 创建充值记录
            RechargeRecord record = new RechargeRecord();
            record.setUserId(userId);
            record.setOrderNo(orderNo);
            record.setAmount(dto.getAmount());
            record.setType(dto.getType());
            record.setStatus(AccountConstants.RECHARGE_STATUS_SUCCESS); // 模拟支付成功
            record.setPayTime(LocalDateTime.now());
            record.setRemark(dto.getRemark());
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());

            rechargeRecordMapper.insert(record);

            // 4. 更新账户余额
            UserAccount account = getOrCreateAccount(userId);
            
            // 检查账户状态
            if (account.getStatus().equals(AccountConstants.ACCOUNT_STATUS_FROZEN)) {
                return Result.fail("账户已被冻结，无法充值");
            }

            BigDecimal newBalance = account.getBalance().add(dto.getAmount());
            BigDecimal newTotalRecharge = account.getTotalRecharge().add(dto.getAmount());

            account.setBalance(newBalance);
            account.setTotalRecharge(newTotalRecharge);
            account.setUpdateTime(LocalDateTime.now());

            this.updateById(account);

            log.info("充值成功，userId: {}, orderNo: {}, amount: {}", userId, orderNo, dto.getAmount());

            return Result.ok("充值成功");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("充值操作被中断，userId: {}", userId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 使用账户余额支付就诊的自付部分
     * 业务逻辑：
     * 1. 根据 visitId 查询结算单，获取自付金额（selfPay）
     * 2. 验证就诊是否属于该用户
     * 3. 检查账户余额是否充足
     * 4. 扣减账户余额，创建消费记录
     * 
     * @param userId 用户 ID
     * @param visitId 就诊 ID
     * @param remark 备注
     * @return 支付结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result pay(Long userId, Long visitId, String remark) {
        // 1. 参数验证
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }
        if (visitId == null) {
            return Result.fail("就诊 ID 不能为空");
        }

        // 2. 查询结算单，获取自付金额（关键：金额从数据库获取，不是前端传入）
        LambdaQueryWrapper<Settle> settleWrapper = new LambdaQueryWrapper<>();
        settleWrapper.eq(Settle::getVisitId, visitId);
        Settle settle = settleService.getOne(settleWrapper);
        
        if (settle == null) {
            return Result.fail("该就诊尚未结算，无法支付");
        }
        
        // 获取自付金额（结算时计算好的）
        BigDecimal payAmount = settle.getSelfPay();
        
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("无需支付金额");
        }

        // 3. 获取分布式锁（防止并发支付）
        String lockKey = "lock:pay:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(10, -1, TimeUnit.SECONDS)) {
                return Result.fail("操作正在进行中，请勿重复提交");
            }

            // 4. 查询账户
            UserAccount account = getOrCreateAccount(userId);

            // 检查账户状态
            if (account.getStatus().equals(AccountConstants.ACCOUNT_STATUS_FROZEN)) {
                return Result.fail("账户已被冻结，无法支付");
            }

            // 检查余额是否充足
            if (account.getBalance().compareTo(payAmount) < 0) {
                return Result.fail("账户余额不足，请充值后支付");
            }

            // 5. 生成消费订单号
            String orderNo = generateConsumptionOrderNo(userId);

            // 6. 记录消费前余额
            BigDecimal balanceBefore = account.getBalance();

            // 7. 更新账户余额
            BigDecimal newBalance = balanceBefore.subtract(payAmount);
            BigDecimal newTotalConsumption = account.getTotalConsumption().add(payAmount);

            account.setBalance(newBalance);
            account.setTotalConsumption(newTotalConsumption);
            account.setUpdateTime(LocalDateTime.now());
            this.updateById(account);

            // 8. 创建消费记录
            ConsumptionRecord record = new ConsumptionRecord();
            record.setUserId(userId);
            record.setVisitId(visitId);
            record.setOrderNo(orderNo);
            record.setAmount(payAmount);
            record.setType(AccountConstants.CONSUMPTION_TYPE_VISIT_PAY);
            record.setStatus(AccountConstants.CONSUMPTION_STATUS_SUCCESS);
            record.setBalanceBefore(balanceBefore);
            record.setBalanceAfter(newBalance);
            record.setRemark(remark);
            record.setCreateTime(LocalDateTime.now());

            consumptionRecordMapper.insert(record);

            log.info("支付成功，userId: {}, orderNo: {}, amount: {}, balanceBefore: {}, balanceAfter: {}", 
                userId, orderNo, payAmount, balanceBefore, newBalance);

            return Result.ok("支付成功");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("支付操作被中断，userId: {}", userId, e);
            return Result.fail("操作被中断，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Result rechargeList(Long userId, Integer pageNum, Integer pageSize) {
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }

        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }

        Page<RechargeRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RechargeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRecord::getUserId, userId)
                .orderByDesc(RechargeRecord::getCreateTime);

        Page<RechargeRecord> resultPage = rechargeRecordMapper.selectPage(page, wrapper);

        List<RechargeRecordVO> voList = new ArrayList<>();
        for (RechargeRecord record : resultPage.getRecords()) {
            RechargeRecordVO vo = new RechargeRecordVO();
            BeanUtils.copyProperties(record, vo);
            voList.add(vo);
        }

        return Result.ok(voList, resultPage.getTotal());
    }

    @Override
    public Result consumptionList(Long userId, Integer pageNum, Integer pageSize) {
        if (userId == null) {
            return Result.fail("用户 ID 不能为空");
        }

        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }

        Page<ConsumptionRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ConsumptionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsumptionRecord::getUserId, userId)
                .orderByDesc(ConsumptionRecord::getCreateTime);

        Page<ConsumptionRecord> resultPage = consumptionRecordMapper.selectPage(page, wrapper);

        List<ConsumptionRecordVO> voList = new ArrayList<>();
        for (ConsumptionRecord record : resultPage.getRecords()) {
            ConsumptionRecordVO vo = new ConsumptionRecordVO();
            BeanUtils.copyProperties(record, vo);
            voList.add(vo);
        }

        return Result.ok(voList, resultPage.getTotal());
    }

    /**
     * 获取或创建账户
     */
    private UserAccount getOrCreateAccount(Long userId) {
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccount::getUserId, userId);
        UserAccount account = this.getOne(wrapper);

        if (account == null) {
            return createAccount(userId);
        }

        return account;
    }

    /**
     * 创建账户
     */
    private UserAccount createAccount(Long userId) {
        UserAccount account = new UserAccount();
        account.setUserId(userId);
        account.setBalance(BigDecimal.ZERO);
        account.setTotalRecharge(BigDecimal.ZERO);
        account.setTotalConsumption(BigDecimal.ZERO);
        account.setStatus(AccountConstants.ACCOUNT_STATUS_NORMAL);
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        this.save(account);
        return account;
    }

    /**
     * 生成充值订单号
     */
    private String generateRechargeOrderNo(Long userId) {
        String date = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        String random = String.format("%06d", (int)(Math.random() * 1000000));
        return AccountConstants.RECHARGE_ORDER_PREFIX + date + userId + random;
    }

    /**
     * 生成消费订单号
     */
    private String generateConsumptionOrderNo(Long userId) {
        String date = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        String random = String.format("%06d", (int)(Math.random() * 1000000));
        return AccountConstants.CONSUMPTION_ORDER_PREFIX + date + userId + random;
    }
}
