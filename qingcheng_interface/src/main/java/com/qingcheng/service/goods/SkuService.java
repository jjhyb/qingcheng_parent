package com.qingcheng.service.goods;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.order.OrderItem;

import java.util.*;

/**
 * sku业务逻辑层
 */
public interface SkuService {


    public List<Sku> findAll();


    public PageResult<Sku> findPage(int page, int size);


    public List<Sku> findList(Map<String,Object> searchMap);


    public PageResult<Sku> findPage(Map<String,Object> searchMap,int page, int size);


    public Sku findById(String id);

    public void add(Sku sku);


    public void update(Sku sku);


    public void delete(String id);

    /**
     * 将全部价格加载到redis
     */
    public void saveAllPriceToRedis();

    /**
     * 根据skuId查询价格
     * @param id
     * @return
     */
    public Integer findPriceById(String id);

    /**
     * 根据sku id更新商品价格到redis中
     * @param id
     * @param price
     */
    public void savePriceToRedisById(String id,Integer price);

    /**
     * 根据sku id删除在redis中缓存的商品价格
     * @param id
     */
    public void deletePriceFromRedis(String id);

    /**
     * 批量扣减库存
     * @param orderItemList 购物车列表
     * @return
     */
    public boolean deductionStock(List<OrderItem> orderItemList);

}
