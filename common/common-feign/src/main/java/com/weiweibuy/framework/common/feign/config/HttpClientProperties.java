package com.weiweibuy.framework.common.feign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author durenhao
 * @date 2019/10/29 10:59
 **/
@ConfigurationProperties(prefix = "http.client")
@Data
public class HttpClientProperties {
    /**
     * 最大连接个数
     */
    private Integer maxTotal = 300;

    /**
     * 单host 最大连接个数
     */
    private Integer defaultMaxPerRoute = 50;

    /**
     * 连接超时
     */
    private Integer connectTimeout = 1000;

    /**
     * 获取连接超时
     */
    private Integer connectionRequestTimeout = 500;

    /**
     * 读取超时
     */
    private Integer socketTimeout = 5000;
}
