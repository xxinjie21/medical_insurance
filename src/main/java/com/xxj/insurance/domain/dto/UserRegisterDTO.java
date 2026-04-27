package com.xxj.insurance.domain.dto;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data

public class UserRegisterDTO {

    @NotBlank(message = "密码不能为空")

    private String password;

    @NotBlank(message = "姓名不能为空")

    private String name;


    private String idCard;


    private Long hospitalId;

    @NotNull(message = "角色不能为空")
    private Integer role;
}