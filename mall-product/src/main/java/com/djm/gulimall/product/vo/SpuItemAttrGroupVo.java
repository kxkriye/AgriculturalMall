package com.djm.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * @author djm
 * @create 2022-02-03 13:16
 */
@Data
public class SpuItemAttrGroupVo {
  private String groupName;
  private List<SpuItemBaseAttrVo> attrs;
}
