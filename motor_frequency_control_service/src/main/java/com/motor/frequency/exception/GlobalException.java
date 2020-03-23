package com.motor.frequency.exception;

import com.motor.frequency.entity.CodeMsg;
import lombok.Data;

/**
 * @program: motor_frequency_control
 * @description: 布隆过滤器服务
 * @author: OneJane
 * @create: 2020-03-17 14:18
 **/
@Data
public class GlobalException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private CodeMsg codeMsg;
    public GlobalException(CodeMsg codeMsg) {
        super(codeMsg.toString());
        this.codeMsg = codeMsg;
    }
}
