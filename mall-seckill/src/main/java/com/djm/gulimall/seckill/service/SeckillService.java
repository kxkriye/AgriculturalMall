package com.djm.gulimall.seckill.service;

import com.djm.gulimall.seckill.to.SeckillSkuRedisTo;

import java.util.List;

/**
 * @author djm
 * @create 2022-03-07 22:39
 */
public interface SeckillService {
    /**
     * 上架三天需要秒杀的商品
     */
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    SeckillSkuRedisTo getSkuSeckilInfo(Long skuId);

    String kill(String killId, String key, Integer num);
}
