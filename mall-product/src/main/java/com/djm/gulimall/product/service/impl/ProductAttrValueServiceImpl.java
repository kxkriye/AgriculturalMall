package com.djm.gulimall.product.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.Query;

import com.djm.gulimall.product.dao.ProductAttrValueDao;
import com.djm.gulimall.product.entity.ProductAttrValueEntity;
import com.djm.gulimall.product.service.ProductAttrValueService;
import org.springframework.transaction.annotation.Transactional;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }
    //[{
    //	"attrId": 7,
    //	"attrName": "入网型号",
    //	"attrValue": "LIO-AL00",
    //	"quickShow": 1
    //}, {
    //	"attrId": 14,
    //	"attrName": "机身材质工艺",
    //	"attrValue": "玻璃",
    //	"quickShow": 0
    //}]
    //如果删除为空值传上来则没有,例机身材质工艺值是空值
//    [{
        //	"attrId": 7,
        //	"attrName": "入网型号",
        //	"attrValue": "LIO-AL00",
        //	"quickShow": 1
        //}]
//    @Transactional
//    @Override
//    public void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities) {
//        List<ProductAttrValueEntity> collect = entities.stream().map(item -> {
//            ProductAttrValueEntity one = new ProductAttrValueEntity();
//            BeanUtils.copyProperties(item, one);//注意没有的属性直接为null
//
//            ProductAttrValueEntity two  = this.getOne(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId).eq("attr_id", item.getAttrId()));
////            System.out.println(one.getSpuId());null
//            one.setId(two.getId());
//            return one;
//        }).collect(Collectors.toList());
//        this.updateBatchById(collect);
//    }
    @Transactional
    @Override
    public void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities) {
        //1、删除这个spuId之前对应的所有属性
        this.baseMapper.delete(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id",spuId));


        List<ProductAttrValueEntity> collect = entities.stream().map(item -> {
            item.setSpuId(spuId);
            return item;
        }).collect(Collectors.toList());
        this.saveBatch(collect);
    }


    @Override
    public List<ProductAttrValueEntity> baseAttrlistforspu(Long spuId) {
        List<ProductAttrValueEntity> entities = this.baseMapper.selectList(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
        return entities;

    }

    @Override
    public List<ProductAttrValueEntity> baseAttrListforspu(Long spuId) {
       return this.list(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
    }


}
