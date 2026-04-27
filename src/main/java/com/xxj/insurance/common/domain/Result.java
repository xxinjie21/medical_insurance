package com.xxj.insurance.common.domain;

import lombok.Data;

import java.util.List;

@Data
public class Result {
    private Boolean success;
    private String errorMsg;
    private Object data;
    private Long total;

    public Result() {
    }

    public static Result ok() {
        Result result = new Result();
        result.setSuccess(true);
        return result;
    }

    public static Result ok(Object data) {
        Result result = new Result();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static Result ok(List<?> data, Long total) {
        Result result = new Result();
        result.setSuccess(true);
        result.setData(data);
        result.setTotal(total);
        return result;
    }

    public static Result fail(String errorMsg) {
        Result result = new Result();
        result.setSuccess(false);
        result.setErrorMsg(errorMsg);
        return result;
    }
}