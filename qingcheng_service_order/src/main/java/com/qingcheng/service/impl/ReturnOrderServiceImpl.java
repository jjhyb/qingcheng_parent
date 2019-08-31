package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.dao.ReturnOrderItemMapper;
import com.qingcheng.dao.ReturnOrderMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.pojo.order.ReturnOrder;
import com.qingcheng.pojo.order.ReturnOrderItem;
import com.qingcheng.service.order.ReturnOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = ReturnOrderService.class)
public class ReturnOrderServiceImpl implements ReturnOrderService {

    @Autowired
    private ReturnOrderMapper returnOrderMapper;

    @Autowired
    private ReturnOrderItemMapper returnOrderItemMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<ReturnOrder> findAll() {
        return returnOrderMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<ReturnOrder> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<ReturnOrder> returnOrders = (Page<ReturnOrder>) returnOrderMapper.selectAll();
        return new PageResult<ReturnOrder>(returnOrders.getTotal(),returnOrders.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<ReturnOrder> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return returnOrderMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<ReturnOrder> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<ReturnOrder> returnOrders = (Page<ReturnOrder>) returnOrderMapper.selectByExample(example);
        return new PageResult<ReturnOrder>(returnOrders.getTotal(),returnOrders.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public ReturnOrder findById(Long id) {
        return returnOrderMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param returnOrder
     */
    public void add(ReturnOrder returnOrder) {
        returnOrderMapper.insert(returnOrder);
    }

    /**
     * 修改
     * @param returnOrder
     */
    public void update(ReturnOrder returnOrder) {
        returnOrderMapper.updateByPrimaryKeySelective(returnOrder);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(Long id) {
        returnOrderMapper.deleteByPrimaryKey(id);
    }

    /**
     * 同意退款
     * 注意：returnOrder这张表是由网站首页用户进行发起的退货
     * @param id
     * @param money
     * @param adminId
     */
    @Transactional
    public void agreeRefund(String id, Integer money, Integer adminId) {
        ReturnOrder returnOrder = returnOrderMapper.selectByPrimaryKey(id);
        if(returnOrder == null){
            throw new RuntimeException("退款订单不存在!");
        }
        if(!"2".equals(returnOrder.getType())){
            throw new RuntimeException("不是退款订单!");
        }
        if(money > returnOrder.getReturnMoney() || money <= 0){
            throw new RuntimeException("退款金额不合法!");
        }
        returnOrder.setReturnMoney(money);//退款金额
        returnOrder.setStatus("1");//同意退款
        returnOrder.setAdminId(adminId);//记录退款管理员
        returnOrder.setDisposeTime(new Date());//处理日期
        returnOrderMapper.updateByPrimaryKeySelective(returnOrder);//保存退款订单

        //调用支付平台的退款接口

    }

    /**
     * 驳回退款请求
     * @param id
     * @param remark
     * @param adminId
     */
    @Transactional
    public void rejectRefund(String id, String remark, Integer adminId) {
        ReturnOrder returnOrder = returnOrderMapper.selectByPrimaryKey(id);
        if(null == returnOrder){
            throw new RuntimeException("退款订单不存在!");
        }
        if(!"2".equals(returnOrder.getType())){
            throw new RuntimeException("不是退款订单!");
        }
        if(remark.length() < 5){
            throw new RuntimeException("请输入驳回理由!");
        }
        //修改属性
        returnOrder.setRemark(remark);//驳回理由
        returnOrder.setStatus("2");//驳回
        returnOrder.setAdminId(adminId);//记录驳回退款请求的管理员
        returnOrder.setDisposeTime(new Date());//处理日期
        returnOrderMapper.updateByPrimaryKeySelective(returnOrder);//保存订单

        //修改对应订单明细的退款状态为未申请
        Example example = new Example(ReturnOrderItem.class);
        example.createCriteria().andEqualTo("returnOrderId",id);
        List<ReturnOrderItem> returnOrderItemList = returnOrderItemMapper.selectByExample(example);
        if(!CollectionUtils.isEmpty(returnOrderItemList)){
            for (ReturnOrderItem returnOrderItem : returnOrderItemList) {
                OrderItem orderItem = new OrderItem();
                orderItem.setId(String.valueOf(returnOrderItem.getOrderItemId()));//获取订单明细Id用于数据存储
                orderItem.setIsReturn("0");
                orderItemMapper.updateByPrimaryKeySelective(orderItem);//更新状态让其可以重新发起退款
            }
        }
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(ReturnOrder.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 用户账号
            if(searchMap.get("userAccount")!=null && !"".equals(searchMap.get("userAccount"))){
                criteria.andLike("userAccount","%"+searchMap.get("userAccount")+"%");
            }
            // 联系人
            if(searchMap.get("linkman")!=null && !"".equals(searchMap.get("linkman"))){
                criteria.andLike("linkman","%"+searchMap.get("linkman")+"%");
            }
            // 联系人手机
            if(searchMap.get("linkmanMobile")!=null && !"".equals(searchMap.get("linkmanMobile"))){
                criteria.andLike("linkmanMobile","%"+searchMap.get("linkmanMobile")+"%");
            }
            // 类型
            if(searchMap.get("type")!=null && !"".equals(searchMap.get("type"))){
                criteria.andLike("type","%"+searchMap.get("type")+"%");
            }
            // 是否退运费
            if(searchMap.get("isReturnFreight")!=null && !"".equals(searchMap.get("isReturnFreight"))){
                criteria.andLike("isReturnFreight","%"+searchMap.get("isReturnFreight")+"%");
            }
            // 申请状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }
            // 凭证图片
            if(searchMap.get("evidence")!=null && !"".equals(searchMap.get("evidence"))){
                criteria.andLike("evidence","%"+searchMap.get("evidence")+"%");
            }
            // 问题描述
            if(searchMap.get("description")!=null && !"".equals(searchMap.get("description"))){
                criteria.andLike("description","%"+searchMap.get("description")+"%");
            }
            // 处理备注
            if(searchMap.get("remark")!=null && !"".equals(searchMap.get("remark"))){
                criteria.andLike("remark","%"+searchMap.get("remark")+"%");
            }

            // 退款金额
            if(searchMap.get("returnMoney")!=null ){
                criteria.andEqualTo("returnMoney",searchMap.get("returnMoney"));
            }
            // 退货退款原因
            if(searchMap.get("returnCause")!=null ){
                criteria.andEqualTo("returnCause",searchMap.get("returnCause"));
            }
            // 管理员id
            if(searchMap.get("adminId")!=null ){
                criteria.andEqualTo("adminId",searchMap.get("adminId"));
            }

        }
        return example;
    }

}
