package com.djm.gulimall.product.service.impl;

import com.djm.gulimall.product.vo.SkuItemSalAttrVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.Query;

import com.djm.gulimall.product.dao.SkuSaleAttrValueDao;
import com.djm.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.djm.gulimall.product.service.SkuSaleAttrValueService;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuItemSalAttrVo> getSaleAttrBySpuId(Long spuId) {

      return  this.baseMapper.getSkuItem(spuId);

    }

    @Override
    public List<String> getSkuSaleforskuId(Long skuId) {

        return this.baseMapper.getSkuSaleforskuId(skuId);
    }

}