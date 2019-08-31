package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.user.User;
import com.qingcheng.service.user.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: huangyibo
 * @Date: 2019/8/24 18:59
 * @Description:
 */

@RestController
@RequestMapping("/user")
public class UserController {

    @Reference
    private UserService userService;

    /**
     * 发送短信验证码
     * @param phone 手机号
     * @return
     */
    @GetMapping("/sendSms")
    public Result sendSms(String phone){
        String regex = "^((13[0-9])|(14[5,7,9])|(15([0-3]|[5-9]))|(166)|(17[0,1,3,5,6,7,8])|(18[0-9])|(19[8|9]))\\d{8}$";
        if(phone.length() != 11){
            return new Result(401,"手机号应为11位");
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(phone);
        if(!matcher.matches()){
            return new Result(401,"请输入正确的手机号");
        }
        userService.sendSms(phone);
        return new Result();
    }

    @PostMapping("/save")
    public Result save(@RequestBody User user, String smsCode){
        //密码加密
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String newPassword = encoder.encode(user.getPassword());
        user.setPassword(newPassword);
        userService.add(user,smsCode);
        return new Result();
    }
}
