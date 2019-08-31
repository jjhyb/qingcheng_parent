package com.qingcheng.consumer;

import com.alibaba.fastjson.JSON;
import com.qingcheng.pojo.goods.Sku;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
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
import java.util.List;

/**
 * @author: huangyibo
 * @Date: 2019/8/25 1:30
 * @Description:
 *
 * 商品索引删除监听服务
 */

@Component
public class GoodsIndexDeleteConsumer implements MessageListener {

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
            if(!CollectionUtils.isEmpty(skuList)){
                for (Sku sku : skuList) {
                    DeleteRequest deleteRequest = new DeleteRequest("sku", "doc", sku.getId());
                    bulkRequest.add(deleteRequest);
                }
            }
            //3、获取执行结果
            BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            int status = bulkResponse.status().getStatus();
            logger.info("商品索引信息删除，状态：status={}",status);
            String indexMessage = bulkResponse.buildFailureMessage();
            logger.info("商品索引信息删除，详情：indexMessage={}",indexMessage);
        } catch (Exception e) {
            logger.error("商品索引删除异常,e={}",e);
        }
    }
}
