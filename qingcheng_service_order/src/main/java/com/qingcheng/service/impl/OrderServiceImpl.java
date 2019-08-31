package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.OrderConfigMapper;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.dao.OrderLogMapper;
import com.qingcheng.dao.OrderMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.*;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.goods.StockBackService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import com.qingcheng.util.IdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service(interfaceClass = OrderService.class)
public class OrderServiceImpl implements OrderService {

    private Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderConfigMapper orderConfigMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Autowired
    private CartService cartService;

    @Reference
    private SkuService skuService;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WxPayService wxPayService;

    @Reference
    private StockBackService stockBackService;

    /**
     * 返回全部记录
     * @return
     */
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Order> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Order> orders = (Page<Order>) orderMapper.selectAll();
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Order> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Order> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Order> orders = (Page<Order>) orderMapper.selectByExample(example);
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Order findById(String id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 生成订单
     * @param order
     * @return 返回订单编号和支付金额Map<String,Object>
     */
    public Map<String,Object> add(Order order) {
        //1、获取选中的购物车
        List<Map<String, Object>> cartList = cartService.findNewOrderItemList(order.getUsername());
        List<OrderItem> orderItemList = cartList.stream().filter(cart -> (boolean) cart.get("checked"))
                .map(cart -> (OrderItem) cart.get("item")).collect(Collectors.toList());
        //2、扣减库存
        boolean deduction = skuService.deductionStock(orderItemList);
        if(!deduction){
            throw new RuntimeException("库存不足！");
        }
        try {
            //3、保存订单主表
            order.setId(idWorker.nextId()+"");//订单号
            int totalNum = orderItemList.stream().mapToInt(OrderItem::getNum).sum();//订单商品总数量
            int totalMoney = orderItemList.stream().mapToInt(OrderItem::getMoney).sum();//订单商品总金额
            int preMoney = cartService.preferential(order.getUsername());//满减优惠金额
            order.setTotalNum(totalNum);//订单商品总数量
            order.setTotalMoney(totalMoney);//订单商品总金额
            order.setPayMoney(preMoney);//满减优惠金额
            order.setPayMoney(totalMoney-preMoney);//实际支付金额
            order.setCreateTime(new Date());//订单创建时间
            order.setOrderStatus("0");//订单状态
            order.setPayStatus("0");//支付状态
            order.setConsignStatus("0");//发货状态
            orderMapper.insert(order);

            //4、保存订单明细表
            //这里说明一下，不同店铺不同商品的满减优惠应该分别查出来，这样订单明细表的实际支付才好算出来
            //这里是获取打折比例是对应的总的优惠活动进行的打折
            double proportion = order.getPayMoney()/totalMoney;//打折比例
            for (OrderItem orderItem : orderItemList) {
                orderItem.setId(idWorker.nextId()+"");
                orderItem.setOrderId(order.getId());
                orderItem.setPayMoney((int)(orderItem.getMoney()*proportion));
                orderItemMapper.insert(orderItem);
            }

            //这里利用rabbitmq的延迟队列功能，创建完订单的时候将订单号发送到rabbitmq的延迟队列中(设置时间为1个小时，测试为10秒钟),
            //等时间到的时候在消费延时队列中的数据，取出订单号调用微信支付api中的查询订单api，实现根据订单号查询支付结果。
            //调用微信支付api中的关闭订单api，实现根据订单号关闭微信订单。
            rabbitTemplate.convertAndSend("","queue.order",order.getId());
        } catch (Exception e) {
            logger.error("生成订单和扣减库存失败!,e={}",e);
            //发送回滚消息
            rabbitTemplate.convertAndSend("","queue.skuback", JSON.toJSONString(orderItemList));
            throw new RuntimeException("创建订单失败");
        }

        //5、清除下单的购物车
        cartService.deleteCheckedCart(order.getUsername());
        Map<String,Object> map = new HashMap<>();
        map.put("ordersn",order.getId());
        map.put("money",order.getPayMoney());
        return map;
    }

    /**
     * 修改
     * @param order
     */
    public void update(Order order) {
        orderMapper.updateByPrimaryKeySelective(order);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        orderMapper.deleteByPrimaryKey(id);
    }

    /**
     *
     * @param id
     * @return
     */
    public OrderDetails findOrderDetailsById(String id) {
        Order order = orderMapper.selectByPrimaryKey(id);
        List<OrderItem> orderItemList = new ArrayList<OrderItem>();
        if(order != null){
            Example example = new Example(OrderItem.class);
            example.createCriteria().andEqualTo("orderId",order.getId());
            orderItemList = orderItemMapper.selectByExample(example);
        }
        OrderDetails orderDetails = new OrderDetails();
        orderDetails.setOrder(order);
        orderDetails.setOrderItemList(orderItemList);
        return orderDetails;
    }

    public List<Order> findOrdersById(String[] ids) {
        Example example = new Example(Order.class);
        example.createCriteria().andIn("id", Arrays.asList(ids))
                .andEqualTo("consignStatus","0");
        List<Order> orders = orderMapper.selectByExample(example);
        return orders;
    }

    /**
     * 批量发货
     * @param orders
     */
    public void batchSend(List<Order> orders) {
        //判断运单号和物流公司是否为空
        for (Order order : orders) {
            if(StringUtils.isEmpty(order.getShippingCode()) || StringUtils.isEmpty(order.getShippingName())){
                throw new RuntimeException("请选择快递公司和填写快递单号");
            }
        }
        //循环订单
        for (Order order : orders) {
            order.setOrderStatus("3");//订单状态 已发货
            order.setConsignStatus("2");//发货状态 已发货
            order.setConsignTime(new Date());//发货时间
            orderMapper.updateByPrimaryKeySelective(order);
            //记录订单日志
        }
    }

    /**
     * 订单超时逻辑处理
     */
    @Transactional
    public void orderTimeOutLogic() {
        //订单超时未支付，自动关闭
        //查询超时时间，OrderConfig表中只有一条数据
        OrderConfig orderConfig = orderConfigMapper.selectByPrimaryKey(1);
        Integer orderTimeout = orderConfig.getOrderTimeout();//设置的订单超时时间
        //LocalDateTime.now().minusMinutes(orderTimeout)现在的时间减超时时间，得到已经超时的时间
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(orderTimeout);//得到超时的时间点

        //设置查询条件
        Example example = new Example(Order.class);
        example.createCriteria().andLessThan("createTime",localDateTime)//创建时间小于超时时间
                .andEqualTo("orderStatus","0")//未付款的
                .andEqualTo("isDelete","0");//未删除的

        //超时订单的查询
        List<Order> orders = orderMapper.selectByExample(example);
        for (Order order : orders) {
            //记录订单变动日志
            OrderLog orderLog = new OrderLog();
            orderLog.setId( idWorker.nextId()+"" );
            orderLog.setOperater("system");//系统
            orderLog.setOperateTime(new Date());//当前时间
            orderLog.setOrderStatus("4");//订单状态，关闭
            orderLog.setPayStatus(order.getPayStatus());//订单支付状态，未支付
            orderLog.setConsignStatus(order.getConsignStatus());
            orderLog.setRemarks("超时订单，系统自动关闭");
            orderLog.setOrderId(order.getId());
            orderLogMapper.insert(orderLog);

            //修改订单状态
            order.setOrderStatus("4");//订单状态，关闭
            order.setCloseTime(new Date());//订单关闭时间
            orderMapper.updateByPrimaryKeySelective(order);
        }

    }

    /**
     * 修改订单状态
     * @param orderId 订单ID
     * @param transactionId 微信交易流水号
     */
    @Override
    @Transactional
    public void updatePayStatus(String orderId, String transactionId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if(order != null && "0".equals(order.getPayStatus())){//多余的字段就不判断了
            //修改订单的状态和信息
            order.setPayStatus("1");//支付状态
            order.setOrderStatus("1");//订单状态
            order.setUpdateTime(new Date());//订单修改时间
            order.setPayTime(new Date());//订单支付时间
            order.setTransactionId(transactionId);//微信支付交易流水号
            orderMapper.updateByPrimaryKeySelective(order);

            //记录订单日志
            OrderLog orderLog = new OrderLog();
            orderLog.setId(idWorker.nextId()+"");
            orderLog.setOperater("system");//系统
            orderLog.setOperateTime(new Date());//操作时间
            orderLog.setOrderId(orderId);
            orderLog.setPayStatus("1");//支付状态
            orderLog.setOrderStatus("1");//订单状态
            orderLog.setRemarks("支付流水号："+transactionId);//备注
            orderLogMapper.insertSelective(orderLog);
        }
    }

    /**
     * 微信未支付或支付失败，订单自动关闭
     * @param orderId
     */
    @Override
    @Transactional
    public void closeOrder(String orderId) {
        //1、为调用微信支付关闭订单
        wxPayService.closeOrder(orderId);
        //2、修改订单表的订单状态
        Order order = orderMapper.selectByPrimaryKey(orderId);//查询订单
        if(order != null && "0".equals(order.getPayStatus())){//多余的字段就不判断了
            //修改订单状态
            order.setOrderStatus("4");//订单状态，关闭
            order.setCloseTime(new Date());//订单关闭时间
            orderMapper.updateByPrimaryKeySelective(order);

            //3、记录订单日志
            OrderLog orderLog = new OrderLog();
            orderLog.setId( idWorker.nextId()+"" );
            orderLog.setOperater("system");//系统
            orderLog.setOperateTime(new Date());//当前时间
            orderLog.setOrderStatus("4");//订单状态，关闭
            orderLog.setPayStatus(order.getPayStatus());//订单支付状态，未支付
            orderLog.setConsignStatus(order.getConsignStatus());
            orderLog.setRemarks("超时未支付订单，系统自动关闭");
            orderLog.setOrderId(order.getId());
            orderLogMapper.insertSelective(orderLog);
            //4、恢复商品表库存
            Example example = new Example(OrderItem.class);
            example.createCriteria().andEqualTo("orderId",orderId);
            List<OrderItem> orderItemList = orderItemMapper.selectByExample(example);
            if(!CollectionUtils.isEmpty(orderItemList)){
                stockBackService.doBack(orderItemList);
            }
        }
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 订单id
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 支付类型，1、在线支付、0 货到付款
            if(searchMap.get("payType")!=null && !"".equals(searchMap.get("payType"))){
                criteria.andLike("payType","%"+searchMap.get("payType")+"%");
            }
            // 物流名称
            if(searchMap.get("shippingName")!=null && !"".equals(searchMap.get("shippingName"))){
                criteria.andLike("shippingName","%"+searchMap.get("shippingName")+"%");
            }
            // 物流单号
            if(searchMap.get("shippingCode")!=null && !"".equals(searchMap.get("shippingCode"))){
                criteria.andLike("shippingCode","%"+searchMap.get("shippingCode")+"%");
            }
            // 用户名称
            if(searchMap.get("username")!=null && !"".equals(searchMap.get("username"))){
                criteria.andLike("username","%"+searchMap.get("username")+"%");
            }
            // 买家留言
            if(searchMap.get("buyerMessage")!=null && !"".equals(searchMap.get("buyerMessage"))){
                criteria.andLike("buyerMessage","%"+searchMap.get("buyerMessage")+"%");
            }
            // 是否评价
            if(searchMap.get("buyerRate")!=null && !"".equals(searchMap.get("buyerRate"))){
                criteria.andLike("buyerRate","%"+searchMap.get("buyerRate")+"%");
            }
            // 收货人
            if(searchMap.get("receiverContact")!=null && !"".equals(searchMap.get("receiverContact"))){
                criteria.andLike("receiverContact","%"+searchMap.get("receiverContact")+"%");
            }
            // 收货人手机
            if(searchMap.get("receiverMobile")!=null && !"".equals(searchMap.get("receiverMobile"))){
                criteria.andLike("receiverMobile","%"+searchMap.get("receiverMobile")+"%");
            }
            // 收货人地址
            if(searchMap.get("receiverAddress")!=null && !"".equals(searchMap.get("receiverAddress"))){
                criteria.andLike("receiverAddress","%"+searchMap.get("receiverAddress")+"%");
            }
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andLike("sourceType","%"+searchMap.get("sourceType")+"%");
            }
            // 交易流水号
            if(searchMap.get("transactionId")!=null && !"".equals(searchMap.get("transactionId"))){
                criteria.andLike("transactionId","%"+searchMap.get("transactionId")+"%");
            }
            // 订单状态
            if(searchMap.get("orderStatus")!=null && !"".equals(searchMap.get("orderStatus"))){
                criteria.andLike("orderStatus","%"+searchMap.get("orderStatus")+"%");
            }
            // 支付状态
            if(searchMap.get("payStatus")!=null && !"".equals(searchMap.get("payStatus"))){
                criteria.andLike("payStatus","%"+searchMap.get("payStatus")+"%");
            }
            // 发货状态
            if(searchMap.get("consignStatus")!=null && !"".equals(searchMap.get("consignStatus"))){
                criteria.andLike("consignStatus","%"+searchMap.get("consignStatus")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }

            // 数量合计
            if(searchMap.get("totalNum")!=null ){
                criteria.andEqualTo("totalNum",searchMap.get("totalNum"));
            }
            // 金额合计
            if(searchMap.get("totalMoney")!=null ){
                criteria.andEqualTo("totalMoney",searchMap.get("totalMoney"));
            }
            // 优惠金额
            if(searchMap.get("preMoney")!=null ){
                criteria.andEqualTo("preMoney",searchMap.get("preMoney"));
            }
            // 邮费
            if(searchMap.get("postFee")!=null ){
                criteria.andEqualTo("postFee",searchMap.get("postFee"));
            }
            // 实付金额
            if(searchMap.get("payMoney")!=null ){
                criteria.andEqualTo("payMoney",searchMap.get("payMoney"));
            }

        }
        return example;
    }

}
