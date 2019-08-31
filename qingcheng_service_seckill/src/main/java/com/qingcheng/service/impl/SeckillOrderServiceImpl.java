package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.dao.SeckillOrderMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.service.seckill.SeckillOrderService;
import com.qingcheng.task.MultiThreadingCreateOrder;
import com.qingcheng.util.IdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;

/**
 * @author: huangyibo
 * @Date: 2019/8/30 0:05
 * @Description:
 */

@Service(interfaceClass = SeckillOrderService.class)
public class SeckillOrderServiceImpl implements SeckillOrderService {

    private Logger logger = LoggerFactory.getLogger(SeckillOrderServiceImpl.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private MultiThreadingCreateOrder multiThreadingCreateOrder;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    /**
     * 秒杀下单实现
     * @param id 秒杀商品id
     * @param time 秒杀商品所在的时间区间
     * @param username 用户名
     * @return
     */
    @Override
    public Boolean add(Long id, String time, String username) {
        //redis自增特性
        //increment(key,value);指定key的值让value自增 -->返回自增后的值 -->这是一个单线程操作
        //A  第一次：increment(username,1) -->value=1
        //   第二次：increment(username,1) -->value=2
        //   利用increment自增，如果value > 1，说明用户多次提交或用户多次排队，这种请求不允许
        Long userQueueCount = redisTemplate.boundHashOps("UserQueueCount").increment(username, 1);
        if(userQueueCount > 1){
            logger.info("--------重复抢单--------");
            //100：表示有重复抢单
            throw new RuntimeException("100");
        }


        //减少无效排队
        Long size = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).size();
        if(size < 0){
            //101表示没有库存
            throw new RuntimeException("101");
        }


        //创建队列所需的排队信息
        SeckillStatus seckillStatus = new SeckillStatus(username,new Date(),1,id,time);

        //将排队信息存入到list集合中，redis中List本身就是一个队列结构
        redisTemplate.boundListOps("SeckillOrderQueue").leftPush(seckillStatus);

        //将抢单状态存入到Redis中，防止前端多次查询是否抢单成功造成的击穿redis缓存
        redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);

        multiThreadingCreateOrder.createOrder();
        logger.info("--------其他程序正在执行--------");
        return true;
    }

    /**
     * 查询用户抢单状态
     * @param username
     * @return
     */
    @Override
    public SeckillStatus queryStatus(String username) {
        return (SeckillStatus)redisTemplate.boundHashOps("UserQueueStatus").get(username);
    }

    /**
     * 修改订单
     * @param orderId 订单号
     * @param username 用户名
     * @param transactionId 交易流水号
     */
    @Override
    public void updateStatus(String orderId, String username, String transactionId) {
        //根据用户名查询订单数据
        SeckillOrder seckillOrder = (SeckillOrder)redisTemplate.boundHashOps("SeckillOrder").get(username);
        if(seckillOrder != null){
            //修改订单-->持久化到mysql中
            seckillOrder.setTransactionId(transactionId);
            seckillOrder.setPayTime(new Date());
            seckillOrder.setStatus("1");//已支付
            //将用户订单存入redis中
            seckillOrderMapper.insertSelective(seckillOrder);

            //用户支付成功后，实时扣减mysql中的商品库存，或者这里不同步，可以写定时任务定时将redis中的秒杀商品库存同步到mysql中
            SeckillGoods seckillGoods = new SeckillGoods();
            seckillGoods.setId(seckillOrder.getSeckillId());
            seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
            seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);

            //清空Redis缓存的订单信息
            redisTemplate.boundHashOps("SeckillOrder").delete(username);

            //清理用户排队信息
            redisTemplate.boundHashOps("UserQueueCount").delete(username);

            //清理用户抢单状态信息
            redisTemplate.boundHashOps("UserQueueStatus").delete(username);
        }

    }

    /**
     * 根据用户名查询订单号
     * @param username
     * @return
     */
    @Override
    public SeckillOrder queryByUsername(String username) {
        return (SeckillOrder)redisTemplate.boundHashOps("SeckillOrder").get(username);
    }
}
