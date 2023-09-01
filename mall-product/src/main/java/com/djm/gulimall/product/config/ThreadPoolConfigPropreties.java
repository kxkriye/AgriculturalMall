package com.djm.gulimall.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author djm
 * @create 2022-02-04 20:30
 */
@Data
@ConfigurationProperties(prefix = "gulimall.thread")
@Component
public class ThreadPoolConfigPropreties {
    private Integer coreSize;
    private Integer maxSize;
    private Integer keepAliveTime;

}
