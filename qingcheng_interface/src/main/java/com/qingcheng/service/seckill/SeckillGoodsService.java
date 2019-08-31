package com.qingcheng.service.seckill;

import com.qingcheng.pojo.seckill.SeckillGoods;

import java.util.List;

/**
 * @author: huangyibo
 * @Date: 2019/8/29 17:02
 * @Description:
 *
 *
 */
public interface SeckillGoodsService {

    /**
     * 根据时间区间查询秒杀商品列表
     * @param time
     * @return
     */
    public List<SeckillGoods> list(String time);

    /**
     * 根据商品id查询商品详情
     * @param time 商品描述时间区间
     * @param id 商品Id
     * @return
     */
    public SeckillGoods findOne(String time,Long id);



}
