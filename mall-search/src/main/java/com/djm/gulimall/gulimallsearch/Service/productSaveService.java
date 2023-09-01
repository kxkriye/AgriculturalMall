package com.djm.gulimall.gulimallsearch.Service;

import com.djm.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List; /**
 * @author djm
 * @create 2022-01-14 21:24
 */
public interface productSaveService {
    boolean productStatusUp(List<SkuEsModel> skuEsModelList)  throws IOException;
}
