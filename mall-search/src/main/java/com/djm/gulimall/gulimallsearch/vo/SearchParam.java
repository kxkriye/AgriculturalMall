package com.djm.gulimall.gulimallsearch.vo;

/**
 * @author djm
 * @create 2022-01-27 23:09
 */

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传递过来的查询条件
 * @author gcq
 * @Create 2020-11-02
 */
@Data
public class SearchParam {

    /**
     * 页面传递过来的全文匹配关键字
     */
    private String keyword;
    /**
     * 三级分类id
     */
    private Long catalog3Id;
    /**
     * sort=saleCout_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     * 排序条件
     */
    private String sort;

    /**
     * hasStock(是否有货) skuPrice区间，brandId、catalog3Id、attrs
     */
    /**
     * 是否显示有货
     */
    private Integer hasStock;
    /**
     * 价格区间查询
     * 1_500/_500/500_
     */
    private String skuPrice;
    /**
     * 按照品牌进行查询，可以多选
     * brandId=1&brandId=2
     */
    private List<Long> brandId;
    /**
     * 按照属性进行筛选
     * attrs=2_5存:6寸
     */
    private List<String> attrs;
    /**
     * 页码
     */
    private Integer pageNum = 1;
    private String _queryString;

}

