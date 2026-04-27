package com.xxj.insurance.common.constants;

/**
 * Redis 常量类
 * 用于定义系统中所有 Redis 相关的键名和过期时间
 * 
 * @author xxj
 * @date 2026-04-19
 */
public class RedisConstants {
    
    // ==================== 登录相关常量 ====================
    /**
     * 登录验证码 Redis Key 前缀
     * 格式：login:code:{手机号}
     */
    public static final String LOGIN_CODE_KEY = "login:code:";
    
    /**
     * 登录验证码过期时间（分钟）
     */
    public static final Long LOGIN_CODE_TTL = 2L;
    
    /**
     * 登录用户 Token Redis Key 前缀
     * 格式：login:token:{token}
     */
    public static final String LOGIN_USER_KEY = "login:token:";
    
    /**
     * 登录用户 Token 过期时间（秒）
     */
    public static final Long LOGIN_USER_TTL = 36000L;
    
    // ==================== 缓存相关常量 ====================
    /**
     * 缓存空值的过期时间（分钟）
     * 用于防止缓存穿透
     */
    public static final Long CACHE_NULL_TTL = 2L;
    
    /**
     * 就诊信息缓存 Key 前缀
     * 格式：cache:visit:{visitId}
     */
    public static final String CACHE_VISIT_KEY = "cache:visit:";
    
    /**
     * 就诊信息缓存过期时间（分钟）
     */
    public static final Long CACHE_VISIT_TTL = 30L;
    
    /**
     * 结算信息缓存 Key 前缀
     * 格式：cache:settle:{settleId}
     */
    public static final String CACHE_SETTLE_KEY = "cache:settle:";
    
    /**
     * 结算信息缓存过期时间（分钟）
     */
    public static final Long CACHE_SETTLE_TTL = 30L;
    
    /**
     * 医保目录缓存 Key 前缀
     * 格式：cache:catalog:{itemCode}
     */
    public static final String CACHE_CATALOG_KEY = "cache:catalog:";
    
    /**
     * 医保目录缓存过期时间（分钟）
     */
    public static final Long CACHE_CATALOG_TTL = 60L;
    
    /**
     * 报销政策缓存 Key 前缀
     * 格式：cache:policy:{policyId}
     */
    public static final String CACHE_POLICY_KEY = "cache:policy:";
    
    /**
     * 报销政策缓存过期时间（分钟）
     */
    public static final Long CACHE_POLICY_TTL = 60L;
    
    // ==================== 分布式锁相关常量 ====================
    /**
     * 就诊记录分布式锁 Key 前缀
     * 格式：lock:visit:{visitId}
     */
    public static final String LOCK_VISIT_KEY = "lock:visit:";
    
    /**
     * 结算操作分布式锁 Key 前缀
     * 格式：lock:settle:{visitId}
     */
    public static final String LOCK_SETTLE_KEY = "lock:settle:";
    
    /**
     * 批次操作分布式锁 Key 前缀
     * 格式：lock:batch:{batchId}
     */
    public static final String LOCK_BATCH_KEY = "lock:batch:";
    
    /**
     * 对账操作分布式锁 Key 前缀
     * 格式：lock:reconcile:{reconcileId}
     */
    public static final String LOCK_RECONCILE_KEY = "lock:reconcile:";
    
    /**
     * 分布式锁默认过期时间（秒）
     */
    public static final Long LOCK_TTL = 10L;
    
    // ==================== 全局 ID 生成相关常量 ====================
    /**
     * 全局 ID 生成器 Redis Key 前缀
     * 格式：global:id:{业务类型}
     */
    public static final String GLOBAL_ID_KEY = "global:id:";
    
    // ==================== 幂等性控制相关常量 ====================
    /**
     * 幂等性 Token Redis Key 前缀
     * 格式：idempotent:{业务}:{唯一标识}
     */
    public static final String IDEMPOTENT_KEY = "idempotent:";
    
    /**
     * 幂等性 Token 过期时间（秒）
     */
    public static final Long IDEMPOTENT_TTL = 3600L;
    
    // ==================== 其他常量（兼容旧代码）====================
    /**
     * 商铺缓存 Key 前缀（兼容黑马点评）
     */
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    
    /**
     * 商铺缓存过期时间（分钟）（兼容黑马点评）
     */
    public static final Long CACHE_SHOP_TTL = 30L;
    
    /**
     * 商铺分布式锁 Key 前缀（兼容黑马点评）
     */
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    
    /**
     * 缓存类型 Key（兼容黑马点评）
     */
    public static final String CACHE_TYPE_KEY = "lock:type:";
    
    /**
     * 秒杀库存 Key（兼容黑马点评）
     */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    
    /**
     * 博客点赞 Key（兼容黑马点评）
     */
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    
    /**
     * 信息流 Key（兼容黑马点评）
     */
    public static final String FEED_KEY = "feed:";
    
    /**
     * 商铺地理位置 Key（兼容黑马点评）
     */
    public static final String SHOP_GEO_KEY = "shop:geo:";
    
    /**
     * 用户签到 Key（兼容黑马点评）
     */
    public static final String USER_SIGN_KEY = "sign:";
}
