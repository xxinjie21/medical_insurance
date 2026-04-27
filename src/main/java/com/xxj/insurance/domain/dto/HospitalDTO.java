package com.xxj.insurance.domain.dto;



import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @ClassName HospitalDTO
 * @Description
 * @Author xxinj
 * @Date 2026/4/21 15:46
 * @Version 1.0
 */

@Data

public class HospitalDTO {

    @NotBlank(message = "医院名称不能为空")
    
    private String name;
}