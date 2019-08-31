package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.service.seckill.SeckillGoodsService;
import com.qingcheng.util.DateUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * @author: huangyibo
 * @Date: 2019/8/29 17:21
 * @Description:
 */

@RestController
@RequestMapping("/seckill/goods")
public class SeckillGoodsController {

    @Reference
    private SeckillGoodsService seckillGoodsService;

    /**
     * 加载秒杀页所有时间菜单
     * @return
     */
    @GetMapping("/menus")
    public List<Date> loadMenus(){
        return DateUtil.getDateMenus();
    }

    /**
     * 加载对应时间区间的秒杀商品
     * @param time 2019082918
     * @return
     */
    @GetMapping("/list")
    public List<SeckillGoods> list(String time){
        return seckillGoodsService.list(time);
    }

    /**
     * 根据商品id查询商品详情
     * @param time
     * @param id
     * @return
     */
    @GetMapping("/one")
    public SeckillGoods findOne(String time,Long id){
        return seckillGoodsService.findOne(time,id);
    }
}
