package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.service.order.CategoryReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/20 1:02
 * @Description:
 */

@RestController
@RequestMapping("/categoryReport")
public class CategoryReportController {

    private Logger logger = LoggerFactory.getLogger(CategoryReportController.class);

    @Reference
    private CategoryReportService categoryReportService;

    /**
     * 昨天的数据统计（商品类目）
     * @return
     */
    @GetMapping("/yesterday")
    public List<CategoryReport> yesterday(){
        LocalDate localDate = LocalDate.now().minusDays(1);//得到昨天的日期
        logger.info("localDate日期为："+localDate.toString());
        return categoryReportService.categoryReport(localDate);
    }

    @GetMapping("/categoryCount")
    public List<Map> categoryCount(String date1,String date2){
        logger.info("CategoryReportController.categoryCount date1={}, date2={}",date1,date2);
        return categoryReportService.categoryCount(date1,date2);
    }
}
