package com.qingcheng.service.impl;

import com.qingcheng.service.business.AdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: huangyibo
 * @Date: 2019/8/21 23:15
 * @Description:
 */

@Component
public class InitCachePre implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(InitCachePre.class);

    @Autowired
    private AdService adService;

    /**
     * spring容器所有bean初始化完成之后会被调用到
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        logger.info("------缓存预热------->将广告位数据放入缓存");
        adService.saveAllAdToRedis();
    }
}
