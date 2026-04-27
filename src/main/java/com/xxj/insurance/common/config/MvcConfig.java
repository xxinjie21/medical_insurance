package com.xxj.insurance.common.config;

import com.xxj.insurance.common.interceptors.UserInfoInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class MvcConfig implements WebMvcConfigurer {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 只传 redisTemplate，和拦截器构造方法匹配
        registry.addInterceptor(new UserInfoInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/sendCode",
                        "/user/sign",
                        "/error"
                );
    }
}