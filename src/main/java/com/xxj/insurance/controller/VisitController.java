package com.xxj.insurance.controller;


import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.domain.dto.VisitAddDTO;
import com.xxj.insurance.service.IVisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 就诊管理 Controller - 处理患者就诊相关业务
 * 权限控制：
 * - 默认需要医院角色访问
 * - 患者查询自己的就诊记录需要患者角色
 */
@RestController
@RequestMapping("/visit")
@RequiredArgsConstructor
@Permission(Role.HOSPITAL) // 默认医院角色，具体方法可以单独指定
public class VisitController {

    private  final IVisitService visitService;

    /**
     * 医院为患者新增就诊记录
     * 需要校验患者和医院是否存在
     */
    @PostMapping("/add")
    public Result add(@RequestBody VisitAddDTO dto) {
        return visitService.add(dto);
    }

    /**
     * 患者查询自己的就诊记录（分页）
     * 使用 @Permission(Role.PATIENT) 确保只有患者本人可以查询
     * 从 Token 获取 userId，确保数据隔离
     */
    @GetMapping("/my/list")
    @Permission(Role.PATIENT)
    public Result myList(PageDTO pageDTO) {
        return visitService.myList(pageDTO);
    }

    /**
     * 医院查询本院的就诊记录（分页）
     * 用于医院管理本机构的就诊数据
     */
    @GetMapping("/hospital/list")
    public Result hospitalList(@RequestParam Long hospitalId,
                               @RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        PageDTO pageDTO = new PageDTO(pageNum, pageSize);
        return visitService.hospitalList(hospitalId, pageDTO);
    }

    /**
     * 根据 ID 查询就诊记录详情
     * 使用 Redis 缓存提升查询性能
     */
    @GetMapping("/{visitId}")
    public Result getVisitById(@PathVariable Long visitId) {
        return visitService.getVisitById(visitId);
    }

}
