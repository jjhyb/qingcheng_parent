package com.qingcheng.pojo.goods;

import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * @author: huangyibo
 * @Date: 2019/8/27 16:23
 * @Description:
 */

@Table(name="tb_stock_back")
public class StockBack {

    @Id
    private String orderId;

    @Id
    private String skuId;

    /**
     * 回滚数量
     */
    private Integer num;

    /**
     * 回滚状态
     */
    private String status;

    /**
     * 回滚记录创建时间
     */
    private Date createTime;

    /**
     * 回滚时间
     */
    private Date backTime;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getBackTime() {
        return backTime;
    }

    public void setBackTime(Date backTime) {
        this.backTime = backTime;
    }
}
