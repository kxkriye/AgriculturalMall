package com.djm.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.djm.common.to.OrderTo;
import com.djm.common.to.mq.StockLockedTo;
import com.djm.common.utils.PageUtils;
import com.djm.gulimall.ware.entity.WareSkuEntity;
import com.djm.gulimall.ware.vo.SkuHasStockVo;
import com.djm.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-23 21:12:57
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void updateBypurchase(Long id);

    List<SkuHasStockVo> getSkusStock(List<Long> skuIds);

    Boolean lockStock(WareSkuLockVo vo);

    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo orderTo);
}

