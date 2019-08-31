package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.user.Address;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.user.AddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/26 14:47
 * @Description:
 */

@RestController
@RequestMapping("/cart")
public class CartController {

    private Logger logger = LoggerFactory.getLogger(CartController.class);

    @Reference
    private CartService cartService;

    @Reference
    private AddressService addressService;

    @Reference
    private OrderService orderService;

    /**
     * 提取某用户的购物车
     * @return
     */
    @GetMapping("/findCartList")
    public List<Map<String,Object>> findCartList(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Map<String, Object>> cartList = cartService.findCartList(username);
        return cartList;
    }

    /**
     * 添加商品到购物车
     * @param skuId 商品id
     * @param num 商品数量
     * @return
     */
    @GetMapping("/addItem")
    public Result addItem(String skuId,Integer num){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.addItem(username,skuId,num);
        return new Result();
    }

    /**
     * 商品详情页添加购物车，且跳转到购物车
     * @param response
     * @param skuId
     * @param num
     * @throws IOException
     */
    @GetMapping("/buy")
    public void buy(HttpServletResponse response,String skuId,Integer num) throws IOException {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.addItem(username,skuId,num);
        response.sendRedirect("/cart.html");
    }

    /**
     * 更新购物车选项选中状态
     * @param skuId
     * @param checked
     * @return
     */
    @GetMapping("/updateChecked")
    public Result updateChecked(String skuId,boolean checked){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean updateChecked = cartService.updateChecked(username, skuId, checked);
        if(!updateChecked){
            return new Result(501,"更新状态失败");
        }
        return new Result();
    }

    /**
     * 删除选中的购物车选项
     * @return
     */
    @GetMapping("/deleteCheckedCart")
    public Result deleteCheckedCart(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.deleteCheckedCart(username);
        return new Result();
    }

    /**
     * 计算当前购物车优惠金额
     * @return
     */
    @GetMapping("/preferential")
    public Map preferential(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        int preferential = cartService.preferential(username);
        Map map = new HashMap();
        map.put("preferential",preferential);
        return map;
    }

    /**
     * 获取刷新单价后的购物车列表
     * @return
     */
    @GetMapping("/findNewOrderItemList")
    public List<Map<String,Object>> findNewOrderItemList(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return cartService.findNewOrderItemList(username);
    }

    /**
     * 根据用户名查询地址列表
     * @return
     */
    @GetMapping("/findAddressList")
    public List<Address> findAddressList(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Address> addressList = addressService.findByUsername(username);
        if(CollectionUtils.isEmpty(addressList)){
            return new ArrayList<>();
        }
        return addressList;
    }

    /**
     * 保存订单
     * @param order
     * @return
     */
    @PostMapping("/saveOrder")
    public Map<String,Object> saveOrder(@RequestBody Order order){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        order.setUsername(username);
        return orderService.add(order);
    }
}
