package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.seckill.SeckillStatus;
import com.qingcheng.service.seckill.SeckillOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: huangyibo
 * @Date: 2019/8/29 23:16
 * @Description:
 */

@RestController
@RequestMapping("/seckill/order")
public class SeckillOrderController {

    private Logger logger = LoggerFactory.getLogger(SeckillOrderController.class);

    @Reference
    private SeckillOrderService seckillOrderService;

    /**
     * 用户下单操作
     * @param id 秒杀商品
     * @param time 秒杀商品所在的秒杀时间区间
     * @return
     */
    @GetMapping("/add")
    public Result add(Long id,String time){
        try {
            //获取用户名
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            //如果用户没登录，则提醒用户登录
            if("anonymousUser".equals(username)){
                return new Result(403,"未登录，请先登录!");
            }
            Boolean add = seckillOrderService.add(id, time, username);
            if(add){
                return new Result(0,"抢单成功");
            }
        } catch (Exception e) {
            logger.error("SeckillOrderController.add Exception,e={}",e);
            //将错误信息返回
            return new Result(2,e.getMessage());
        }
        return new Result(1,"抢单失败");
    }

    @GetMapping("/query")
    public Result queryStatus(){
        //获取用户名
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if("anonymousUser".equals(username)){
                return new Result(403,"用户未登录!");
            }
            SeckillStatus seckillStatus = seckillOrderService.queryStatus(username);
            if(seckillStatus != null){
                Result result = new Result(seckillStatus.getStatus(), "抢单状态!");
                result.setOther(seckillStatus);
                return result;
            }
        } catch (Exception e) {
            //0表示抢单失败
            return new Result(0,e.getMessage());
        }
        return new Result(404,"无相关信息!");
    }
}
