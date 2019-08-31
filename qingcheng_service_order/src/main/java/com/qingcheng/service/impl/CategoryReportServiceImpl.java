package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.CategoryReportMapper;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.service.order.CategoryReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/20 1:00
 * @Description:
 */

@Service(interfaceClass = CategoryReportService.class)
public class CategoryReportServiceImpl implements CategoryReportService {

    @Autowired
    private CategoryReportMapper categoryReportMapper;

    @Override
    public List<CategoryReport> categoryReport(LocalDate localDate) {
        return categoryReportMapper.categoryReport(localDate);
    }

    @Override
    @Transactional
    public void createData() {
        //查询昨天的类目统计数据
        LocalDate localDate = LocalDate.now().minusDays(1);
        List<CategoryReport> categoryReports = categoryReportMapper.categoryReport(localDate);

        //保存到tb_category_report表中
        for (CategoryReport categoryReport : categoryReports) {
            categoryReportMapper.insert(categoryReport);
        }

    }

    @Override
    public List<Map> categoryCount(String date1, String date2) {
        return categoryReportMapper.categoryCount(date1,date2);
    }
}
