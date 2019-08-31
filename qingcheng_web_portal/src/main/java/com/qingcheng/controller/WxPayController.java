package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.github.wxpay.sdk.WXPayUtil;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/27 21:13
 * @Description:
 */

@RestController
@RequestMapping("/wxpay")
public class WxPayController {

    /**
     * 回调地址
     */
    private static final String notifyUrl = "http://yibo.cross.echosite.cn/wxpay/notify.do";

    private Logger logger = LoggerFactory.getLogger(WxPayController.class);

    @Reference
    private WxPayService wxPayService;

    @Reference
    private OrderService orderService;

    @GetMapping("/createNative")
    public Map createNative(String orderId){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Order order = orderService.findById(orderId);
        if(null != order){
            if("0".equals(order.getPayStatus()) && "0".equals(order.getOrderStatus())
                    && username.equals(order.getUsername())){
                logger.info("微信支付二维码生成成功");
                return wxPayService.createNative(orderId,order.getPayMoney(),notifyUrl);
            }
        }
        logger.info("微信支付二维码生成失败");
        return null;
    }

    /**
     * 支付成功回调
     */
    @PostMapping("/notify")
    public void notifyLogic(HttpServletRequest request,HttpServletResponse response){
        try {
            logger.info("支付成功回调");
            InputStream inputStream = request.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len=0;
            while((len = inputStream.read(buffer)) != -1){
                outputStream.write(buffer,0,len);
            }
            inputStream.close();
            outputStream.close();
            String resultXml = new String(outputStream.toByteArray(),"UTF-8");
            logger.info("支付成功回调，返回结果：resultXml={}",resultXml);
            Map<String,String> responseMap = wxPayService.notifyLogic(resultXml);
            if("SUCCESS".equals(responseMap.get("return_code"))){
                //修改订单状态
                orderService.updatePayStatus(responseMap.get("orderId"),responseMap.get("transactionId"));
            }
            //将结果信息返回给微信支付
            Map<String,String> respMap = new HashMap<>();
            respMap.put("return_code",responseMap.get("return_code"));
            respMap.put("return_msg",responseMap.get("return_msg"));
            PrintWriter writer = response.getWriter();
            writer.write(WXPayUtil.mapToXml(respMap));
            writer.flush();
            writer.close();
        } catch (Exception e) {
            logger.info("支付成功异常：e={}",e);
        }
    }
}
