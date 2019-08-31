package com.qingcheng.task;

import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.util.IdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author: huangyibo
 * @Date: 2019/8/30 1:22
 * @Description:
 *
 * 多线程异步操作类
 */

@Component
public class MultiThreadingCreateOrder {

    private Logger logger = LoggerFactory.getLogger(MultiThreadingCreateOrder.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 异步操作方法
     * @Async
     */
    @Async
    public void createOrder(){
        try {
            logger.info("----------------准备@Async执行----------------");
            Thread.sleep(10000);

            //将排队信息从redislist集合中取出，redis中List本身就是一个队列结构
            SeckillStatus seckillStatus = (SeckillStatus)redisTemplate.boundListOps("SeckillOrderQueue").rightPop();
            //从redis队列中取出排队数据
            String username = seckillStatus.getUsername();
            Long id = seckillStatus.getGoodsId();
            String time = seckillStatus.getTime();

            //获取队列中的商品id
            Object sId = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).rightPop();
            //售罄
            if(sId == null){
                //清理排队信息
                clearQueue(seckillStatus);
                return ;
            }

            SeckillGoods seckillGoods = null;
            if(seckillStatus != null){
                //查询商品详情
                seckillGoods = (SeckillGoods)redisTemplate.boundHashOps("SeckillGoods_"+time).get(id);
            }

            Thread.sleep(10000);
            System.out.println("--------查询到到的商品库存："+seckillGoods.getStockCount());
            if(seckillGoods != null && seckillGoods.getStockCount() > 0) {
                //创建订单
                //如果有库存，则创建秒杀商品订单
                SeckillOrder seckillOrder = new SeckillOrder();
                seckillOrder.setId(idWorker.nextId());
                seckillOrder.setSeckillId(id);
                seckillOrder.setMoney(seckillGoods.getCostPrice());
                seckillOrder.setUserId(username);
                seckillOrder.setSellerId(seckillGoods.getSellerId());
                seckillOrder.setCreateTime(new Date());
                seckillOrder.setStatus("0");
                //将秒杀订单存入到Redis中
                redisTemplate.boundHashOps("SeckillOrder").put(username, seckillOrder);

                //削减库存
                Long surplusCount  = redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGoods.getId(), -1);//商品数量递减
                seckillGoods.setStockCount(surplusCount.intValue());//根据计数器统计商品剩余库存
                //判断当前商品是否还有库存
                if (surplusCount <= 0) {
                    //并且将商品数据同步到MySQL中
                    seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);
                    //如果没有库存,则清空Redis缓存中该商品
                    redisTemplate.boundHashOps("SeckillGoods_" + time).delete(id);
                } else {
                    //如果有库存，则直数据重置到Reids中
                    redisTemplate.boundHashOps("SeckillGoods_" + time).put(id, seckillGoods);
                }

                seckillStatus.setOrderId(seckillOrder.getId());
                seckillStatus.setMoney(seckillOrder.getMoney());
                seckillStatus.setStatus(2);//抢单成功，待支付
                //变更用户抢单状态
                redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);

                //向rabbitmq发送订单号延时消息，用于用户不付款的库存回滚操作
                sendDelayMessage(seckillStatus);
            }
            logger.info("----------------正在执行----------------");
        } catch (Exception e) {
            logger.info("MultiThreadingCreateOrder.createOrder Exception, e={}",e);
        }
    }

    /**
     * 清理用户排队信息
     * @param seckillStatus
     */
    private void clearQueue(SeckillStatus seckillStatus) {
        //清理重复排队标识
        redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());

        //清理排队存储信息
        redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());
    }


    /***
     * 延时消息发送
     * @param seckillStatus
     */
    public void sendDelayMessage(SeckillStatus seckillStatus){
        rabbitTemplate.convertAndSend(
                "exchange.delay.order.begin",
                "delay",
                JSON.toJSONString(seckillStatus),       //发送数据
                new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        //消息有效期30分钟，测试使用10秒钟
                        message.getMessageProperties().setExpiration(String.valueOf(20000));
                        return message;
                    }
                });
    }
}
