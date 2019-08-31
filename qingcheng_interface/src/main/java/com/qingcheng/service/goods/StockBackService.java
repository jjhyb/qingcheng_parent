package com.qingcheng.service.goods;

import com.qingcheng.pojo.order.OrderItem;

import java.util.List;

/**
 * @author: huangyibo
 * @Date: 2019/8/27 16:28
 * @Description:
 */
public interface StockBackService {

    /**
     * 添加回滚记录
     * @param orderItemList
     */
    public void addList(List<OrderItem> orderItemList);

    /**
     * 执行库存回滚
     */
    public void doBack();

    /**
     * 批量执行库存回滚
     * @param orderItemList 订单项列表
     */
    public void doBack(List<OrderItem> orderItemList);
}
