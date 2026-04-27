package com.xxj.insurance.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitVO {
    private Long id;
    private Long userId;
    private String userName;
    private Long hospitalId;
    private String hospitalName;
    private Integer type;
    private String diagnosis;
    private LocalDateTime createTime;
}