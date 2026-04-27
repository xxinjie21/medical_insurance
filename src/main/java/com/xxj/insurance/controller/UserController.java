package com.xxj.insurance.controller;


import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.UserLoginDTO;
import com.xxj.insurance.domain.dto.UserRegisterDTO;
import com.xxj.insurance.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理 Controller - 处理用户登录、注册、登出等基础功能
 * 支持三种角色：患者、医院、医保局
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /**
     * 用户登录接口
     * 校验用户信息后生成 JWT Token，并将登录状态存入 Redis
     */
    @PostMapping("/login")
    public Result login(@RequestBody UserLoginDTO userLoginDTO) {
        return userService.login(userLoginDTO);
    }

    /**
     * 用户注册接口
     * 支持患者、医院、医保局三种角色注册
     */
    @PostMapping("/sign")
    public Result sign(@RequestBody UserRegisterDTO userRegisterDTO) {
        return userService.sign(userRegisterDTO);
    }

    /**
     * 用户登出接口
     * 删除 Redis 中的登录状态，使 Token 失效
     */
    @PostMapping("/loginout")
    public Result loginout(String token) {
        return userService.loginout(token);
    }

}
