package com.djm.gulimall.product.dao;

import com.djm.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-22 22:56:21
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
