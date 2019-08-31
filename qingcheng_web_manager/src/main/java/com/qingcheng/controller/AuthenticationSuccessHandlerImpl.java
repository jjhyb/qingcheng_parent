package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.system.LoginLog;
import com.qingcheng.service.system.LoginLogService;
import com.qingcheng.util.WebUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * @author: huangyibo
 * @Date: 2019/8/20 17:34
 * @Description:
 */
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    private Logger logger = LoggerFactory.getLogger(AuthenticationSuccessHandlerImpl.class);

    @Reference
    private LoginLogService loginLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //登录之后会调用
        logger.info("AuthenticationSuccessHandlerImpl.onAuthenticationSuccess Enter in method");
        //添加登录日志
        String ip = request.getRemoteAddr();//获取远程登录ip地址
        String agent = request.getHeader("user-agent");//获取头信息
        LoginLog loginLog = new LoginLog();
        loginLog.setLoginName(authentication.getName());//当前登录用户
        loginLog.setLoginTime(new Date());//当前登录时间
        loginLog.setIp(ip);//远程客户端ip
        loginLog.setLocation(WebUtil.getCityByIP(ip));//根据IP地址算出ip地址所在地
        loginLog.setBrowserName(WebUtil.getBrowserName(agent));//浏览器名称
        loginLogService.add(loginLog);
        request.getRequestDispatcher("/main.html").forward(request,response);
    }
}
