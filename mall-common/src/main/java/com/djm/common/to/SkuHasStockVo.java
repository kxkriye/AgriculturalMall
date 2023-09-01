package com.djm.common.to;

import lombok.Data;

/**
 * @author djm
 * @create 2022-01-14 20:36
 */
@Data
public class SkuHasStockVo {
    private Long skuId;
    private Boolean hasStock;
}
