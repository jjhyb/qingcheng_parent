package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.order.CategoryReportService;
import com.qingcheng.service.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author: huangyibo
 * @Date: 2019/8/18 23:39
 * @Description:
 */

@Component
public class OrderTask {

    private Logger logger = LoggerFactory.getLogger(OrderTask.class);

    @Reference
    private OrderService orderService;

    @Reference
    private CategoryReportService categoryReportService;

    /**
     * 订单超时逻辑处理
     */
    @Scheduled(cron="0 0/2 * * * ?")
    public void orderTimeOutLogic(){
        logger.info("每两分钟间隔执行任务："+new Date());
        orderService.orderTimeOutLogic();
    }

    /**
     * 每天凌晨1点执行该任务，生成类目数据
     */
    @Scheduled(cron="0 0 1 * * ?")
    public void createCategoryReportData(){
        logger.info("每天凌晨1点执行该任务，生成类目数据："+new Date());
        categoryReportService.createData();;
    }
}
