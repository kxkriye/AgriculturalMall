package com.djm.gulimall.product.dao;

import com.djm.gulimall.product.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品属性
 * 
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-22 22:56:21
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {

    List<Long> selectBySearchAttrIds(@Param("attrIds") List<Long> collect);
}
