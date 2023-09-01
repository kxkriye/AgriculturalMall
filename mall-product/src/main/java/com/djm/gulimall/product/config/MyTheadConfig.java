package com.djm.gulimall.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author djm
 * @create 2022-02-04 20:24
 */
@Configuration
public class MyTheadConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfigPropreties threadPool){
        return new ThreadPoolExecutor(threadPool.getCoreSize(),threadPool.getMaxSize(),threadPool.getKeepAliveTime(), TimeUnit.SECONDS,new LinkedBlockingQueue<>(10000),Executors.defaultThreadFactory(),new ThreadPoolExecutor.AbortPolicy());
    }
}
