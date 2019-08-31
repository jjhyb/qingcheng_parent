package com.qingcheng.service.seckill;

import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.pojo.seckill.SeckillStatus;

/**
 * @author: huangyibo
 * @Date: 2019/8/30 0:05
 * @Description:
 */
public interface SeckillOrderService {

    /**
     * 秒杀下单实现
     * @param id 秒杀商品id
     * @param time 秒杀商品所在的时间区间
     * @param username 用户名
     * @return
     */
    public Boolean add(Long id,String time,String username);

    /**
     * 查询用户抢单状态
     * @param username
     * @return
     */
    public SeckillStatus queryStatus(String username);

    /**
     * 修改订单
     * @param orderId 订单号
     * @param username 用户名
     * @param transactionId 交易流水号
     */
    public void updateStatus(String orderId,String username,String transactionId);

    /**
     * 根据用户名查询订单号
     * @param username
     * @return
     */
    public SeckillOrder queryByUsername(String username);
}
