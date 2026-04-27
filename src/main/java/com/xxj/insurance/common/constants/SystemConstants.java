package com.xxj.insurance.common.constants;

import java.math.BigDecimal;

/**
 * 系统常量类
 * 用于定义系统中的通用常量配置
 * 
 * @author xxj
 * @date 2026-04-19
 */
public class SystemConstants {
    
    /**
     * 图片上传目录（兼容黑马点评）
     */
    public static final String IMAGE_UPLOAD_DIR = "D:\\JavaCodes\\hm-dianping\\docs\\dev-ops\\nginx-1.18.0\\html\\hmdp\\imgs";
    
    /**
     * 用户昵称前缀
     */
    public static final String USER_NICK_NAME_PREFIX = "user_";
    
    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;
    
    /**
     * 最大分页大小
     */
    public static final int MAX_PAGE_SIZE = 100;
    
    /**
     * 金额精度（小数位数）
     */
    public static final int AMOUNT_SCALE = 2;
    
    /**
     * 对账允许误差（元）
     */
    public static final BigDecimal RECONCILE_ERROR_TOLERANCE = new BigDecimal("0.01");
    
    /**
     * 就诊编号前缀
     * 格式：VIS + 日期 + 序列号
     */
    public static final String VISIT_NO_PREFIX = "VIS";
    
    /**
     * 结算单编号前缀
     * 格式：STL + 日期 + 序列号
     */
    public static final String SETTLE_NO_PREFIX = "STL";
    
    /**
     * 批次编号前缀
     * 格式：DB + 日期 + 序列号
     */
    public static final String BATCH_NO_PREFIX = "DB";
    
    /**
     * 对账单编号前缀
     * 格式：RC + 日期 + 序列号
     */
    public static final String RECONCILE_NO_PREFIX = "RC";
}
