package com.qingcheng.service.impl;

import com.qingcheng.dao.BrandMapper;
import com.qingcheng.dao.CategoryMapper;
import com.qingcheng.dao.SpecMapper;
import com.qingcheng.pojo.goods.Category;
import com.qingcheng.util.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/24 0:39
 * @Description:
 *
 * 品牌和规格列表定时存入缓存任务
 */

@Component
public class BrandAndSpecTask {

    private Logger logger = LoggerFactory.getLogger(BrandAndSpecTask.class);

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SpecMapper specMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 每天凌晨1点执行该任务，将品牌列表添加进redis
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void cacheBrand(){
        logger.info("执行该任务，将品牌列表添加进redis开始");
        try {
            Example example = new Example(Category.class);
            example.createCriteria().andEqualTo("isShow","1");
            List<Category> categoryList = categoryMapper.selectByExample(example);
            if(!CollectionUtils.isEmpty(categoryList)) {
                for (Category category : categoryList) {
                    List<Map> brandList = brandMapper.findListByCategoryName(category.getName());
                    //存入redis中
                    redisTemplate.boundHashOps(CacheKey.CATEGORY_BRAND).put(category.getName(),brandList);
                }
            }
            logger.info("执行该任务，将品牌列表添加进redis成功");
        } catch (Exception e) {
            logger.error("将品牌列表添加进redis任务执行异常,e={}",e);
        }
    }

    /**
     * 每天凌晨2点执行该任务，将规格列表添加进redis
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cacheSpec(){
        logger.info("执行该任务，将规格列表添加进redis开始");
        try {
            Example example = new Example(Category.class);
            example.createCriteria().andEqualTo("isShow","1");
            List<Category> categoryList = categoryMapper.selectByExample(example);
            if(!CollectionUtils.isEmpty(categoryList)) {
                for (Category category : categoryList) {
                    List<Map> specList = specMapper.findListByCategoryName(category.getName());
                    //存入redis中
                    redisTemplate.boundHashOps(CacheKey.CATEGORY_SPEC).put(category.getName(),specList);
                }
            }
            logger.info("执行该任务，将规格列表添加进redis成功");
        } catch (Exception e) {
            logger.error("将规格列表添加进redis任务执行异常,e={}",e);
        }
    }
}
