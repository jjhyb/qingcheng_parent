package com.qingcheng.service.goods;

import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.goods.Spu;

import java.util.List;
import java.util.Map;

/**
 * spu业务逻辑层
 */
public interface SpuService {


    public List<Spu> findAll();


    public PageResult<Spu> findPage(int page, int size);


    public List<Spu> findList(Map<String,Object> searchMap);


    public PageResult<Spu> findPage(Map<String,Object> searchMap,int page, int size);


    public Spu findById(String id);

    public void add(Spu spu);


    public void update(Spu spu);


    public void delete(String id);

    /**
     * 新增商品
     * @param goods
     */
    public void saveGoods(Goods goods);

    /**
     * 根据spuId查询Goods包装类
     * @param id
     * @return
     */
    public Goods findGoodsById(String id);

    /**
     * 商品审核
     * @param id 商品id
     * @param status 审核状态
     * @param message 审核不通过的原因，通过为空
     */
    public void audit(String id,String status,String message);

    /**
     * 商品下架
     * @param id
     */
    public void pull(String id);

    /**
     * 商品上架
     * @param id
     */
    public void put(String id);

    /**
     * 批量上架
     * @param ids
     * @return
     */
    public int putMany(String[] ids);

}
