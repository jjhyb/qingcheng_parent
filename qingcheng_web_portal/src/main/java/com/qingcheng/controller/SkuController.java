package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.SkuService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * @author: huangyibo
 * @Date: 2019/8/22 1:16
 * @Description:
 */

@CrossOrigin
@RestController
@RequestMapping("/sku")
public class SkuController {

    @Reference
    private SkuService skuService;

    @GetMapping("/price")
    public Integer price(String id,HttpServletResponse response){
        return skuService.findPriceById(id);
    }
}
