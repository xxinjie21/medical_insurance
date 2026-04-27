package com.xxj.insurance.common.constants;

/**
 * 账户相关常量
 * 
 * @author xxj
 * @date 2026-04-21
 */
public class AccountConstants {
    
    /**
     * 账户状态：0-冻结
     */
    public static final Integer ACCOUNT_STATUS_FROZEN = 0;
    
    /**
     * 账户状态：1-正常
     */
    public static final Integer ACCOUNT_STATUS_NORMAL = 1;
    
    /**
     * 充值类型：1-微信
     */
    public static final Integer RECHARGE_TYPE_WECHAT = 1;
    
    /**
     * 充值类型：2-支付宝
     */
    public static final Integer RECHARGE_TYPE_ALIPAY = 2;
    
    /**
     * 充值类型：3-银行卡
     */
    public static final Integer RECHARGE_TYPE_BANK = 3;
    
    /**
     * 充值类型：4-现金
     */
    public static final Integer RECHARGE_TYPE_CASH = 4;
    
    /**
     * 充值状态：0-待支付
     */
    public static final Integer RECHARGE_STATUS_PENDING = 0;
    
    /**
     * 充值状态：1-支付成功
     */
    public static final Integer RECHARGE_STATUS_SUCCESS = 1;
    
    /**
     * 充值状态：2-支付失败
     */
    public static final Integer RECHARGE_STATUS_FAILED = 2;
    
    /**
     * 充值状态：3-已退款
     */
    public static final Integer RECHARGE_STATUS_REFUNDED = 3;
    
    /**
     * 消费类型：1-就诊支付
     */
    public static final Integer CONSUMPTION_TYPE_VISIT_PAY = 1;
    
    /**
     * 消费类型：2-退款
     */
    public static final Integer CONSUMPTION_TYPE_REFUND = 2;
    
    /**
     * 消费类型：3-调整
     */
    public static final Integer CONSUMPTION_TYPE_ADJUST = 3;
    
    /**
     * 消费状态：0-取消
     */
    public static final Integer CONSUMPTION_STATUS_CANCEL = 0;
    
    /**
     * 消费状态：1-成功
     */
    public static final Integer CONSUMPTION_STATUS_SUCCESS = 1;
    
    /**
     * 充值订单号前缀
     */
    public static final String RECHARGE_ORDER_PREFIX = "RC";
    
    /**
     * 消费订单号前缀
     */
    public static final String CONSUMPTION_ORDER_PREFIX = "CP";
}
