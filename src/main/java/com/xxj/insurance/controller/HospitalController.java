package com.xxj.insurance.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.HospitalDTO;
import com.xxj.insurance.domain.po.Hospital;
import com.xxj.insurance.service.IHospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 医院管理 Controller - 处理医院注册、查询等业务
 * 核心功能：
 * - 医院注册（签约）
 * - 医院列表查询（患者/医保局可用）
 * - 医院查询本院患者
 */
@RestController
@RequestMapping("/hospital")
@RequiredArgsConstructor
public class HospitalController {

    private final IHospitalService hospitalService;

    /**
     * 医院注册（签约）
     * 创建医院机构账号
     */
    @PostMapping("/sign")
    public Result sign(@RequestBody HospitalDTO  hospitalDTO) {
        return  hospitalService.sign(hospitalDTO);
    }

    /**
     * 获取所有医院列表（分页）
     * 患者和医保局可以查询，用于选择医院
     */
    @GetMapping("/list")
    public Result listAll(PageDTO pageDTO) {
        // 1. 处理分页参数默认值
        if (pageDTO == null || pageDTO.getPageNum() == null || pageDTO.getPageSize() == null) {
            pageDTO = new PageDTO(1, 10);
        }
        
        // 2. 构建分页对象
        Page<Hospital> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        // 3. 调用 service 分页查询
        Page<Hospital> hospitalPage = hospitalService.page(page);
        // 4. 返回统一结果
        return Result.ok(hospitalPage);
    }

    /**
     * 医院查询本院的患者列表
     * 通过请求头传入 hospitalId，查询在该医院就诊的患者
     */
    @GetMapping("/patient/list")
    public Result listMyPatient(
            @RequestHeader(value = "hospitalId", required = true) Long hospitalId, PageDTO pageDTO) {
        return hospitalService.listMyPatient(hospitalId, pageDTO);
    }

}
