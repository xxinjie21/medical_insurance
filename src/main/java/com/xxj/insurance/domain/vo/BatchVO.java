package com.xxj.insurance.domain.vo;

import com.xxj.insurance.domain.po.BatchItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BatchVO {
    // 批次基本信息
    private Long id;
    private String batchNo;
    private Long hospitalId;
    private Integer settleCnt;
    private BigDecimal totalAmt;
    private Integer status;
    private LocalDateTime createTime;
    private List<BatchItem> batchItems;
}