package com.motor.frequency.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bloom.filter")
@Data
public class BloomFilterConfig {

    private Integer size;

    private Double  fpp;


}
