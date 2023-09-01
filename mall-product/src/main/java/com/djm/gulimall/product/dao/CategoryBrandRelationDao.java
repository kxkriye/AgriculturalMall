package com.djm.gulimall.product.dao;

import com.djm.gulimall.product.entity.CategoryBrandRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 品牌分类关联
 * 
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-22 22:56:21
 */
@Mapper
public interface CategoryBrandRelationDao extends BaseMapper<CategoryBrandRelationEntity> {
    @Update(" UPDATE `pms_category_brand_relation` SET catelog_name=#{name} WHERE catelog_id=#{catId}")
    void updateCategory(@Param("catId") Long catId, @Param("name") String name);
}
