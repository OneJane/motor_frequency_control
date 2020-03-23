package com.motor.frequency.exception;

import com.motor.frequency.entity.CodeMsg;
import com.motor.frequency.entity.Result;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @program: motor_frequency_control
 * @description: 布隆过滤器服务
 * @author: OneJane
 * @create: 2020-03-17 14:18
 **/
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    public Result<String> exceptionHandler(HttpServletRequest request, Exception e){
        e.printStackTrace();
        if(e instanceof GlobalException) {
            GlobalException exception = (GlobalException) e;
            return Result.error(exception.getCodeMsg());
        }else if(e instanceof BindException) {
            BindException ex = (BindException) e;
            List<ObjectError> errors = ex.getAllErrors();
            ObjectError error = errors.get(0);
            String msg = error.getDefaultMessage();
            return Result.error(CodeMsg.BIND_ERROR.fillArgs(msg));
        }else {
            return Result.error(CodeMsg.SERVER_ERROR);
        }
    }
}
