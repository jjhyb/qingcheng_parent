package com.qingcheng.service.order;

import com.qingcheng.pojo.order.CategoryReport;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/20 0:59
 * @Description:
 */
public interface CategoryReportService {

    public List<CategoryReport> categoryReport(LocalDate localDate);

    public void createData();

    public List<Map> categoryCount(String date1, String date2);
}
