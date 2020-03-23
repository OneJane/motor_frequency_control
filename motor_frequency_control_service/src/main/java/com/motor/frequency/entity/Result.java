package com.motor.frequency.entity;

import lombok.Data;

/**
 * @program: motor_frequency_control
 * @description: 布隆过滤器服务
 * @author: OneJane
 * @create: 2020-03-17 14:18
 **/
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;
    /**
     * 成功时候的调用
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>(data);
    }
    /**
     * 失败时候的调用
     */
    public static <T> Result<T> error(CodeMsg cm) {
        return new Result<T>(cm);
    }
    private Result(T data) {
        this.code = 0;
        this.msg = "success";
        this.data = data;
    }
    private Result(CodeMsg cm) {
        if (cm == null) {
            return;
        }
        this.code = cm.getCode();
        this.msg = cm.getMsg();
    }
}
