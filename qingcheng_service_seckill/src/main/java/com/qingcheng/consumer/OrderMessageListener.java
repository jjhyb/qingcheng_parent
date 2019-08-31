package com.qingcheng.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.service.order.WxPayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/31 17:43
 * @Description:
 */
public class OrderMessageListener implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(OrderMessageListener.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Reference
    private WxPayService wxPayService;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    /**
     * 消息监听
     * @param message
     */
    @Override
    public void onMessage(Message message) {
        String content = new String(message.getBody());
        logger.info("监听到的消息：content={}",content);

        //订单回滚操作
        rollbackOrder(JSON.parseObject(content,SeckillStatus.class));
    }

    /**
     * 订单回滚操作
     * @param seckillStatus
     */
    public void rollbackOrder(SeckillStatus seckillStatus){
        if(null == seckillStatus){
            return;
        }
        //判断redis中是否存在对应的订单
        SeckillOrder seckillOrder = (SeckillOrder)redisTemplate.boundHashOps("SeckillOrder").get(seckillStatus.getUsername());

        //如果存在开始回滚
        if(null != seckillOrder){
            //1、关闭微信支付
            Map<String, String> resultMap = wxPayService.closeOrder(seckillStatus.getOrderId().toString());
            if("SUCCESS".equals(resultMap.get("return_code")) && "SUCCESS".equals(resultMap.get("result_code"))){
                //2、删除用户订单
                redisTemplate.boundHashOps("SeckillOrder").delete(seckillStatus.getUsername());
                //3、查询出商品数据
                SeckillGoods seckillGoods = (SeckillGoods)redisTemplate.boundHashOps("SeckillGoods_"+seckillStatus.getTime()).get(seckillStatus.getGoodsId());
                if(null == seckillGoods){//如果未支付的是redis中秒杀商品的最后一件
                    seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillStatus.getGoodsId());

                }
                //4、递增库存
                //创建自增Key的值，利用redis的单线程方法increment回滚库存
                Long seckillGoodsCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillStatus.getGoodsId(), 1);
                seckillGoods.setStockCount(seckillGoodsCount.intValue());

                //这里将数据同步到mysql中
                seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);

                //5、将商品数据同步到redis中
                redisTemplate.boundHashOps("SeckillGoods_"+seckillStatus.getTime()).put(seckillStatus.getGoodsId(),seckillGoods);
                redisTemplate.boundListOps("SeckillGoodsCountList_"+seckillStatus.getGoodsId()).leftPush(seckillStatus.getGoodsId());
                //6、清理用户抢单排队信息
                //清理重复排队标识
                redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());

                //清理排队存储信息
                redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());
            }
        }
    }
}
