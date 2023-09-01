package com.djm.gulimall.gulimallsearch.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.djm.common.to.es.SkuEsModel;
import com.djm.common.utils.R;
import com.djm.gulimall.gulimallsearch.config.GulimallElasticsearchConfig;
import com.djm.gulimall.gulimallsearch.constant.EsConstant;
import com.djm.gulimall.gulimallsearch.feign.ProductFeignService;
import com.djm.gulimall.gulimallsearch.vo.AttrResponseVo;
import com.djm.gulimall.gulimallsearch.vo.SearchParam;
import com.djm.gulimall.gulimallsearch.vo.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author djm
 * @create 2022-01-27 23:08
 */
@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    ProductFeignService productFeignService;

    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder(); //构建DSL语句
        /**
         * 模糊匹配 过滤（按照属性、分类、品牌、价格区间、库存）
         */
        // 1、构建bool - query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 1.1 must - 模糊匹配
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        // 1.2 bool - filter 按照三级分类id来查询
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catelogId", param.getCatalog3Id()));
        }
        // 1.2 bool - filter 按照品牌id来查询
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
//        错误，不可能即有15和16的attrid，乱了
//        // 1.2 bool - filter 按照所有指定的属性来进行查询 *******不理解这个attr=1_5寸:8寸这样的设计
//        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
//                // attr=1_5寸:8寸&attrs=2_16G:8G
//                BoolQueryBuilder nestedboolQuery = QueryBuilders.boolQuery();
//            for (String attr : param.getAttrs()) {
//                String[] s = attr.split("_");
//                String attrId = s[0];// 检索的属性id
//                String[] attrValues = s[1].split(":");
//                nestedboolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
//                nestedboolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
//            }
//                // 每一个必须都生成一个nested查询
//                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedboolQuery, ScoreMode.None);
//                boolQuery.filter(nestedQuery);
//        }
        // 1.2 bool - filter 按照所有指定的属性来进行查询 *******不理解这个attr=1_5寸:8寸这样的设计
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attr : param.getAttrs()) {
                // attr=1_5寸:8寸&attrs=2_16G:8G
                BoolQueryBuilder nestedboolQuery = QueryBuilders.boolQuery();
                String[] s = attr.split("_");
                String attrId = s[0];// 检索的属性id
                String[] attrValues = s[1].split(":");
                nestedboolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedboolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                // 每一个必须都生成一个nested查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedboolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }
        // 1.2 bool - filter 按照库存是否存在
        if (param.getHasStock()!=null){
        boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));

        }
        // 1.2 bool - filter 按照价格区间
        /**
         * 1_500/_500/500_
         */
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                if (s[0].equals("")){
                    rangeQuery.lte(s[1]);
                }else {rangeQuery.gte(s[0]).lte(s[1]);}
                // 区间
            } else if (s.length == 1) {
                if (param.getSkuPrice().startsWith("_")) {
                    rangeQuery.lte(s[0]);
                }
                if (param.getSkuPrice().endsWith("_")) {
                    rangeQuery.gte(s[0]);
                }
            }
            boolQuery.filter(rangeQuery);
        }
        //把以前所有条件都拿来进行封装
        sourceBuilder.query(boolQuery);
        /**
         * 排序、分页、高亮
         */
        //2.1、排序
        if (!StringUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();
            //sort=hotScore_asc/desc
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
        }
        //2.2 分页 pageSize:5
        // pageNum:1 from 0 size:5 [0,1,2,3,4]
        // pageNum:2 from 5 size:5
        // from (pageNum - 1)*size
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        //2.3、高亮
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }
        /**
         * 聚合分析
         */
        //1、品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        //品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(2));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(2));
        // TODO 1、聚合brand
        sourceBuilder.aggregation(brand_agg);
        //2、分类聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catelogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catelogName").size(1));
        // TODO 2、聚合catalog
        sourceBuilder.aggregation(catalog_agg);
        //3、属性聚合 attr_agg
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        // 聚合出当前所有的attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        //聚合分析出当前attr_id对应的名字
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // 聚合分析出当前attr_id对应的可能的属性值attractValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        // TODO 3、聚合attr
        sourceBuilder.aggregation(attr_agg);


        String s = sourceBuilder.toString();
        System.out.println("构建的DSL:" + s);
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);

        return searchRequest;
    }

    @Override
    public SearchResult search(SearchParam param) {
        try {
            SearchResponse response = restHighLevelClient.search(buildSearchRequest(param), GulimallElasticsearchConfig.COMMON_OPTIONS);
            return buildSearchResult(response,param);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {

        SearchResult result = new SearchResult();
        SearchHits hits = response.getHits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(string);
                }
                esModels.add(skuEsModel);
            }
        }
        //1、返回所有查询到的商品
        result.setProducts(esModels);
        //2、当前所有商品设计到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            // 1、得到属性的id
            Long attrId = bucket.getKeyAsNumber().longValue();
            // 2、得到属性的名字
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            // 3、得到属性的所有值
            List<String> attrValue = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = item.getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());
            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValue);
            attrVos.add(attrVo);
        }
            result.setAttrs(attrVos);
            //3、当前所有商品的分类信息
            ParsedLongTerms Catalog_agg = response.getAggregations().get("catalog_agg");
            List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
            List<? extends Terms.Bucket> buckets = Catalog_agg.getBuckets();
            for (Terms.Bucket bucket : buckets) {
                SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
                // 得到分类id
                String keyAsString = bucket.getKeyAsString();
                catalogVo.setCatalogId(Long.parseLong(keyAsString));
                // 得到分类名
                ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
                String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
                catalogVo.setCatalogName(catalog_name);
                catalogVos.add(catalogVo);
            }
            result.setCatalogs(catalogVos);

            //4、当前所有商品的品牌信息
            List<SearchResult.BrandVo> brandVos = new ArrayList<>();
            ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
            for (Terms.Bucket bucket : brand_agg.getBuckets()) {
                SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
                // 1、得到品牌的id
                long brandId = bucket.getKeyAsNumber().longValue();
                // 2、得到品牌的图片
                String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
                // 3、得到品牌的姓名
                String brandname = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
                brandVo.setBrandName(brandname);
                brandVo.setBrandId(brandId);
                brandVo.setBrandImg(brandImg);
                brandVos.add(brandVo);
            }
            result.setBrands(brandVos);
            //5、分页信息 - 总记录数
            long total = hits.getTotalHits().value;
            result.setTotal(total);
            //6、分页信息 - 页码
            result.setPageNum(param.getPageNum());
            //7、分页信息 - 总页码
            int totalPages = (int) total % EsConstant.PRODUCT_PAGESIZE == 0 ? (int) total / EsConstant.PRODUCT_PAGESIZE : ((int) total / EsConstant.PRODUCT_PAGESIZE + 1);
            result.setTotalPages(totalPages);

            List<Integer> pageNavs = new ArrayList<>();
            for(int i = 1; i <= totalPages; i++) {
                pageNavs.add(i);
            }
            result.setPageNavs(pageNavs);
        // 8、构建面包屑导航功能
        if (param.getAttrs()!=null && param.getAttrs().size() >0){
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
                // 1、分析每个 attrs传递过来的值
                SearchResult.NavVo navo = new SearchResult.NavVo();
                // attrs=2_5寸：6寸
                String[] s = attr.split("_");
                navo.setNavValue(s[1]);
                R r = productFeignService.info(Long.parseLong(s[0]));
                result.getAttrIds().add(Long.parseLong(s[0]));
                if (r.getCode() == 0) {
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navo.setNavName(data.getAttrName());
                } else {
                    navo.setNavName(s[0]);
                }
                // 取消这个面包屑导航以后，我们要跳转到那个地方，将请求地址的url里面的当前置空
                //拿到所有的查询条件后，去掉当前
                // attrs=15_海思(Hisilicon)
                String encode=null;
                try {
                    encode=URLEncoder.encode(attr,"UTF-8" );
                    encode= encode.replace("+","%20" );//浏览器 %20和java空格UTF转化处理不一样
                    encode= encode.replace("%3B",";" );
                    System.out.println(encode);
//                System.out.println(URLEncoder.encode(";", "UTF-8"));//＋
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                System.out.println(param.get_queryString());
//                System.out.println(attr);
                String replace = param.get_queryString().replace("&attrs="+encode, "");
                navo.setLink("http://search.gulimall.com/list.html?" + replace);

                return navo;
            }).collect(Collectors.toList());
            result.setNavs(collect);
        }

//        // 品牌、分类
//        if(param.getBrandId() != null && param.getBrandId().size() > 0) {
//            List<SearchResult.NavVo> navs = result.getNavs();
//            SearchResult.NavVo navVo = new SearchResult.NavVo();
//            navVo.setNavName("品牌");
//            //TODO 远程查询所有品牌
//            R info = productFeignService.info(param.getBrandId());
//            if (info.getCode() == 0) {
//                List<BrandVo> brand = info.getData("brand", new TypeReference<List<BrandVo>>() {
//                });
//                StringBuffer buffer = new StringBuffer();
//                String replace = "";
//                for (BrandVo brandVo : brand) {
//                    buffer.append(brandVo.getBrandName() + ";");
//                    replace = replaceQueryString(param,brandVo.getBrandId() + "","brandId");
//                }
//                navVo.setNavValue(buffer.toString());
//                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
//            }
//            navs.add(navVo);
//        }
        //TODO 分类：不需要导航
        return result;
}
}

