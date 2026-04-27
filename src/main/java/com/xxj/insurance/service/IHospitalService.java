package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.PageDTO;
import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.HospitalDTO;
import com.xxj.insurance.domain.po.Hospital;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 医院服务接口 - 处理医院注册、查询等业务
 */
public interface IHospitalService extends IService<Hospital> {

    /**
     * 医院注册（签约）
     * 创建医院机构账号
     * @param hospitalDTO 医院信息
     * @return 注册结果
     */
    Result sign(HospitalDTO hospitalDTO);

    /**
     * 医院查询本院患者列表（分页）
     * 根据医院 ID 查询在该医院就诊的患者
     * @param hospitalId 医院 ID
     * @param pageDTO 分页参数
     * @return 患者列表
     */
    Result listMyPatient(Long hospitalId, PageDTO pageDTO);
}
