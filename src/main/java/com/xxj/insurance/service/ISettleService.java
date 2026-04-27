package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.po.Settle;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 医保结算服务接口 - 处理就诊费用的医保报销计算
 */
public interface ISettleService extends IService<Settle> {

    /**
     * 就诊医保结算
     * 根据费用明细计算报销金额：
     * - 甲类费用：100% 报销
     * - 乙类费用：80% 报销
     * - 自费费用：0% 报销
     * 使用分布式锁防止重复结算
     * 
     * @param visitId 就诊 ID
     * @return 结算结果（包含总费用、报销金额、自付金额）
     */
    Result calculate(Long visitId);

    /**
     * 查询就诊的结算详情
     * @param visitId 就诊 ID
     * @return 结算信息
     */
    Result getSettleDetail(Long visitId);
}
