package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.BrandMapper;
import com.qingcheng.dao.SpecMapper;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.util.CacheKey;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author: huangyibo
 * @Date: 2019/8/23 16:03
 * @Description:
 *
 * Sku搜索服务
 */

@Service
public class SkuSearchServiceImpl implements SkuSearchService {

    private Logger logger = LoggerFactory.getLogger(SkuSearchServiceImpl.class);

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SpecMapper specMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    public Map search(Map<String,String> searchMap) {
        //1、封装查询请求
        SearchRequest searchRequest = new SearchRequest("sku");//查询请求对象
        searchRequest.types("doc");//设置查询类型
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();//查询源构建器
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();//布尔查询构建器

        //1.1、关键字搜索
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("name",searchMap.get("keywords"));
        boolQueryBuilder.must(matchQueryBuilder);

        //1.2、商品分类的过滤
        if(!StringUtils.isEmpty(searchMap.get("category"))){
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("categoryName", searchMap.get("category"));
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //1.3、商品品牌的过滤
        if(!StringUtils.isEmpty(searchMap.get("brand"))){
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("brandName", searchMap.get("brand"));
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //1.4、规格过滤
        for(String key: searchMap.keySet()){
            if(key.startsWith("spec.")){//如果是规格参数
                TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery(key+".keyword", searchMap.get(key));
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        //1.5、价格过滤
        if(!StringUtils.isEmpty(searchMap.get("price"))){
            String[] prices = searchMap.get("price").split("-");
            if(!"0".equals(prices[0])){//最低价格不等于0
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").gte(prices[0] + "00");
                boolQueryBuilder.filter(rangeQueryBuilder);
            }
            if(!"*".equals(prices[1])){//如果价格有上限
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").lte(prices[1] + "00");
                boolQueryBuilder.filter(rangeQueryBuilder);
            }
        }

        //聚合查询（商品分类）
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("sku_category").field("categoryName");
        searchSourceBuilder.aggregation(termsAggregationBuilder);
        searchSourceBuilder.query(boolQueryBuilder);

        //分页
        Integer pageNum = Integer.parseInt(searchMap.get("pageNum"));//页码
        Integer pageSize = 30;//页大小
        int fromIndex = (pageNum-1)*pageSize;//计算开始索引
        searchSourceBuilder.from(fromIndex);//开始索引设置
        searchSourceBuilder.size(pageSize);//每页记录数设置

        //排序
        String sort = searchMap.get("sort");//排序极端
        String sortOrder = searchMap.get("sortOrder");//排序规则
        if(!StringUtils.isEmpty(sort)){
            searchSourceBuilder.sort(sort, SortOrder.valueOf(sortOrder));
        }

        //关键字高亮设置
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("name").preTags("<font style='color:red'>").postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);

        searchRequest.source(searchSourceBuilder);



        //2、封装查询结果
        Map resultMap = new HashMap();
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits searchHits = searchResponse.getHits();
            long totalHits = searchHits.totalHits;
            System.out.println("查询记录数："+totalHits);
            SearchHit[] hits = searchHits.getHits();

            //2.1、封装商品列表
            List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
            for (SearchHit hit : hits) {
                Map<String, Object> skuMap = hit.getSourceAsMap();

                //name高亮处理
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField highlightField = highlightFields.get("name");
                Text[] fragments = highlightField.fragments();
                skuMap.put("name",fragments[0].toString());//用高亮内容替换原内容
                resultList.add(skuMap);
            }
            resultMap.put("rows",resultList);

            //2.2、商品分类列表封装
            Aggregations aggregations = searchResponse.getAggregations();
            Map<String, Aggregation> aggregationMap = aggregations.getAsMap();
            Terms terms = (Terms)aggregationMap.get("sku_category");
            List<? extends Terms.Bucket> buckets = terms.getBuckets();
            List<String> categoryList = new ArrayList<String>();
            for (Terms.Bucket bucket : buckets) {
                categoryList.add(bucket.getKeyAsString());
            }
            resultMap.put("categoryList",categoryList);


            //获取参数中为category的字段，因为后面品牌列表和规格列表都要用到这个
            String categoryName = searchMap.get("category");//获取商品分类名称
            if(StringUtils.isEmpty(categoryName)){//如果没有分类条件
                if(!CollectionUtils.isEmpty(categoryList)){
                    categoryName = categoryList.get(0);//提取分类列表的第一个分类
                }
            }
            //2.3、品牌列表
            //过期时间应该用一个范围内的随机数
            Random random = new Random();
            if(StringUtils.isEmpty(searchMap.get("brand"))){//如果参数中没有品牌列表，才去查询
                List<Map> brandList = new ArrayList<Map>();
                if(!StringUtils.isEmpty(categoryName)){
                    brandList = (List<Map>)redisTemplate.boundHashOps(CacheKey.CATEGORY_BRAND).get(categoryName);
                    if(CollectionUtils.isEmpty(brandList)){
                        brandList = brandMapper.findListByCategoryName(categoryName);//查询品牌列表
                        redisTemplate.boundHashOps(CacheKey.CATEGORY_BRAND).put(categoryName,brandList);
                        //过期时间设置为300秒到600秒之间，防止缓存雪崩
                        int expireTime = random.nextInt(300)+300;
                        redisTemplate.expire(categoryName,expireTime,TimeUnit.SECONDS);//设置过期时间为300秒到600秒之间
                    }

                }
                resultMap.put("brandList",brandList);
            }

            //2.4、规格列表
            if(!StringUtils.isEmpty(categoryName)){
                List<Map> specList = new ArrayList<Map>();
                specList = (List<Map>)redisTemplate.boundHashOps(CacheKey.CATEGORY_SPEC).get(categoryName);
                if(CollectionUtils.isEmpty(specList)){
                    specList = specMapper.findListByCategoryName(categoryName);//规格列表
                    redisTemplate.boundHashOps(CacheKey.CATEGORY_SPEC).put(categoryName,specList);
                    //过期时间设置为300秒到600秒之间，防止缓存雪崩
                    int expireTime = random.nextInt(300)+300;
                    redisTemplate.expire(categoryName,expireTime,TimeUnit.SECONDS);//设置过期时间为300秒到600秒之间
                }
                if(!CollectionUtils.isEmpty(specList)){
                    for (Map spec : specList) {
                        //因为数据库中options这列数据为字符串，中间用逗号隔开,这里转成数组，便于前端遍历取数据
                        String[] options = ((String) spec.get("options")).split(",");//规格选项列表
                        spec.put("options",options);
                    }
                }
                resultMap.put("specList",specList);
            }

            //2.5、页码
            long totalCount = searchHits.getTotalHits();//总记录数
            long pageCount = totalCount%pageSize==0?totalCount/pageSize:totalCount/pageSize+1;//计算总页数
            resultMap.put("totalPages",pageCount);
        } catch (IOException e) {
            logger.error("Sku搜索服务异常，e={}",e);
        }

        return resultMap;
    }
}
