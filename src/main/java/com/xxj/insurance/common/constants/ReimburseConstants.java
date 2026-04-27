package com.xxj.insurance.common.constants;

import java.math.BigDecimal;

/**
 * 医保报销常量
 * 
 * @author xxj
 * @date 2026-04-21
 */
public class ReimburseConstants {

    /**
     * 甲类费用报销比例：100%
     */
    public static final BigDecimal CATEGORY_A_RATE = new BigDecimal("1.0");

    /**
     * 乙类费用报销比例：80%
     */
    public static final BigDecimal CATEGORY_B_RATE = new BigDecimal("0.8");

    /**
     * 自费费用报销比例：0%
     */
    public static final BigDecimal CATEGORY_C_RATE = BigDecimal.ZERO;

    /**
     * 费用类型：1-甲类
     */
    public static final Integer TYPE_A = 1;

    /**
     * 费用类型：2-乙类
     */
    public static final Integer TYPE_B = 2;

    /**
     * 费用类型：3-自费
     */
    public static final Integer TYPE_C = 3;

    /**
     * 结算状态：0-待申报
     */
    public static final Integer SETTLE_STATUS_UNDECLARED = 0;

    /**
     * 结算状态：1-已申报
     */
    public static final Integer SETTLE_STATUS_DECLARED = 1;

    /**
     * 结算状态：2-已审核
     */
    public static final Integer SETTLE_STATUS_AUDITED = 2;

    /**
     * 结算状态：3-已拨付
     */
    public static final Integer SETTLE_STATUS_PAID = 3;

    /**
     * 就诊状态：0-就诊中/待结算
     */
    public static final Integer VISIT_STATUS_PENDING = 0;

    /**
     * 就诊状态：1-已结算
     */
    public static final Integer VISIT_STATUS_SETTLED = 1;

    /**
     * 批次状态：0-待申报（新建批次）
     */
    public static final Integer BATCH_STATUS_PENDING = 0;

    /**
     * 批次状态：1-已申报（已提交审核）
     */
    public static final Integer BATCH_STATUS_DECLARED = 1;

    /**
     * 批次状态：2-已完成（审核通过并拨付）
     */
    public static final Integer BATCH_STATUS_COMPLETED = 2;

    /**
     * 审核结果：0-通过
     */
    public static final Integer AUDIT_PASS = 0;

    /**
     * 审核结果：1-扣款
     */
    public static final Integer AUDIT_REJECT = 1;

    /**
     * 支付状态：0-待支付
     */
    public static final Integer PAY_STATUS_PENDING = 0;

    /**
     * 支付状态：1-已支付
     */
    public static final Integer PAY_STATUS_PAID = 1;
}
