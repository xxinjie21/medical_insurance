package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.VisitAddDTO;
import com.xxj.insurance.domain.po.Visit;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 就诊服务接口 - 定义就诊相关的业务方法
 */
public interface IVisitService extends IService<Visit> {

    /**
     * 医院新增就诊记录
     * @param dto 就诊信息（包含患者 ID、医院 ID、诊断结果等）
     * @return 新增的就诊信息
     */
    Result add(VisitAddDTO dto);

    /**
     * 患者查询个人就诊记录（分页）
     * @param pageDTO 分页参数
     * @return 患者就诊记录列表
     */
    Result myList(PageDTO pageDTO);

    /**
     * 医院查询本院就诊记录（分页）
     * @param hospitalId 医院 ID
     * @param pageDTO 分页参数
     * @return 就诊记录列表
     */
    Result hospitalList(Long hospitalId, PageDTO pageDTO);

    /**
     * 根据 ID 查询就诊记录
     * @param visitId 就诊 ID
     * @return 就诊记录
     */
    Result getVisitById(Long visitId);
}
