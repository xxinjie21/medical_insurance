package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.domain.dto.FeeAddDTO;
import com.xxj.insurance.service.IFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 费用管理 Controller - 处理就诊费用明细业务
 * 权限控制：仅限医院角色访问
 * 核心功能：
 * - 批量添加费用明细
 * - 查询费用明细列表
 */
@RestController
@RequestMapping("/fee")
@RequiredArgsConstructor
@Permission(Role.HOSPITAL) // 只有医院角色可以访问
public class FeeController {

    private final IFeeService feeService;

    /**
     * 医院为就诊记录批量添加费用明细
     * 支持药品费、诊疗费、材料费等多种类型
     * 自动计算总价（单价 × 数量）
     */
    @PostMapping("/batch/add")
    public Result batchAdd(@RequestBody List<FeeAddDTO> dtoList) {
        return feeService.batchAdd(dtoList);
    }

    /**
     * 根据就诊 ID 查询费用明细列表
     * 用于查看就诊的所有费用
     */
    @GetMapping("/listByVisitId")
    public Result listByVisitId(@RequestParam Long visitId) {
        return feeService.listByVisitId(visitId);
    }
}
