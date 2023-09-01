package com.djm.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.djm.common.to.SkuReductionTo;
import com.djm.common.utils.PageUtils;
import com.djm.gulimall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:10:42
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuReduction(SkuReductionTo reductionTo);
}

