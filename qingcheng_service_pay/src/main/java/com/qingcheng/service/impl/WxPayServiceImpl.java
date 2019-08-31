package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.wxpay.sdk.Config;
import com.github.wxpay.sdk.WXPayRequest;
import com.github.wxpay.sdk.WXPayUtil;
import com.qingcheng.service.order.WxPayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/27 20:11
 * @Description:
 */

@Service
public class WxPayServiceImpl implements WxPayService {

    private Logger logger = LoggerFactory.getLogger(WxPayServiceImpl.class);

    @Autowired
    private Config config;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    /**
     * 生成微信支付二维码（统一下单）
     * @param orderId 订单号
     * @param money 订单金额(分)
     * @param notifyUrl 回调地址
     * @param attach 附加数据
     * @return
     */
    @Override
    public Map createNative(String orderId, Integer money, String notifyUrl,String... attach) {
        try {
            //1、封装请求参数
            Map<String,String> map = new HashMap<>();
            map.put("appid",config.getAppID());//公众账号ID
            map.put("mch_id",config.getMchID());//商户号
            map.put("nonce_str",WXPayUtil.generateNonceStr());//随机字符串
            map.put("body","青橙秒杀");//商品描述
            map.put("out_trade_no",orderId);//商户订单号
            map.put("total_fee",money+"");//标价金额
            map.put("spbill_create_ip","127.0.0.1");//终端IP
            map.put("notify_url",notifyUrl);//通知地址 --> 微信支付成功之后，微信会自动调用，通知支付已经完成
            map.put("trade_type","NATIVE");//交易类型
            if(attach != null && attach.length > 0){
                map.put("attach",attach[0]);//附加数据
            }


            String xmlParam = WXPayUtil.generateSignedXml(map, config.getKey());//xml格式的参数
            logger.info("微信支付统一下单请求参数： xmlParam={}",xmlParam);

            //2、发送请求
            WXPayRequest wxPayRequest = new WXPayRequest(config);
            String xmlResponse = wxPayRequest.requestWithCert("/pay/unifiedorder", null, xmlParam, false);
            logger.info("微信支付统一下单返回结果： xmlResponse={}",xmlResponse);

            //3、解析返回结果
            Map<String, String> mapResponse = WXPayUtil.xmlToMap(xmlResponse);
            logger.info("微信支付统一下单返回结果： mapResponse={}",mapResponse);
            Map mapResult = new HashMap();
            mapResult.put("code_url",mapResponse.get("code_url"));
            mapResult.put("result_code",mapResponse.get("result_code"));
            mapResult.put("prepay_id",mapResponse.get("prepay_id"));
            mapResult.put("trade_type",mapResponse.get("trade_type"));
            mapResult.put("out_trade_no",orderId);
            mapResult.put("total_fee",money+"");
            return mapResult;
        } catch (Exception e) {
            logger.error("微信支付统一下单异常： e={}",e);
            return new HashMap();
        }
    }

    /**
     * 微信支付回调
     * @param resultXml 微信支付结果回调
     */
    @Override
    public Map<String,String> notifyLogic(String resultXml) {
        Map responseMap = new HashMap();
        responseMap.put("return_code","FAIL");
        responseMap.put("return_msg","报文为空");
        try {
            //1、对xml文件进行解析成Map
            Map<String, String> resultMap = WXPayUtil.xmlToMap(resultXml);
            //2、验证签名
            boolean signatureValid = WXPayUtil.isSignatureValid(resultMap, config.getKey());
            if(signatureValid){
                logger.info("微信支付结果验证签名正确：signatureValid={}",signatureValid);
                if("SUCCESS".equals(resultMap.get("result_code"))){
                    String orderId = resultMap.get("out_trade_no");
                    String payMoney = resultMap.get("total_fee");
                    String transactionId = resultMap.get("transaction_id");
                    //修改订单状态
//                    orderService.updatePayStatus(orderId,transactionId);
//                    logger.info("修改订单状态成功");
                    //返回修改订单状态数据
                    responseMap.put("transactionId",resultMap.get("transaction_id"));
                    responseMap.put("orderId",resultMap.get("out_trade_no"));
                    responseMap.put("payMoney",resultMap.get("total_fee"));//实际开发中需要将支付的金额取出来，用于订单表比对
                    responseMap.put("transactionId",resultMap.get("transaction_id"));
                    responseMap.put("attach",resultMap.get("attach"));//获取用户名

                    //返回微信支付数据数据
                    responseMap.put("return_code","SUCCESS");
                    responseMap.put("return_msg","OK");


                    //发送订单号给rabbitmq，用于支付成功消息推送给前端
                    rabbitTemplate.convertAndSend("paynotify","",orderId);
                }else{
                    logger.info("微信支付结果不通过，支付可能不成功等，记录日志等");
                }
            }else {
                logger.info("微信支付结果验证签名解析不通过，为非法请求，记录日志等");
            }
            return responseMap;
        } catch (Exception e) {
            logger.info("微信支付结果验证签名解析异常：e={}",e);
            return responseMap;
        }
    }

    /**
     * 根据本地订单号查询微信支付结果
     * @param orderId 订单号
     * @return
     */
    @Override
    public Map<String,String> orderQuery(String orderId) {
        Map resultMap = new HashMap();
        try {
            //1、封装请求参数
            Map<String,String> map = new HashMap<>();
            map.put("appid",config.getAppID());//公众账号ID
            map.put("mch_id",config.getMchID());//商户号
            map.put("out_trade_no",orderId);//商户订单号
            map.put("nonce_str",WXPayUtil.generateNonceStr());//随机字符串

            String xmlParam = WXPayUtil.generateSignedXml(map, config.getKey());//xml格式的参数
            logger.info("微信支付查询订单请求参数： xmlParam={}",xmlParam);

            //2、发送请求
            WXPayRequest wxPayRequest = new WXPayRequest(config);
            String responseXml = wxPayRequest.requestWithCert("/pay/orderquery", null, xmlParam, false);
            logger.info("微信支付查询订单返回结果： responseXml={}",responseXml);

            //3、解析返回结果
            Map<String, String> responseMap = WXPayUtil.xmlToMap(responseXml);
            logger.info("微信支付查询订单返回结果： responseMap={}",responseMap);

            //2、验证签名
            boolean signatureValid = WXPayUtil.isSignatureValid(responseMap, config.getKey());
            if(signatureValid){
                logger.info("微信支付查询订单结果验证签名正确：signatureValid={}",signatureValid);
                if("SUCCESS".equals(responseMap.get("result_code")) && !"SUCCESS".equals(responseMap.get("trade_state"))){
                    logger.info("微信支付查询订单["+orderId+"]支付失败");
                    resultMap.put("resultCode",responseMap.get("result_code"));
                    resultMap.put("errCode",responseMap.get("err_code"));
                }
                if("SUCCESS".equals(responseMap.get("result_code")) && "SUCCESS".equals(responseMap.get("trade_state"))){
                    logger.info("微信支付查询订单["+orderId+"]支付成功");
                    resultMap.put("total_fee",responseMap.get("total_fee"));//实际开发中需要将支付的金额取出来，用于订单表比对
                    resultMap.put("transaction_id",responseMap.get("transaction_id"));
                    resultMap.put("trade_state",responseMap.get("trade_state"));
                }
            }else{
                logger.info("微信支付查询订单结果验证签名解析不通过");
            }
        } catch (Exception e) {
            logger.info("微信支付查询订单异常：e={}",e);
        }
        return resultMap;
    }

    /**
     * 根据本地订单号关闭微信支付订单
     * @param orderId 订单号
     */
    @Override
    public Map<String,String> closeOrder(String orderId) {
        try {
            //1、封装请求参数
            Map<String,String> map = new HashMap<>();
            map.put("appid",config.getAppID());//公众账号ID
            map.put("mch_id",config.getMchID());//商户号
            map.put("out_trade_no",orderId);//商户订单号
            map.put("nonce_str",WXPayUtil.generateNonceStr());//随机字符串

            String xmlParam = WXPayUtil.generateSignedXml(map, config.getKey());//xml格式的参数
            logger.info("微信支付关闭订单请求参数： xmlParam={}",xmlParam);

            //2、发送请求
            WXPayRequest wxPayRequest = new WXPayRequest(config);
            String responseXml = wxPayRequest.requestWithCert("/pay/closeorder", null, xmlParam, false);
            logger.info("微信支付关闭订单返回结果： responseXml={}",responseXml);

            //3、解析返回结果
            Map<String, String> responseMap = WXPayUtil.xmlToMap(responseXml);
            logger.info("微信支付关闭订单返回结果： responseMap={}",responseMap);

            //2、验证签名
            boolean signatureValid = WXPayUtil.isSignatureValid(responseMap, config.getKey());
            if(signatureValid){
                logger.info("微信支付关闭订单结果验证签名正确：signatureValid={}",signatureValid);
                return responseMap;
                /*if("SUCCESS".equals(responseMap.get("result_code"))){
                    logger.info("订单["+orderId+"]状态关闭成功");
                }else {
                    String errCode = responseMap.get("err_code");
                    String errCodeDes = responseMap.get("err_code_des");
                    logger.info("订单["+orderId+"]状态关闭失败，原因：errCode={},errCodeDes{}",errCode,errCodeDes);
                }*/
            }else{
                logger.info("微信支付关闭订单结果验证签名不通过");
            }
        } catch (Exception e) {
            logger.error("微信支付关闭订单异常：e={}",e);
        }
        return new HashMap<>();
    }
}
