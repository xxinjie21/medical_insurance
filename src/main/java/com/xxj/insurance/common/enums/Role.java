package com.xxj.insurance.common.enums;

/**
 * 用户角色枚举
 * 与数据库 user.role 字段对应：1-患者 2-医院 3-医保局
 */
public enum Role {
    PATIENT(1, "患者"),
    HOSPITAL(2, "医院"),
    MEDICAL(3, "医保局");

    private final Integer code;
    private final String name;

    Role(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据 code 获取角色
     */
    public static Role valueOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (Role role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }
}