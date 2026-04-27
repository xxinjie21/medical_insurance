package com.xxj.insurance.common.utils;

/**
 * 用户持有者
 * 用于在请求线程中保存当前登录用户信息
 *
 * @author xxj
 * @date 2026-04-19
 */
public class UserHolder {

    // 存储 用户ID
    private static final ThreadLocal<Long> TL = new ThreadLocal<>();

    /**
     * 保存用户ID
     */
    public static void save(Long userId) {
        TL.set(userId);
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        return TL.get();
    }

    /**
     * 移除用户信息（必须调用，防止内存泄漏）
     */
    public static void remove() {
        TL.remove();
    }
}