package com.xxj.insurance.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付 DTO（使用账户余额支付）
 */
@Data
public class AccountPayDTO {
    
    /**
     * 就诊 ID
     */
    private Long visitId;
    
    /**
     * 支付金额
     */
    private BigDecimal amount;
    
    /**
     * 备注
     */
    private String remark;
}
