package com.djm.gulimall.gulimallsearch.Service;

import com.alibaba.fastjson.JSON;
import com.djm.common.to.es.SkuEsModel;
import com.djm.gulimall.gulimallsearch.config.GulimallElasticsearchConfig;
import com.djm.gulimall.gulimallsearch.constant.EsConstant;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author djm
 * @create 2022-01-14 21:24
 */
@Service
@Slf4j
public class productSaveServiceImpl  implements productSaveService   {

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModelList) throws IOException {

        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel model : skuEsModelList) {
        // 1、构造请求 指定es索引
        IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
        indexRequest.id(model.getSkuId().toString());
        indexRequest.source(JSON.toJSONString(model), XContentType.JSON );
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GulimallElasticsearchConfig.COMMON_OPTIONS);

        // TODO 1、 如果批量错误
        boolean b = bulk.hasFailures();
        List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        log.error("商品上架完成：{}",collect);

        return b;


    }
}
