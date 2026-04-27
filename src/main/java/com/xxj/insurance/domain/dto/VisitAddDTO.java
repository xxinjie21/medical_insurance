package com.xxj.insurance.domain.dto;

import lombok.Data;

@Data
public class VisitAddDTO {
    private Long userId;       // 患者ID
    private Long hospitalId;   // 医院ID
    private Integer type;      // 1门诊 2住院
    private String diagnosis;  // 诊断结果
}