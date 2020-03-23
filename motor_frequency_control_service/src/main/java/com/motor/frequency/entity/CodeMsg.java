package com.motor.frequency.entity;

import lombok.Data;

/**
 * @program: motor_frequency_control
 * @description: 布隆过滤器服务
 * @author: OneJane
 * @create: 2020-03-17 14:18
 **/
@Data
public class CodeMsg {
    private int code;
    private String msg;
    public static CodeMsg SUCCESS = new CodeMsg(0, "success");
    public static CodeMsg SERVER_ERROR = new CodeMsg(500100, "服务端异常");
    public static CodeMsg BIND_ERROR = new CodeMsg(50001, "参数校验异常：%s");
    private CodeMsg(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    public CodeMsg fillArgs(Object... args) {
        int code = this.code;
        String message = String.format(this.msg, args);
        return new CodeMsg(code, message);
    }
}

