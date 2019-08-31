package com.qingcheng.consumer;

import com.alibaba.fastjson.JSON;
import com.qingcheng.pojo.goods.Sku;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/25 1:30
 * @Description:
 *
 * 商品索引更新服务
 */

@Component
public class GoodsIndexUpdateConsumer implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(GoodsIndexUpdateConsumer.class);

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public void onMessage(Message message) {
        try {
            String jsonString = new String(message.getBody(), Charset.forName("UTF-8"));
            List<Sku> skuList = JSON.parseObject(jsonString, List.class);

            //2、封装请求对象
            BulkRequest bulkRequest = new BulkRequest();//BulkRequest可以封装多个IndexRequest

            //这里批量导入数据到elasticsearch中，一次导入9万条数据，ES直接OOM异常了，
            //量太大的话，要分批导入，测试用45000条进行批量导入成功
            if(!CollectionUtils.isEmpty(skuList)){
                for (Sku sku : skuList) {
                    IndexRequest indexRequest = new IndexRequest("sku","doc",sku.getId());
                    Map skuMap = new HashMap();
                    skuMap.put("name",sku.getName());
                    skuMap.put("brandName",sku.getBrandName());
                    skuMap.put("categoryName",sku.getCategoryName());
                    skuMap.put("price",sku.getPrice());
                    skuMap.put("createTime",sku.getCreateTime());
                    skuMap.put("updateTime",sku.getUpdateTime());
                    skuMap.put("image",sku.getImage());
                    skuMap.put("saleNum",sku.getSaleNum());
                    skuMap.put("commentNum",sku.getCommentNum());
                    skuMap.put("spuId",sku.getSpuId());
                    skuMap.put("categoryId",sku.getCategoryId());
                    skuMap.put("weight",sku.getWeight());
                    String spec = sku.getSpec();
                    Map map = JSON.parseObject(spec, Map.class);
                    Map specMap = new HashMap();
                    if(!CollectionUtils.isEmpty(map)){
                        map.forEach((key,value) -> {
                            specMap.put(key,value);
                        });
                    }
                    skuMap.put("spec",specMap);
                    indexRequest.source(skuMap);
                    bulkRequest.add(indexRequest);//可以多次添加
                }
            }

            //3、获取执行结果
            BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            int status = bulkResponse.status().getStatus();
            logger.info("商品索引信息更新，状态：status={}",status);
            String indexMessage = bulkResponse.buildFailureMessage();
            logger.info("商品索引信息更新，详情：indexMessage={}",indexMessage);
        } catch (Exception e) {
            logger.error("商品索引更新异常,e={}",e);
        }
    }
}
