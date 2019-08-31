package com.qingcheng.timer;

import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author: huangyibo
 * @Date: 2019/8/29 2:52
 * @Description:
 */

@Component
public class SeckillGoodsTask {

    private Logger logger = LoggerFactory.getLogger(SeckillGoodsTask.class);

    @Autowired
    private RedisTemplate redisTemplate;
    
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    /**
     * 每隔30秒执行一次
     *
     * 0/30从0秒开始执行，在每隔30秒执行一次
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void loadGoods(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        logger.info(simpleDateFormat.format(new Date())+"：开始执行将秒杀商品加载到redis中");
        //1、查询所有时间区间
        List<Date> dateMenus = DateUtil.getDateMenus();
        //2、循环时间区间，查询每个时间区间的秒杀商品
        for (Date startTime : dateMenus) {
            logger.info(simpleDateFormat.format(new Date())+"：开始执行将秒杀商品加载到redis中");
            Example example = new Example(SeckillGoods.class);
            Example.Criteria criteria = example.createCriteria();
            //2.1、商品必须审核通过
            criteria.andEqualTo("status","1");
            //2.2、库存>0
            criteria.andGreaterThan("stockCount",0);
            //2.3、秒杀开始时间>=当前循环时间区间的开始时间
            criteria.andGreaterThanOrEqualTo("startTime",startTime);
            //2.3、秒杀截止时间<当前循环时间区间的开始时间+2小时
            criteria.andLessThan("endTime",DateUtil.addDateHour(startTime,2));
            //2.4、过滤redis中已经存在的该区间的秒杀商品
            Set keys = redisTemplate.boundHashOps("SeckillGoods_" + DateUtil.date2Str(startTime)).keys();
            if(!CollectionUtils.isEmpty(keys)){
                //select * from table where id not in(keys)
                criteria.andNotIn("id",keys);
            }
            //2.5、执行查询
            List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectByExample(example);
            //3、将秒杀商品存入到redis缓存，Hashkey为SeckillGoods_2019090912，为对应的时间区间
            if (!CollectionUtils.isEmpty(seckillGoods)){
                logger.info(simpleDateFormat.format(new Date())+"：共查询到商品数量："+seckillGoods.size());
                for (SeckillGoods seckillGood : seckillGoods) {
                    //完整数据
                    redisTemplate.boundHashOps("SeckillGoods_"+DateUtil.date2Str(startTime)).put(seckillGood.getId(),seckillGood);

                    //剩余库存个数 seckillGood.getStockCount() = 5
                    //     创建独立队列：存储商品剩余库存
                    //      seckillGoodList_110(商品id)  : [110,110,110,110,110]集合中存入5分数据
                    Long[] ids = pushIds(seckillGood.getStockCount(), seckillGood.getId());
                    //创建队列，存储商品剩余库存
                    redisTemplate.boundListOps("SeckillGoodsCountList_"+seckillGood.getId()).leftPushAll(ids);

                    //创建自增Key的值，在减库存的时候使用-1，这样就达到了递减的目的
                    redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGood.getId(),seckillGood.getStockCount());
                }
            }
        }
        logger.info(simpleDateFormat.format(new Date())+"：将秒杀商品加载到redis中执行完毕");
    }

    /**
     *  组装商品id，将商品Id组装成数组
     *  创建独立队列：存储商品剩余库存
     * @param len 商品剩余库存
     * @param id 商品id
     * @return
     */
    private Long[] pushIds(int len,Long id){
        Long[] ids = new Long[len];
        for (int i = 0; i < len; i++) {
            ids[i] = id;
        }
        return ids;
    }
}
