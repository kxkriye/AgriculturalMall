package com.djm.gulimall.product.feign;

import com.djm.common.to.es.SkuEsModel;
import com.djm.common.utils.R;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyAgreementSpi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author djm
 * @create 2022-01-14 21:21
 */
@FeignClient("gulimall-search")
public interface esFeignClient {
    @PostMapping("/search/save/product")
   R productStatusUp(@RequestBody List<SkuEsModel> skuEsModelList);










}
