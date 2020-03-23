package com.motor.frequency;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.motor")
@EnableDubbo
public class FrequencyControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrequencyControlApplication.class, args);
    }
}
