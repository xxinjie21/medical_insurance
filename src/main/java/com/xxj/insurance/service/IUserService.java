package com.xxj.insurance.service;

import com.xxj.insurance.common.domain.Result;
import com.xxj.insurance.domain.dto.UserLoginDTO;
import com.xxj.insurance.domain.dto.UserRegisterDTO;
import com.xxj.insurance.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户服务接口 - 定义用户相关的业务方法
 */
public interface IUserService extends IService<User> {

    /**
     * 用户登录
     * @param userLoginDTO 登录参数（包含 userId、password、role）
     * @return 登录成功返回 token 和用户信息
     */
    Result login(UserLoginDTO userLoginDTO);

    /**
     * 用户注册
     * @param userRegisterDTO 注册参数（包含 name、password、role 等）
     * @return 注册成功返回用户信息
     */
    Result sign(UserRegisterDTO userRegisterDTO);

    /**
     * 用户登出
     * @param token 登录 token
     * @return 登出结果
     */
    Result loginout(String token);
}
