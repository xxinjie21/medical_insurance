package com.xxj.insurance.common.interceptors;

import cn.hutool.core.util.StrUtil;
import com.xxj.insurance.common.annotation.Permission;
import com.xxj.insurance.common.enums.Role;
import com.xxj.insurance.common.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户信息拦截器
 * 功能：
 * 1. 校验用户登录状态（通过 token）
 * 2. 从 Redis 获取用户信息并保存到 ThreadLocal
 * 3. 根据@Permission 注解进行权限校验
 * 
 * @author xxj
 * @since 2026-04-21
 */
public class UserInfoInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    public UserInfoInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 请求处理前的拦截方法
     * 
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 处理器
     * @return true-放行，false-拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // ================== 1. 校验 token（检查登录状态）==================
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            // token 为空，返回 401 未授权
            response.setStatus(401);
            response.getWriter().write("请先登录");
            return false;
        }

        // ================== 2. 从 Redis 获取用户 ID ==================
        // Redis key 格式：login:token:{token} -> userId
        String userId = redisTemplate.opsForValue().get("login:token:" + token);
        if (userId == null) {
            // token 不存在或已过期，返回 401
            response.setStatus(401);
            response.getWriter().write("登录已过期");
            return false;
        }

        // ================== 3. 保存用户 ID 到 ThreadLocal ==================
        // 方便后续业务代码通过 UserHolder.getUserId() 获取当前登录用户
        UserHolder.save(Long.valueOf(userId));

        // ================== 4. 权限注解校验 ==================
        // 如果不是方法级别的请求（如静态资源），直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod hm = (HandlerMethod) handler;
        
        // 优先获取方法上的@Permission 注解
        Permission permission = hm.getMethodAnnotation(Permission.class);
        // 如果方法上没有，再获取类上的@Permission 注解
        if (permission == null) {
            permission = hm.getBeanType().getAnnotation(Permission.class);
        }
        // 如果没有@Permission 注解，说明不需要权限控制，直接放行
        if (permission == null) {
            return true;
        }

        // ================== 5. 获取用户角色并校验 ==================
        // 从 Redis 获取用户角色：login:role:{userId} -> role
        String userRoleStr = redisTemplate.opsForValue().get("login:role:" + userId);
        if (StrUtil.isBlank(userRoleStr)) {
            // 无法获取用户角色，返回 403 禁止访问
            response.setStatus(403);
            response.getWriter().write("无法获取用户角色");
            return false;
        }

        // 将角色字符串转换为 Role 枚举
        Role userRole = Role.valueOf(Integer.valueOf(userRoleStr));
        if (userRole == null) {
            // 无效的角色，返回 403
            response.setStatus(403);
            response.getWriter().write("无效的角色");
            return false;
        }

        // ================== 6. 校验角色是否匹配 ==================
        // 如果用户角色与@Permission 要求的角色一致，放行
        if (permission.value() == userRole) {
            return true;
        }

        // 角色不匹配，返回 403 无权限访问
        response.setStatus(403);
        response.getWriter().write("无权限访问");
        return false;
    }

    /**
     * 请求完成后的清理方法
     * 清理 ThreadLocal 中的用户信息，防止内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.remove();
    }
}