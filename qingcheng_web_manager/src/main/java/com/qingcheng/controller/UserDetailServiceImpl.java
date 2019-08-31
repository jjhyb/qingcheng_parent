package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.system.Admin;
import com.qingcheng.service.system.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/20 16:02
 * @Description:
 */
public class UserDetailServiceImpl implements UserDetailsService {

    private Logger logger = LoggerFactory.getLogger(UserDetailServiceImpl.class);

    @Reference
    private AdminService adminService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("UserDetailServiceImpl.loadUserByUsername Enter in method,username={}",username);
        Map<String,Object> searchMap = new HashMap<>();
        searchMap.put("loginName",username);
        searchMap.put("status","1");
        List<Admin> list = adminService.findList(searchMap);
        if(CollectionUtils.isEmpty(list)){
            return null;
        }
        //构造用户权限集合
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        //获取用户关联的resKey
        List<String> resKeyList = adminService.findResKeyByLoginName(username);
        for (String resKey : resKeyList) {
            grantedAuthorities.add(new SimpleGrantedAuthority(resKey));
        }
        return new User(username,list.get(0).getPassword(),grantedAuthorities);
    }

}
