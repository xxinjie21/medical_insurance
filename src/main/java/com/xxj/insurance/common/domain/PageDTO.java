package com.xxj.insurance.common.domain;

import lombok.Data;

/**
 * @ClassName PageDTO
 * @Description
 * @Author xxinj
 * @Date 2026/4/21 16:54
 * @Version 1.0
 */
@Data
public class PageDTO {
    // 默认第 1 页
    Integer pageNum = 1;
    // 默认每页 10 条
    Integer pageSize = 10;
    
    // 无参构造函数
    public PageDTO() {
    }
    
    // 有参构造函数
    public PageDTO(Integer pageNum, Integer pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}