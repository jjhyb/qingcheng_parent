package com.qingcheng.service.order;

import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/27 20:08
 * @Description:
 */
public interface WxPayService {

    /**
     * 生成微信支付二维码（统一下单）
     * @param orderId 订单号
     * @param money 订单金额(分)
     * @param notifyUrl 回调地址
     * @param attach 附加数据
     * @return
     */
    public Map createNative(String orderId,Integer money,String notifyUrl,String... attach);

    /**
     * 微信支付回调
     * @param resultXml 微信支付结果回调
     */
    public Map notifyLogic(String resultXml);

    /**
     * 根据本地订单号查询微信支付结果
     * @param orderId 订单号
     * @return
     */
    public Map<String,String> orderQuery(String orderId);

    /**
     * 根据本地订单号微信支付订单(用户未支付或订单超时才调用)
     * @param orderId 订单号
     * @return
     */
    public Map<String,String> closeOrder(String orderId);

}
