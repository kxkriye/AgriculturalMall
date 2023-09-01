package com.djm.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.djm.common.utils.PageUtils;
import com.djm.common.valid.AddGroup;
import com.djm.common.valid.UpdateGroup;
import com.djm.gulimall.product.entity.BrandEntity;
import com.djm.gulimall.product.entity.CategoryBrandRelationEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.List;
import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author djm
 * @email djm@gmail.com
 * @date 2021-11-22 22:56:21
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveDetail(CategoryBrandRelationEntity categoryBrandRelation);
    void updateBrand(Long brandId, String name);

    void updateCategory(Long catId, String name);

    List<BrandEntity> getBrandsByCatId(Long catId);
}

