package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.AdMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.business.Ad;
import com.qingcheng.service.business.AdService;
import com.qingcheng.util.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AdServiceImpl implements AdService {

    private Logger logger = LoggerFactory.getLogger(AdServiceImpl.class);

    @Autowired
    private AdMapper adMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 返回全部记录
     * @return
     */
    public List<Ad> findAll() {
        return adMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Ad> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Ad> ads = (Page<Ad>) adMapper.selectAll();
        return new PageResult<Ad>(ads.getTotal(),ads.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Ad> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return adMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Ad> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Ad> ads = (Page<Ad>) adMapper.selectByExample(example);
        return new PageResult<Ad>(ads.getTotal(),ads.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Ad findById(Integer id) {
        return adMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param ad
     */
    public void add(Ad ad) {
        adMapper.insert(ad);
        saveAdToRedisByPosition(ad.getPosition());//数据新增后，重新进行数据缓存预热
    }

    /**
     * 修改
     * @param ad
     */
    public void update(Ad ad) {
        //获取更新之前的广告位置
        String position = adMapper.selectByPrimaryKey(ad.getId()).getPosition();
        //执行更新操作
        adMapper.updateByPrimaryKeySelective(ad);
        //如果广告位置没有发生变化
        if(position.equals(ad.getPosition())){
            //更新一次广告为缓存
            saveAdToRedisByPosition(position);
        }else {
            //如果广告位置发生了变化，先更新之前的广告位缓存
            saveAdToRedisByPosition(position);
            //在更新更新后的广告位缓存
            saveAdToRedisByPosition(ad.getPosition());
        }
    }

    /**
     *  删除
     * @param id
     */
    public void delete(Integer id) {
        //根据id获取广告位置
        String position = adMapper.selectByPrimaryKey(id).getPosition();
        adMapper.deleteByPrimaryKey(id);
        saveAdToRedisByPosition(position);//数据新增后，重新进行数据缓存预热
    }

    /**
     * 根据广告位查询广告列表
     * @param position
     * @return
     */
    public List<Ad> findByPosition(String position) {
        //从缓存中根据广告位查询广告列表
        List<Ad> adList = (List<Ad>)redisTemplate.boundHashOps(CacheKey.AD).get(position);
        logger.info("从缓存中根据广告位查询广告列表成功，adList={}", JSON.toJSONString(adList));
        return adList;
    }

    /**
     * 将某个位置的广告存入缓存
     * @param position
     */
    public void saveAdToRedisByPosition(String position) {
        //查询某个位置的广告列表
        Example example = new Example(Ad.class);
        example.createCriteria().andEqualTo("position",position)
                .andLessThanOrEqualTo("startTime",new Date())//开始实际小于等于当前时间
                .andGreaterThanOrEqualTo("endTime",new Date())//截止时间大于等于当前时间
                .andEqualTo("status","1");
        List<Ad> adList = adMapper.selectByExample(example);
        //装入缓存
        redisTemplate.boundHashOps(CacheKey.AD).put(position,adList);
    }

    /**
     * 将全部的广告数据存入缓存
     */
    public void saveAllAdToRedis() {
        //循环所有的广告位置，将其所有位置的广告添加进redis
        List<String> positionList = getPositionList();
        for (String position : positionList) {
            saveAdToRedisByPosition(position);
        }
    }

    /**
     * 返回所有的广告位置
     * @return
     */
    private List<String> getPositionList(){
        List<String> positionList = new ArrayList<String>();
        positionList.add("web_index_lb");
        //这里加载更多的首页广告位
        return positionList;
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Ad.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 广告名称
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
            }
            // 广告位置
            if(searchMap.get("position")!=null && !"".equals(searchMap.get("position"))){
                criteria.andLike("position","%"+searchMap.get("position")+"%");
            }
            // 状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }
            // 图片地址
            if(searchMap.get("image")!=null && !"".equals(searchMap.get("image"))){
                criteria.andLike("image","%"+searchMap.get("image")+"%");
            }
            // URL
            if(searchMap.get("url")!=null && !"".equals(searchMap.get("url"))){
                criteria.andLike("url","%"+searchMap.get("url")+"%");
            }
            // 备注
            if(searchMap.get("remarks")!=null && !"".equals(searchMap.get("remarks"))){
                criteria.andLike("remarks","%"+searchMap.get("remarks")+"%");
            }

            // ID
            if(searchMap.get("id")!=null ){
                criteria.andEqualTo("id",searchMap.get("id"));
            }

        }
        return example;
    }

}
