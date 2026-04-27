package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import com.xxj.insurance.domain.dto.RechargeDTO;
import com.xxj.insurance.service.IUserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 患者账户管理 Controller - 处理患者充值、支付、查询等资金相关业务
 * 安全控制：
 * 1. 所有接口仅限患者角色访问（@Permission(Role.PATIENT)）
 * 2. userId 从 Token 获取，不允许前端传入，防止为他人操作
 * 3. 支付金额从数据库结算单获取，不允许前端传入，防止篡改
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@Permission(Role.PATIENT) // 只有患者角色可以访问
public class UserAccountController {

    private final IUserAccountService userAccountService;

    /**
     * 获取患者账户信息
     * 包含余额、充值总额、消费总额
     * 从当前登录用户获取 ID，确保只能查询自己的账户
     */
    @GetMapping("/get")
    public Result getAccount() {
        // 从当前登录用户拿自己的 ID，绝对不能让前端传
        Long userId = UserHolder.getUserId();
        return userAccountService.getAccount(userId);
    }

    /**
     * 患者账户充值
     * 支持微信、支付宝、银行卡、现金四种充值方式
     * 使用分布式锁防止重复充值
     */
    @PostMapping("/recharge")
    public Result recharge(@RequestBody RechargeDTO dto) {
        // 从 Token 获取当前登录用户 ID（关键！防止给他人充值）
        Long userId = UserHolder.getUserId();

        // 只能给自己充值
        return userAccountService.recharge(userId, dto);
    }

    /**
     * 账户余额支付 - 支付就诊自付部分
     * 业务逻辑：
     * 1. 根据 visitId 查询结算单
     * 2. 获取结算单中的自付金额（selfPay）
     * 3. 使用账户余额支付该金额
     * 
     * 安全控制：
     * - userId 从 Token 获取，只能给自己支付
     * - 金额从结算单获取，不允许前端传入
     * - 使用分布式锁防止超额支付
     */
    @PostMapping("/pay")
    public Result pay(@RequestParam Long visitId,
                     @RequestParam(required = false) String remark) {
        // 从当前登录用户获取 ID（只能给自己支付）
        Long userId = UserHolder.getUserId();
        
        // 金额从结算单中获取，不允许前端传入（关键安全控制！）
        return userAccountService.pay(userId, visitId, remark);
    }

    /**
     * 查询患者充值记录列表（分页）
     */
    @GetMapping("/recharge/list")
    public Result rechargeList(@RequestParam Long userId,
                               @RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return userAccountService.rechargeList(userId, pageNum, pageSize);
    }

    /**
     * 查询患者消费记录列表（分页）
     */
    @GetMapping("/consumption/list")
    public Result consumptionList(@RequestParam Long userId,
                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return userAccountService.consumptionList(userId, pageNum, pageSize);
    }
}
