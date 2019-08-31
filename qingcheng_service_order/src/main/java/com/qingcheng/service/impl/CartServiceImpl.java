package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.pojo.goods.Category;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.PreferentialService;
import com.qingcheng.util.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: huangyibo
 * @Date: 2019/8/26 14:40
 * @Description:
 *
 * 购物车服务实现类
 */

@Service(interfaceClass = CartService.class)
public class CartServiceImpl implements CartService {

    private Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Reference
    private SkuService skuService;

    @Reference
    private CategoryService categoryService;

    @Autowired
    private PreferentialService preferentialService;

    /**
     * 从redis中提取某用户的购物车
     * @param username
     * @return
     */
    @Override
    public List<Map<String, Object>> findCartList(String username) {
        logger.info("从redis中提取["+username+"]的购物车");
        List<Map<String, Object>> cartList = (List<Map<String, Object>>)redisTemplate.boundHashOps(CacheKey.CART_LIST).get(username);
        if(CollectionUtils.isEmpty(cartList)){
            cartList = new ArrayList<>();//如果获取购物车为null，则实例化购物车
        }
        return cartList;
    }

    /**
     * 添加商品到购物车
     * @param username 用户名
     * @param skuId 商品id
     * @param num 商品数量
     */
    @Override
    public void addItem(String username, String skuId, Integer num) {
        //实现思路：遍历购物车，如果购物车中存在该商品则累加，如果不存在则添加购物车项
        //获取购物车
        List<Map<String, Object>> cartList = findCartList(username);
        boolean flag = false;//skuId是否在购物车中存在
        for (Map<String, Object> map : cartList) {
            OrderItem orderItem = (OrderItem)map.get("item");
            if(orderItem.getSkuId().equals(skuId)){//购物车中存在该商品
                if(orderItem.getNum() <= 0){//如果购物车中该商品数量小于等于0，那么没有意义
                    cartList.remove(map);//直接移除该商品
                    break;
                }
                int weight = orderItem.getWeight() / orderItem.getNum();//单个商品重量
                orderItem.setNum(orderItem.getNum()+num);//商品数量的变更
                orderItem.setMoney(orderItem.getPrice() * orderItem.getNum());//商品金额的变更
                orderItem.setWeight(weight * orderItem.getNum());//商品重量的变更
                if(orderItem.getNum() <= 0){//如果购物车中改商品数量小于等于0，那么没有意义
                    cartList.remove(map);//直接移除该商品
                    flag = true;//标记原购物车中存在该商品，并且增减该商品数量成功
                    break;
                }
                flag = true;//标记原购物车中存在该商品，并且增减该商品数量成功
                break;
            }
        }
        //如果购物车中没有该商品，则添加
        if(!flag){
            if(num <= 0){//商品数量为0或负数
                throw new RuntimeException("添加的商品数量不合法");
            }
            Sku sku = skuService.findById(skuId);
            if(sku == null){
                throw new RuntimeException("商品不存在");
            }
            if(!"1".equals(sku.getStatus())){
                throw new RuntimeException("商品状态不合法");
            }
            //设置购物车项
            OrderItem orderItem = new OrderItem();
            orderItem.setSkuId(skuId);
            orderItem.setNum(num);
            orderItem.setSpuId(sku.getSpuId());
            orderItem.setImage(sku.getImage());
            orderItem.setPrice(sku.getPrice());
            orderItem.setName(sku.getName());
            orderItem.setMoney(sku.getPrice()*num);//计算该商品的总价格
            if(sku.getWeight() == null){
                sku.setWeight(0);
            }
            orderItem.setWeight(sku.getWeight()*num);//商品重量计算，用于物流

            //设置商品分类
            orderItem.setCategoryId3(sku.getCategoryId());
            Category category3 = (Category)redisTemplate.boundHashOps(CacheKey.CATEGORY).get(sku.getCategoryId());
            if(null == category3){
                category3 = categoryService.findById(sku.getCategoryId());//通过3级分类id找2级
                redisTemplate.boundHashOps(CacheKey.CATEGORY).put(sku.getCategoryId(),category3);
            }
            orderItem.setCategoryId2(category3.getParentId());

            Category category2 = (Category)redisTemplate.boundHashOps(CacheKey.CATEGORY).get(category3.getParentId());
            if(null == category2){
                category2 = categoryService.findById(category3.getParentId());//根据2级分类Id查1级
                redisTemplate.boundHashOps(CacheKey.CATEGORY).put(category3.getParentId(),category2);
            }
            orderItem.setCategoryId1(category2.getParentId());

            Map map = new HashMap();
            map.put("item",orderItem);
            map.put("checked",true);//默认选中
            cartList.add(map);
        }
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);
    }

    /**
     * 更新购物车商品选中状态
     * @param username 用户名
     * @param skuId 商品id
     * @param checked 选中状态
     * @return
     */
    @Override
    public boolean updateChecked(String username, String skuId, boolean checked) {
        //获取购物车
        List<Map<String, Object>> cartList = findCartList(username);
        boolean isOk = false;
        if(!CollectionUtils.isEmpty(cartList)){
            for (Map<String, Object> map : cartList) {
                OrderItem orderItem = (OrderItem)map.get("item");
                if(skuId.equals(orderItem.getSkuId())){
                    map.put("checked",checked);
                    isOk = true;
                }
            }
            if(isOk){
                redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);
            }
        }
        return isOk;
    }

    /**
     * 删除选中的购物车
     * @param username 用户名
     */
    @Override
    public void deleteCheckedCart(String username) {
        //获取未选中购物车
        List<Map<String, Object>> cartList = findCartList(username).stream().filter(cart -> !(Boolean) cart.get("checked"))
                .collect(Collectors.toList());
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);
    }

    /**
     * 计算购物车的优惠金额
     * @param username
     * @return
     */
    @Override
    public int preferential(String username) {
        //获取选中的购物车  List<Map<String, Object>> ---> List<OrderItem>
        List<OrderItem> orderItemList = findCartList(username).stream()
                .filter(cart -> (Boolean) cart.get("checked"))
                .map(cart -> (OrderItem) cart.get("item"))
                .collect(Collectors.toList());
        //按分类聚合统计每个分类的金额  在sql中的话是group by
        // 分类        金额
        //  1          120
        //  2          100
        Map<Integer, Integer> cartMap = orderItemList.stream()
                .collect(Collectors.groupingBy(OrderItem::getCategoryId3, Collectors.summingInt(OrderItem::getMoney)));

        int allPreMoney = 0;//累计优惠金额

        //循环结果，统计每个分类的优惠金额，并累加
        for (Integer categoryId : cartMap.keySet()) {
            //根据品类获取消费金额
            Integer sumMoney = cartMap.get(categoryId);
            //获取品类id和消费金额获取优惠金额
            int preMoney = preferentialService.findPreMoneyByCategoryId(categoryId, sumMoney);
            allPreMoney += preMoney;//将总的优惠金额相加
            logger.info("分类：categoryId="+categoryId+"，消费金额：sumMoney="+sumMoney+"，优惠金额：preMoney="+preMoney);
        }
        logger.info("用户["+username+"]总的优惠金额为："+allPreMoney);
        return allPreMoney;
    }

    /**
     * 获取最新的购物车列表
     * @param username
     * @return
     */
    @Override
    public List<Map<String, Object>> findNewOrderItemList(String username) {
        //获取购物车
        List<Map<String, Object>> cartList = findCartList(username);
        //循环购物车，刷新价格
        for (Map<String, Object> cart : cartList) {
            OrderItem orderItem = (OrderItem) cart.get("item");
            Sku sku = skuService.findById(orderItem.getSkuId());
            orderItem.setPrice(sku.getPrice());//更新价格
            orderItem.setMoney(sku.getPrice() * orderItem.getNum());//更新金额
        }
        //保存最新购物车
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);
        return cartList;
    }
}
