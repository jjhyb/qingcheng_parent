package com.qingcheng.pojo.order;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: huangyibo
 * @Date: 2019/8/18 18:52
 * @Description:
 */
public class OrderDetails implements Serializable {

    private Order order;

    private List<OrderItem> orderItemList = new ArrayList<OrderItem>();

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public List<OrderItem> getOrderItemList() {
        return orderItemList;
    }

    public void setOrderItemList(List<OrderItem> orderItemList) {
        this.orderItemList = orderItemList;
    }
}
