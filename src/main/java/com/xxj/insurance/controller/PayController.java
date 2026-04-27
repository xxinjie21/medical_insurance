package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.service.IPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基金拨付管理 Controller - 处理医保局对医院申报批次的基金拨付业务
 * 权限控制：仅限医保局角色访问
 * 核心功能：
 * - 拨付批次款项
 * - 查询拨付信息
 */
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
@Permission(Role.MEDICAL) // 只有医保局角色可以访问
public class PayController {

    private final IPayService payService;

    /**
     * 医保局对申报批次进行基金拨付
     * 拨付后批次状态变为"已完成"
     * 使用分布式锁防止重复拨付
     */
    @PostMapping("/pay-batch/{batchId}")
    public Result payBatch(@PathVariable Long batchId) {
        return payService.payBatch(batchId);
    }

    /**
     * 根据批次 ID 查询拨付信息
     * 查看批次是否已拨付及拨付详情
     */
    @GetMapping("/by-batch/{batchId}")
    public Result getPayByBatchId(@PathVariable Long batchId) {
        return payService.getPayByBatchId(batchId);
    }
}
