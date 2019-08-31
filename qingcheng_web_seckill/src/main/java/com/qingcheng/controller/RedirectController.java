package com.qingcheng.controller;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author: huangyibo
 * @Date: 2019/8/29 23:52
 * @Description:
 */

@Controller
@RequestMapping("/redirect")
public class RedirectController {

    /**
     * 秒杀登录跳转方法
     * @param referer 用户访问该方法的来源页面
     * @return
     */
    @GetMapping("/back")
    public String back(@RequestHeader(value = "Referer",required = false) String referer){
        if(StringUtils.isEmpty(referer)){
            return "/seckill-index.html";
        }else{
            return "redirect:"+referer;
        }
    }
}
