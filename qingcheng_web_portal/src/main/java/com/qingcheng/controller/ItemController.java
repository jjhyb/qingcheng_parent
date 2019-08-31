package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.goods.Spu;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SpuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/21 0:37
 * @Description:
 */

@RestController
@RequestMapping("/item")
public class ItemController {

    private Logger logger = LoggerFactory.getLogger(ItemController.class);

    @Reference
    private SpuService spuService;

    @Reference
    private CategoryService categoryService;

    @Autowired
    private TemplateEngine templateEngine;


    @Value("${pagePath}")
    private String pagePath;

    @GetMapping("/createPage")
    public Result createId(String spuId){
        //查询商品信息
        Goods goods = spuService.findGoodsById(spuId);
        //获取spu信息
        Spu spu = goods.getSpu();
        //获取sku列表
        List<Sku> skuList = goods.getSkuList();

        //查询商品分类
        List<String> categoryList = new ArrayList<>();
        categoryList.add(categoryService.findById(spu.getCategory1Id()).getName());//一级分类
        categoryList.add(categoryService.findById(spu.getCategory2Id()).getName());//二级分类
        categoryList.add(categoryService.findById(spu.getCategory3Id()).getName());//三级分类

        //构建sku地址列表
        Map<String,String> urlMap = new HashMap<>();
        for (Sku sku : skuList) {
            if("1".equals(sku.getStatus())){
                String specJson = JSON.toJSONString(JSON.parseObject(sku.getSpec()),SerializerFeature.MapSortField);
                urlMap.put(specJson,sku.getId()+".html");
            }
        }

        //批量生成页面
        for (Sku sku : skuList) {
            //(1)创建上下文和数据模型
            Context context = new Context();
            Map<String,Object> dataModel = new HashMap<>();
            dataModel.put("spu",spu);
            dataModel.put("sku",sku);
            dataModel.put("categoryList",categoryList);
            dataModel.put("skuImages",sku.getImages().split(","));//sku的图片列表
            dataModel.put("spuImages",spu.getImages().split(","));//spu的图片列表

            Map paraItems = JSON.parseObject(spu.getParaItems());//参数列表
            dataModel.put("paraItems",paraItems);
            Map<String,String> specItems = JSON.parseObject(sku.getSpec(),Map.class);//当前sku规格列表
            dataModel.put("specItems",specItems);

            Map<String,List> specMap = JSON.parseObject(spu.getSpecItems(),Map.class);//规格和规格选项
            for(String key : specMap.keySet()){//循环规格
                List<String> list = specMap.get(key);
                List<Map> mapList = new ArrayList<>();
                //循环规格选项
                for (String value : list) {
                    Map<String,Object> map = new HashMap<>();
                    map.put("option",value);//规格选项
                    if(value.equals(specItems.get(key))){//如果和当前sku规格相同，就是默认选中的
                        map.put("checked",true);//是否选中
                    }else {
                        map.put("checked",false);//是否选中
                    }
                    Map<String,String> spec = JSON.parseObject(sku.getSpec(),Map.class);//当前的sku
                    spec.put(key,value);
                    String specJson = JSON.toJSONString(spec,SerializerFeature.MapSortField);
                    map.put("url",urlMap.get(specJson));
                    mapList.add(map);
                }

                specMap.put(key,mapList);//用新的集合替换原有的集合
            }
            dataModel.put("specMap",specMap);

            context.setVariables(dataModel);
            //(2)准备文件
            File dir = new File(pagePath);
            if(!dir.exists()){
                dir.mkdirs();
            }
            //最终生成的文件
            File dest = new File(dir, sku.getId()+".html");
            //(3)生成页面
            try {
                PrintWriter printWriter = new PrintWriter(dest,"UTF-8");
                templateEngine.process("item",context,printWriter);
                logger.info("生成页面："+sku.getId()+".html");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new Result();
    }
}
