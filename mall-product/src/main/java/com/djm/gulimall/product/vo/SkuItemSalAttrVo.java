package com.djm.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * @author djm
 * @create 2022-02-03 13:16
 */
@Data
public class SkuItemSalAttrVo {
    private Long attrId;
    private String attrName;
    private List<AttrValueWithSkuIdVo> attrValues;
}
