package com.xxj.insurance.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 充值 DTO
 */
@Data
public class RechargeDTO {
    
    /**
     * 充值金额
     */
    private BigDecimal amount;
    
    /**
     * 充值类型：1-微信 2-支付宝 3-银行卡 4-现金
     */
    private Integer type;
    
    /**
     * 备注
     */
    private String remark;
}
