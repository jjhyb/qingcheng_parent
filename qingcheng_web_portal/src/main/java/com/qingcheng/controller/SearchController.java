package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.util.WebUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/23 16:17
 * @Description:
 */

@Controller
public class SearchController {

    private Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Reference
    private SkuSearchService skuSearchService;

    @GetMapping("/search")
    public String search(Model model,@RequestParam Map<String, String> searchMap) throws Exception {
        //字符集处理，防止中文乱码
        try {
            searchMap = WebUtil.convertCharsetToUTF8(searchMap);
            //没有页码，默认设置为1
            if(StringUtils.isEmpty(searchMap.get("pageNum"))){
                searchMap.put("pageNum","1");
            }else{
                //页码小于1，默认设置为1
                int pageNum = Integer.parseInt(searchMap.get("pageNum"));
                if(pageNum < 1){
                    searchMap.put("pageNum","1");
                }
            }

            //页面传递给后端两个参数，sort:排序字段，sortOrder:排序规则，升序或降序
            //排序容错处理
            if(StringUtils.isEmpty(searchMap.get("sort"))){//排序字段
                searchMap.put("sort","");
            }
            if(StringUtils.isEmpty(searchMap.get("sortOrder"))){//排序规则
                searchMap.put("sortOrder","DESC");//默认降序
            }

            Map result = skuSearchService.search(searchMap);
            model.addAttribute("result",result);

            //url处理
            StringBuilder url = new StringBuilder("/search.do?");
            searchMap.forEach((key,value) -> {
                url.append("&"+key+"="+value);
            });
            model.addAttribute("url",url);
            model.addAttribute("searchMap",searchMap);
            //这里为了页面上分页显示当前页的效果
            int pageNum = Integer.parseInt(searchMap.get("pageNum"));
            model.addAttribute("pageNum",pageNum);

            //页码处理
            Long totalPages = (Long)result.get("totalPages");//总页数
            int startPage = 1;//开始页码
            int endPage = totalPages.intValue();//截止代码
            if(totalPages > 5){
                startPage = pageNum - 2;
                if(startPage < 1){
                    startPage = 1;
                }
                endPage = startPage + 4;
            }
            model.addAttribute("startPage",startPage);
            model.addAttribute("endPage",endPage);

        } catch (Exception e) {
            logger.error("SearchController.search Exception,searchMap={},e={}",searchMap,e);
        }
        return "search";
    }
}
