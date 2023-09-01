package com.djm.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author djm
 * @create 2022-01-23 20:03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Catelog2Vo {

        private String catalog1Id;
        private List<catelog3Vo> catalog3List;
        private String id;
        private String name;
@Data
@AllArgsConstructor
@NoArgsConstructor
  public static class catelog3Vo{
      private String catalog2Id;
      private String id;
      private String name;
  }


}
