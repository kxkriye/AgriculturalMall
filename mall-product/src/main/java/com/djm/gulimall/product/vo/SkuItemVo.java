package com.djm.gulimall.product.vo;

import com.djm.gulimall.product.entity.SkuImagesEntity;
import com.djm.gulimall.product.entity.SkuInfoEntity;
import com.djm.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author djm
 * @create 2022-02-03 13:14
 */
@Data
@ToString
public class SkuItemVo {
    // 1、sku基本获取 pms_sku_info
    SkuInfoEntity info;
    // 是否有库存
    boolean  hasStock = true;
    // 2、sku的图片信息 pms_sku_images
    List<SkuImagesEntity> images;
    // 3、获取spu的销售属性组
    List<SkuItemSalAttrVo> saleAttr;
    // 4、获取spu的介绍
    SpuInfoDescEntity desc;
    // 5、获取spu的规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;
    //6、秒杀商品的优惠信息
    private SeckillSkuVo seckillSkuVo;
}
