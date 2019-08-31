package com.qingcheng.service.impl;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * @author: huangyibo
 * @Date: 2019/8/23 15:53
 * @Description:
 */
public class RestClientFactory {

    public static RestHighLevelClient getRestHighLevelClient(String hostname,int port){
        //1、连接rest接口
        HttpHost http = new HttpHost(hostname,port,"http");
        RestClientBuilder restClientBuilder = RestClient.builder(http);//rest构建器
        return new RestHighLevelClient(restClientBuilder);//高级客户端对象(连接)
    }
}
