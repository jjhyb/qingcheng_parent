package com.qingcheng.consumer;

import com.alibaba.fastjson.JSON;
import com.aliyuncs.CommonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/24 20:58
 * @Description:
 */
public class SmsMessageConsumer implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(SmsMessageConsumer.class);

    @Autowired
    private SmsUtil smsUtil;

    @Value("${smsCode}")
    private String smsCode;

    @Value("${param}")
    private String param;

    @Override
    public void onMessage(Message message) {
        String jsonString = new String(message.getBody());
        Map<String,String> map = JSON.parseObject(jsonString, Map.class);
        String phone = map.get("phone");//手机号
        String code = map.get("code");//验证码
        logger.info("手机号："+phone+"，验证码："+code);

        //调用阿里云通信发短信
        CommonResponse commonResponse = smsUtil.sendSms(phone, smsCode, param.replace("[value]", code));
        logger.info("注册短信发送成功："+commonResponse.getData());
    }
}
