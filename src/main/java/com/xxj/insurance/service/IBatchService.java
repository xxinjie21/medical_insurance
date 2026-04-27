package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Batch;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 批次申报服务接口 - 处理医院向医保局申报结算单的业务
 */
public interface IBatchService extends IService<Batch> {

    /**
     * 创建申报批次
     * 生成唯一的批次号，用于打包多个结算单
     * 使用分布式锁防止并发创建
     * 
     * @param hospitalId 医院 ID
     * @return 批次信息
     */
    Result createBatch(Long hospitalId);

    /**
     * 添加结算单到批次
     * 一个结算单只能添加到一个批次中，使用幂等性控制防止重复添加
     * 
     * @param batchId 批次 ID
     * @param settleId 结算单 ID
     * @return 操作结果
     */
    Result addSettleToBatch(Long batchId, Long settleId);

    /**
     * 查询批次详情（包含批次信息和明细列表）
     * @param batchId 批次 ID
     * @return 批次详情
     */
    Result getBatchDetail(Long batchId);
}
