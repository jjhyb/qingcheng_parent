package com.qingcheng.consumer;

import com.alibaba.fastjson.JSON;
import com.qingcheng.pojo.goods.Sku;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: huangyibo
 * @Date: 2019/8/25 1:30
 * @Description:
 *
 * 商品详细页生成监听服务
 */

@Component
public class GoodsDetailsPageGenerateConsumer implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(GoodsDetailsPageGenerateConsumer.class);

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void onMessage(Message message) {
        String jsonString = new String(message.getBody(), Charset.forName("UTF-8"));
        List<Sku> skuList = JSON.parseObject(jsonString, List.class);
        //由于考虑答批量上架，这里对spuId进行遍历，使用set接收souId，因为set元素不可重复
        Set<String> spuIdSet = new HashSet<>();
        if(!CollectionUtils.isEmpty(skuList)){
            for (Sku sku : skuList) {
                spuIdSet.add(sku.getSpuId());
            }
        }
        if(!CollectionUtils.isEmpty(spuIdSet)){
            for (String spuId : spuIdSet) {
                //发送rest请求调用ItemController下的createPage服务
                //这里偷懒，其实应该依赖qingcheng_common_web服务，将ItemController下的createPage服务剪切过来才对
                String detailsPageGenerate = restTemplate.getForObject("http://localhost:9102/item/createPage.do?spuId=" + spuId, String.class);
                logger.info("商品详细页生成结果：detailsPageGenerate={}",detailsPageGenerate);
            }
        }
    }
}
