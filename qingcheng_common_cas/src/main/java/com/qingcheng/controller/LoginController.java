package com.qingcheng.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/26 0:20
 * @Description:
 */

@RestController
@RequestMapping("/login")
public class LoginController {

    private Logger logger = LoggerFactory.getLogger(LoginController.class);

    /**
     * 获取用户名
     * @return
     */
    @GetMapping("/username")
    public Map<String,String> username(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("当前登录用户为：username={}",username);
        if("anonymousUser".equals(username)){   //未登录
            username="";
        }
        Map<String,String> map = new HashMap<>();
        map.put("username",username);
        return map;
    }
}
