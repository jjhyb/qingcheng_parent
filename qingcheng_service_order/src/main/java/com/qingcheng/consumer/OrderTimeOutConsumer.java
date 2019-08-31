package com.qingcheng.consumer;

import com.qingcheng.pojo.order.Order;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/28 21:21
 * @Description:
 *
 * 订单超时监听服务
 */

@Component
public class OrderTimeOutConsumer implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(OrderTimeOutConsumer.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private WxPayService wxPayService;

    @Override
    public void onMessage(Message message) {
        String orderId = new String(message.getBody());
        logger.info("订单超时未支付处理开始，orderId={}",orderId);
        //1、先查询业务系统的订单状态
        Order order = orderService.findById(orderId);
        //2、如果订单状态为未支付，调用微信支付查询订单的方法查询
        if(null != order){
            if(!"1".equals(order.getPayStatus())){
                Map map = wxPayService.orderQuery(order.getId());
                //3、如果返回结果是未支付，调用关闭订单的业务逻辑方法
                if(!CollectionUtils.isEmpty(map)){
                    //微信支付的查询支付结果有很多种状态，这里只大概判定是否支付，或支付失败
                    if(!"SUCCESS".equals(map.get("trade_state"))){
                        //这里调用关闭微信订单
                        Map<String, String> resultMap = wxPayService.closeOrder(order.getId());
                        if("SUCCESS".equals(resultMap.get("return_code")) && "SUCCESS".equals(resultMap.get("result_code"))){
                            //关闭本地订单操作，进行库存销量的回滚
                            orderService.closeOrder(order.getId());
                            logger.info("订单超时未支付，关闭订单成功，orderId={}",orderId);
                        }
                    }else{
                        //4、如果返回结果是已支付，实现补偿操作（这里建议记录日志，使用人工干预比较好）
                        orderService.updatePayStatus(orderId,(String)map.get("transaction_id"));
                        logger.info("订单超时已支付，更新本地订单状态，orderId={}",orderId);
                    }
                }
            }
        }
    }
}
