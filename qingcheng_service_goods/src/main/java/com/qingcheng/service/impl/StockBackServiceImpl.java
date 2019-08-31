package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.SkuMapper;
import com.qingcheng.dao.StockBackMapper;
import com.qingcheng.pojo.goods.StockBack;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.StockBackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author: huangyibo
 * @Date: 2019/8/27 16:29
 * @Description:
 */

@Service(interfaceClass = StockBackService.class)
public class StockBackServiceImpl implements StockBackService {

    private Logger logger = LoggerFactory.getLogger(StockBackServiceImpl.class);

    @Autowired
    private StockBackMapper stockBackMapper;

    @Autowired
    private SkuMapper skuMapper;

    /**
     * 添加回滚记录
     * @param orderItemList
     */
    @Transactional
    public void addList(List<OrderItem> orderItemList) {
        for (OrderItem orderItem : orderItemList) {
            StockBack stockBack = new StockBack();
            stockBack.setOrderId(orderItem.getOrderId());
            stockBack.setSkuId(orderItem.getSkuId());
            stockBack.setNum(orderItem.getNum());
            stockBack.setStatus("0");
            stockBack.setCreateTime(new Date());
            stockBackMapper.insertSelective(stockBack);
        }
    }

    /**
     * 执行库存回滚
     */
    @Transactional
    public void doBack() {
        logger.info("库存回滚任务执行开始");
        //查询库存回滚表中状态为0的记录
        StockBack stockBack = new StockBack();
        stockBack.setStatus("0");
        List<StockBack> stockBackList = stockBackMapper.select(stockBack);//回滚列表
        for (StockBack back : stockBackList) {
            //添加库存，调用之前减库存的方法所以加一个减号，-back.getNum()
            skuMapper.deductionStock(back.getSkuId(),-back.getNum());
            //减少销量原理同上
            skuMapper.addSaleNum(back.getSkuId(),-back.getNum());
            back.setStatus("1");
            stockBackMapper.updateByPrimaryKeySelective(back);
        }
        logger.info("库存回滚任务执行结束");
    }

    /**
     * 批量执行库存回滚
     * @param orderItemList 订单项列表
     */
    public void doBack(List<OrderItem> orderItemList) {
        logger.info("库存回滚任务执行开始");
        for (OrderItem orderItem : orderItemList) {
            //添加库存，调用之前减库存的方法所以加一个减号，-back.getNum()
            skuMapper.deductionStock(orderItem.getSkuId(),-orderItem.getNum());
            //减少销量原理同上
            skuMapper.addSaleNum(orderItem.getSkuId(),-orderItem.getNum());
            //记录库存和销量回滚记录
            StockBack stockBack = new StockBack();
            stockBack.setStatus("1");
            stockBack.setCreateTime(new Date());
            stockBack.setBackTime(new Date());
            stockBack.setOrderId(orderItem.getOrderId());
            stockBack.setSkuId(orderItem.getSkuId());
            stockBack.setNum(orderItem.getNum());
            stockBackMapper.updateByPrimaryKeySelective(stockBack);
        }
        logger.info("库存回滚任务执行结束");
    }
}
