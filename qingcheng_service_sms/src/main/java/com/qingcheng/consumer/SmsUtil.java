package com.qingcheng.consumer;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author: huangyibo
 * @Date: 2019/8/24 22:35
 * @Description:
 */

@Component
public class SmsUtil {

    private Logger logger = LoggerFactory.getLogger(SmsUtil.class);

    @Value("${accessKeyId}")
    private String accessKeyId;

    @Value("${accessKeySecret}")
    private String accessKeySecret;

    public CommonResponse sendSms(String phone,String smsCode,String param){
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("dysmsapi.aliyuncs.com");
        request.setVersion("2017-05-25");
        request.setAction("SendSms");
        request.putQueryParameter("RegionId", "cn-hangzhou");
        request.putQueryParameter("PhoneNumbers", phone);
        request.putQueryParameter("SignName", "水漫庭");
        request.putQueryParameter("TemplateCode", smsCode);
        request.putQueryParameter("TemplateParam", param);
        try {
            CommonResponse response = client.getCommonResponse(request);
            logger.info("短信发送成功");
            return response;
        } catch (ServerException e) {
            logger.info("短信发送失败,e={}",e);
            return null;
        } catch (ClientException e) {
            logger.info("短信发送失败,e={}",e);
            return null;
        }

    }
}
