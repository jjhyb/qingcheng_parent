package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.service.seckill.SeckillGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * @author: huangyibo
 * @Date: 2019/8/29 17:04
 * @Description:
 */

@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据时间区间查询秒杀商品列表
     * @param time
     * @return
     */
    @Override
    public List<SeckillGoods> list(String time) {
        //组装key
        String key = "SeckillGoods_"+time;
        return redisTemplate.boundHashOps(key).values();
    }

    /**
     * 根据商品id查询商品详情
     * @param time 商品描述时间区间
     * @param id 商品Id
     * @return
     */
    @Override
    public SeckillGoods findOne(String time, Long id) {
        //组装key
        String key = "SeckillGoods_"+time;
        return (SeckillGoods)redisTemplate.boundHashOps(key).get(id);
    }
}
