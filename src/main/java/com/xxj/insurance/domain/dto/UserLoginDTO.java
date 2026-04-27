package com.xxj.insurance.domain.dto;

import lombok.Data;

@Data
public class UserLoginDTO {
    private Long userId;      // 直接传用户ID登录
    private String password;  // 密码
    private Integer role;     // 1患者 2医院 3医保局
}