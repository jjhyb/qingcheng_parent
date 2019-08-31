package com.qingcheng.service.impl;

import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
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
    private CategoryService categoryService;

    @Autowired
    private SkuService skuService;

    /**
     * spring容器所有bean初始化完成之后会被调用到
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        categoryService.saveCategoryTreeToRedis();
        logger.info("------缓存预热------->将商品分类树放入缓存");
        skuService.saveAllPriceToRedis();
        logger.info("------缓存预热------->将商品sku价格放入缓存");
    }
}
