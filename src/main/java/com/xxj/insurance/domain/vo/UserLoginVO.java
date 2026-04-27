package com.xxj.insurance.domain.vo;

import lombok.Data;

@Data
public class UserLoginVO {
    private Long userId;
    private String name;
    private Integer role;
    private Long hospitalId;  // 医院/患者所属医院，医保局为null
    private String token;
}