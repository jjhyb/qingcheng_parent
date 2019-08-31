package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.CategoryBrandMapper;
import com.qingcheng.dao.CategoryMapper;
import com.qingcheng.dao.SkuMapper;
import com.qingcheng.dao.SpuMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.*;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.goods.SpuService;
import com.qingcheng.util.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service(interfaceClass = SpuService.class)
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SkuMapper skuMapper;

    /**
     * 分布式Id雪花算法
     */
    @Autowired
    private IdWorker idWorker;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CategoryBrandMapper categoryBrandMapper;

    @Autowired
    private SkuService skuService;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    /**
     * 返回全部记录
     * @return
     */
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Spu> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectAll();
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Spu> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return spuMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Spu> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectByExample(example);
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Spu findById(String id) {
        return spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param spu
     */
    public void add(Spu spu) {
        spuMapper.insert(spu);
    }

    /**
     * 修改
     * @param spu
     */
    public void update(Spu spu) {
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        Map map = new HashMap();
        map.put("spuId",id);
        List<Sku> list = skuService.findList(map);
        for (Sku sku : list) {
            skuService.deletePriceFromRedis(sku.getId());//根据sku id删除在redis中缓存的商品价格
        }

        spuMapper.deleteByPrimaryKey(id);//这里为逻辑删除，此代码为自动生成
        //sku列表的删除，同为逻辑删除
    }

    /**
     * 新增商品
     *
     * 新增和修改公用一个方法
     * @param goods
     */
    @Transactional
    public void saveGoods(Goods goods) {
        //保存一个Spu的商品信息
        Spu spu = goods.getSpu();

        if(null == spu.getId()){//新增数据
            //分布式Id，雪花算法
            spu.setId(String.valueOf(idWorker.nextId()));
            spuMapper.insertSelective(spu);
        }else {//修改数据
            //删除原来的sku列表

            Example example = new Example(Sku.class);
            example.createCriteria().andEqualTo("spuId",spu.getId());
            skuMapper.deleteByExample(example);
            //执行spu的修改
            spuMapper.updateByPrimaryKeySelective(spu);
        }

        //保存Sku列表信息
        List<Sku> skuList = goods.getSkuList();
        Date date = new Date();
        Category category = categoryMapper.selectByPrimaryKey(spu.getCategory3Id());
        for(Sku sku : skuList){
            if(null == sku.getId()){//表示为新增
                sku.setId(String.valueOf(idWorker.nextId()));
                sku.setCreateTime(date);
            }
            sku.setSpuId(spu.getId());

            //不启用规格的sku处理
            if(StringUtils.isEmpty(sku.getSpec())){
                sku.setSpec("{}");
            }

            //sku名称 = spu名称 + 规格值列表
            String name = spu.getName();
            //sku.getSpec() {颜色：红，机身内存：64G}
            Map<String,String> specMap = JSON.parseObject(sku.getSpec(), Map.class);
            for (String value : specMap.values()) {
                name += " "+value;
            }
            sku.setName(name);

            sku.setUpdateTime(date);
            sku.setCategoryId(spu.getCategory3Id());//分类id
            sku.setCategoryName(category.getName());//分类名称
            sku.setCommentNum(0);//刚添加商品，评论数为0
            sku.setSaleNum(0);//刚添加商品，销售数为0
            skuMapper.insertSelective(sku);

            //从新将价格更新到缓存
            skuService.savePriceToRedisById(sku.getId(),sku.getPrice());
        }

        //建立分类和品牌的管理
        CategoryBrand categoryBrand = new CategoryBrand();
        categoryBrand.setCategoryId(spu.getCategory3Id());
        categoryBrand.setBrandId(spu.getBrandId());
        /**
         * 统计查询：
         * 	1、赋值为null或未初始化的实体类对象：统计所有记录数
         * 	2、若设置属性，则按照属性查询符合的记录数
         */
        int count = categoryBrandMapper.selectCount(categoryBrand);
        //如果数据库中不存在这条记录
        if(count == 0){
            categoryBrandMapper.insert(categoryBrand);
        }

    }

    public Goods findGoodsById(String id) {
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);

        //查询sku列表
        Example example = new Example(Sku.class);
        example.createCriteria().andEqualTo("spuId",id);
        List<Sku> skuList = skuMapper.selectByExample(example);

        //封装为组合实体类
        Goods goods = new Goods();
        goods.setSpu(spu);
        goods.setSkuList(skuList);
        return goods;
    }

    /**
     * 商品审核
     * @param id 商品id
     * @param status 审核状态
     * @param message 审核不通过的原因，通过为空
     */
    @Transactional
    public void audit(String id, String status, String message) {
        //1、修改状态 审核状态和上架状态
        //因为spu表数据量太大，先查询在更新效率慢，所以走根据主键id直接更新的策略
        Spu spu = new Spu();
        spu.setId(id);
        spu.setStatus(status);
        if(status == "1"){
            //如果审核通过，自动上架
            spu.setIsMarketable("1");
        }
        spuMapper.updateByPrimaryKeySelective(spu);

        //2、通过spuId查询sku集合，将sku信息发送到rabbitmq进行商品详细页的生成和索引数据的更新
        Example example = new Example(Sku.class);
        example.createCriteria().andEqualTo("spuId",id).andEqualTo("status","1");
        List<Sku> skuList = skuMapper.selectByExample(example);
        if(!CollectionUtils.isEmpty(skuList)){
            //2、1 将sku信息发送到rabbitmq中的商品上架交换器
            rabbitTemplate.convertAndSend("exchange.goods_upper_shelf","",skuList);
        }


        //下面两条，新建表自己实现
        //3、商品审核记录


        //4、记录商品日志
    }

    /**
     * 商品下架
     * @param id
     */
    @Transactional
    public void pull(String id) {
        //1、修改状态
        Spu spu = new Spu();
        spu.setId(id);
        spu.setStatus("0");
        spuMapper.updateByPrimaryKeySelective(spu);

        skuService.deletePriceFromRedis(id);//根据sku id删除在redis中缓存的商品价格

        //2、通过spuId查询sku集合，将sku信息发送到rabbitmq进行商品详细页的删除和索引数据的删除
        Example example = new Example(Sku.class);
        example.createCriteria().andEqualTo("spuId",id).andEqualTo("status","1");
        List<Sku> skuList = skuMapper.selectByExample(example);
        if(!CollectionUtils.isEmpty(skuList)){
            //2、1 将sku信息发送到rabbitmq中的商品上架交换器
            rabbitTemplate.convertAndSend("exchange.goods_lower_shelf","",skuList);
        }

        //3、记录商品日志，需要单独建表实现
        //......
    }

    /**
     * 商品上架
     * @param id
     */
    public void put(String id) {
        //1、修改状态
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if("1" != spu.getStatus()){
            //如果未通过审核，抛出异常
            throw new RuntimeException("此商品未通过审核");
        }
        spu.setIsMarketable("1");
        spuMapper.updateByPrimaryKeySelective(spu);


        //2、通过spuId查询sku集合，将sku信息发送到rabbitmq进行商品详细页的生成和索引数据的更新
        Example example = new Example(Sku.class);
        example.createCriteria().andEqualTo("spuId",id).andEqualTo("status","1");
        List<Sku> skuList = skuMapper.selectByExample(example);
        if(!CollectionUtils.isEmpty(skuList)){
            //2、1 将sku信息发送到rabbitmq中的商品上架交换器
            rabbitTemplate.convertAndSend("exchange.goods_upper_shelf","",skuList);
        }

        //3、记录商品日志
    }

    /**
     * 批量上架
     * @param ids
     * @return
     */
    @Transactional
    public int putMany(String[] ids) {
        //1、修改状态
        Spu spu = new Spu();
        spu.setIsMarketable("1");
        Example example = new Example(Spu.class);
        example.createCriteria().andIn("id", Arrays.asList(ids))
        .andEqualTo("isMarketable","0")//必须是下架的
        .andEqualTo("status","1");//商品审核通过的
        int count = spuMapper.updateByExampleSelective(spu, example);

        //TODO
        //2、通过spuId查询sku集合，将sku信息发送到rabbitmq进行商品详细页的生成和索引数据的更新
        Example skuExample = new Example(Sku.class);
        example.createCriteria().andIn("spuId",Arrays.asList(ids))
                .andEqualTo("status","1");
        List<Sku> skuList = skuMapper.selectByExample(skuExample);
        if(!CollectionUtils.isEmpty(skuList)){
            //2、1 将sku信息发送到rabbitmq中的商品上架交换器
            rabbitTemplate.convertAndSend("exchange.goods_upper_shelf","",skuList);
        }

        //3、添加商品的日志,建表实现

        return count;
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 主键
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 货号
            if(searchMap.get("sn")!=null && !"".equals(searchMap.get("sn"))){
                criteria.andLike("sn","%"+searchMap.get("sn")+"%");
            }
            // SPU名
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
            }
            // 副标题
            if(searchMap.get("caption")!=null && !"".equals(searchMap.get("caption"))){
                criteria.andLike("caption","%"+searchMap.get("caption")+"%");
            }
            // 图片
            if(searchMap.get("image")!=null && !"".equals(searchMap.get("image"))){
                criteria.andLike("image","%"+searchMap.get("image")+"%");
            }
            // 图片列表
            if(searchMap.get("images")!=null && !"".equals(searchMap.get("images"))){
                criteria.andLike("images","%"+searchMap.get("images")+"%");
            }
            // 售后服务
            if(searchMap.get("saleService")!=null && !"".equals(searchMap.get("saleService"))){
                criteria.andLike("saleService","%"+searchMap.get("saleService")+"%");
            }
            // 介绍
            if(searchMap.get("introduction")!=null && !"".equals(searchMap.get("introduction"))){
                criteria.andLike("introduction","%"+searchMap.get("introduction")+"%");
            }
            // 规格列表
            if(searchMap.get("specItems")!=null && !"".equals(searchMap.get("specItems"))){
                criteria.andLike("specItems","%"+searchMap.get("specItems")+"%");
            }
            // 参数列表
            if(searchMap.get("paraItems")!=null && !"".equals(searchMap.get("paraItems"))){
                criteria.andLike("paraItems","%"+searchMap.get("paraItems")+"%");
            }
            // 是否上架
            if(searchMap.get("isMarketable")!=null && !"".equals(searchMap.get("isMarketable"))){
                criteria.andLike("isMarketable","%"+searchMap.get("isMarketable")+"%");
            }
            // 是否启用规格
            if(searchMap.get("isEnableSpec")!=null && !"".equals(searchMap.get("isEnableSpec"))){
                criteria.andLike("isEnableSpec","%"+searchMap.get("isEnableSpec")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }
            // 审核状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }

            // 品牌ID
            if(searchMap.get("brandId")!=null ){
                criteria.andEqualTo("brandId",searchMap.get("brandId"));
            }
            // 一级分类
            if(searchMap.get("category1Id")!=null ){
                criteria.andEqualTo("category1Id",searchMap.get("category1Id"));
            }
            // 二级分类
            if(searchMap.get("category2Id")!=null ){
                criteria.andEqualTo("category2Id",searchMap.get("category2Id"));
            }
            // 三级分类
            if(searchMap.get("category3Id")!=null ){
                criteria.andEqualTo("category3Id",searchMap.get("category3Id"));
            }
            // 模板ID
            if(searchMap.get("templateId")!=null ){
                criteria.andEqualTo("templateId",searchMap.get("templateId"));
            }
            // 运费模板id
            if(searchMap.get("freightId")!=null ){
                criteria.andEqualTo("freightId",searchMap.get("freightId"));
            }
            // 销量
            if(searchMap.get("saleNum")!=null ){
                criteria.andEqualTo("saleNum",searchMap.get("saleNum"));
            }
            // 评论数
            if(searchMap.get("commentNum")!=null ){
                criteria.andEqualTo("commentNum",searchMap.get("commentNum"));
            }

        }
        return example;
    }

}
