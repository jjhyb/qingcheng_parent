package com.qingcheng.service.order;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderDetails;

import java.util.*;

/**
 * order业务逻辑层
 */
public interface OrderService {


    public List<Order> findAll();


    public PageResult<Order> findPage(int page, int size);


    public List<Order> findList(Map<String,Object> searchMap);


    public PageResult<Order> findPage(Map<String,Object> searchMap,int page, int size);


    public Order findById(String id);

    /**
     * 生成订单
     * @param order
     * @return 返回订单编号和支付金额Map<String,Object>
     */
    public Map<String,Object> add(Order order);


    public void update(Order order);


    public void delete(String id);

    /**
     * 根据订单Id获取订单和订单详情
     * @param id
     * @return
     */
    public OrderDetails findOrderDetailsById(String id);

    /**
     * 根据id集合获取订单集合
     * @param ids
     * @return
     */
    public List<Order> findOrdersById(String[] ids);

    /**
     * 批量发货
     * @param orders
     */
    public void batchSend(List<Order> orders);

    /**
     * 订单超时逻辑处理
     */
    public void orderTimeOutLogic();

    /**
     * 修改订单状态
     * @param orderId 订单ID
     * @param transactionId 微信交易流水号
     */
    public void updatePayStatus(String orderId,String transactionId);

    /**
     * 订单未支付自动关闭
     */
    public void closeOrder(String orderId);

}
