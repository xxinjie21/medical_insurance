package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.service.ISettleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结算管理 Controller - 处理医保结算业务
 * 权限控制：仅限医院角色访问
 * 核心功能：
 * - 就诊结算：计算报销金额和自付金额
 * - 结算查询：查看结算详情
 */
@RestController
@RequestMapping("/settle")
@RequiredArgsConstructor
@Permission(Role.HOSPITAL) // 只有医院角色可以访问
public class SettleController {

    private final ISettleService settleService;

    /**
     * 就诊医保结算
     * 根据费用明细计算报销金额（甲类 100%、乙类 80%、自费 0%）
     * 使用分布式锁防止重复结算
     */
    @PostMapping("/calculate/{visitId}")
    public Result calculate(@PathVariable Long visitId) {
        return settleService.calculate(visitId);
    }

    /**
     * 查询就诊的结算详情
     * 包含总费用、报销金额、自付金额等信息
     */
    @GetMapping("/detail/{visitId}")
    public Result getSettleDetail(@PathVariable Long visitId) {
        return settleService.getSettleDetail(visitId);
    }
}
