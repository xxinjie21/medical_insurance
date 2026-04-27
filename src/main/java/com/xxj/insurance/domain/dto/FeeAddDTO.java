package com.xxj.insurance.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeAddDTO {
    private Long visitId;      // 就诊ID
    private String name;       // 项目名称
    private BigDecimal price;  // 单价
    private Integer num;       // 数量
    private Integer type;      // 1甲类 2乙类 3自费
}