package com.delivery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 美团开放平台配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "meituan.api")
public class MeituanConfig {

    /**
     * API基础URL
     */
    private String baseUrl = "https://waimaiopen.meituan.com/api/v1";

    /**
     * 应用Key
     */
    private String appKey;

    /**
     * 应用Secret
     */
    private String appSecret;

    /**
     * 门店ID
     */
    private String storeId;
}
