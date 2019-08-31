package com.qingcheng.pojo.goods;

import java.io.Serializable;
import java.util.List;

/**
 * @author: huangyibo
 * @Date: 2019/8/18 0:18
 * @Description:
 */
public class Goods implements Serializable {

    private Spu spu;

    private List<Sku> skuList;

    public Spu getSpu() {
        return spu;
    }

    public void setSpu(Spu spu) {
        this.spu = spu;
    }

    public List<Sku> getSkuList() {
        return skuList;
    }

    public void setSkuList(List<Sku> skuList) {
        this.skuList = skuList;
    }
}
