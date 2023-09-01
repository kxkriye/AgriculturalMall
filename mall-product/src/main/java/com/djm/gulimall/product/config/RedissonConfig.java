package com.djm.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author djm
 * @create 2022-01-25 0:19
 */
@Configuration
public class RedissonConfig {
    @Bean(destroyMethod = "shutdown")
    public  RedissonClient redissonClient(){

    // 1. Create config object
    Config config = new Config();
//    集群配置，单个报错
//config.useClusterServers().addNodeAddress("redis://192.168.12.135:6379");
        config.useSingleServer().setAddress("redis://192.168.12.135:6379");
        return Redisson.create(config);
    }

}
