package com.djm.gulimall.product.dao;

import com.djm.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.djm.gulimall.product.vo.SkuItemSalAttrVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * sku销售属性&值
 * 
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-22 22:56:21
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    List<SkuItemSalAttrVo> getSkuItem(Long spuId);

    List<String> getSkuSaleforskuId(Long skuId);
}
