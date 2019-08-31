package com.qingcheng.consumer;

import com.alibaba.fastjson.JSON;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.StockBackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author: huangyibo
 * @Date: 2019/8/27 16:36
 * @Description:
 */
public class BackMessageConsumer implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(BackMessageConsumer.class);

    @Autowired
    private StockBackService stockBackService;

    public void onMessage(Message message) {
        try {
            //提取消息
            String jsonString = new String(message.getBody());
            List<OrderItem> orderItemList = JSON.parseArray(jsonString, OrderItem.class);
            if(!CollectionUtils.isEmpty(orderItemList)){
                stockBackService.addList(orderItemList);
            }
        } catch (Exception e) {
            logger.error("回滚消息添加失败,e={}", e);
            //人工干预
        }
    }
}
