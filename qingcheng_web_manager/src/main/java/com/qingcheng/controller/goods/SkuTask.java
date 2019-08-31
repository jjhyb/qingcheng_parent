package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.StockBackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author: huangyibo
 * @Date: 2019/8/27 16:54
 * @Description:
 */

@Component
public class SkuTask {

    private Logger logger = LoggerFactory.getLogger(SkuTask.class);

    @Reference
    private StockBackService stockBackService;

    /**
     * SKU库存回滚任务
     */
    @Scheduled(cron="0 0 0/1 * * ?")//没隔一个小时执行一次
//    @Scheduled(cron="0 * * * * ?")//测试使用每分钟执行一次
    public void skuStockBack(){
        logger.info("SkuTask.skuStockBack Enter in method");
        stockBackService.doBack();
        logger.info("SkuTask.skuStockBack success");
    }
}
