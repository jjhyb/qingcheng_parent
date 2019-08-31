package com.qingcheng.service.goods;

import java.util.Map;

/**
 * @author: huangyibo
 * @Date: 2019/8/23 16:01
 * @Description:
 *
 * Sku搜索服务接口
 */
public interface SkuSearchService {

    public Map search(Map<String,String> searchMap);
}
