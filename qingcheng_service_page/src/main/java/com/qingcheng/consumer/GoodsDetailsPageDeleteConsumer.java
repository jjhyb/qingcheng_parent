package com.qingcheng.consumer;

import com.alibaba.fastjson.JSON;
import com.qingcheng.pojo.goods.Sku;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.List;

/**
 * @author: huangyibo
 * @Date: 2019/8/25 1:30
 * @Description:
 *
 * 商品详细页删除监听服务
 *
 * 由于实际工作中这一步是直接将商品详细页html文件生成到nginx的指定目录下的，
 * 这里要删除文件要用Jsch进行远程操作linux服务器进行文件上传、下载，删除和显示目录信息，详细我的简书博客
 */

@Component
public class GoodsDetailsPageDeleteConsumer implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(GoodsDetailsPageDeleteConsumer.class);

    @Override
    public void onMessage(Message message) {
        String jsonString = new String(message.getBody(), Charset.forName("UTF-8"));
        List<Sku> skuList = JSON.parseObject(jsonString, List.class);
        logger.info("要删除的商品详细页信息：skuList={}",JSON.toJSONString(skuList));
    }
}
