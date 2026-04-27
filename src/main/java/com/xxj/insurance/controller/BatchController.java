package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.service.IBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 批次申报管理 Controller - 处理医院向医保局申报结算单的业务
 * 权限控制：仅限医院角色访问
 * 核心功能：
 * - 创建申报批次
 * - 添加结算单到批次
 * - 查询批次详情
 */
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
@Permission(Role.HOSPITAL) // 只有医院角色可以访问
public class BatchController {

    private final IBatchService batchService;

    /**
     * 医院创建医保申报批次
     * 生成唯一的批次号，用于打包多个结算单
     */
    @PostMapping("/create/{hospitalId}")
    public Result createBatch(@PathVariable Long hospitalId) {
        return batchService.createBatch(hospitalId);
    }

    /**
     * 将结算单添加到申报批次
     * 一个结算单只能添加到一个批次中
     */
    @PostMapping("/add-settle")
    public Result addSettleToBatch(Long batchId, Long settleId) {
        return batchService.addSettleToBatch(batchId, settleId);
    }

    /**
     * 查询批次详情及包含的结算单列表
     */
    @GetMapping("/detail/{batchId}")
    public Result getBatchDetail(@PathVariable Long batchId) {
        return batchService.getBatchDetail(batchId);
    }
}
