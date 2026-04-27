package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.FeeAddDTO;
import com.xxj.insurance.domain.po.Fee;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 费用明细服务接口 - 处理就诊费用明细的添加和查询
 */
public interface IFeeService extends IService<Fee> {

    /**
     * 批量添加费用明细
     * 医院为就诊记录添加多个费用项目，自动计算总价
     * @param dtoList 费用明细列表（包含项目名称、类型、单价、数量）
     * @return 添加成功的费用列表
     */
    Result batchAdd(List<FeeAddDTO> dtoList);

    /**
     * 根据就诊 ID 查询费用明细列表
     * @param visitId 就诊 ID
     * @return 费用明细列表
     */
    Result listByVisitId(Long visitId);
}
