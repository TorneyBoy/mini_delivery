package com.delivery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    /**
     * OSS endpoint，如：oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;

    /**
     * AccessKey ID
     */
    private String accessKeyId;

    /**
     * AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * Bucket名称
     */
    private String bucketName;

    /**
     * 自定义域名（可选）
     * 如果配置了自定义域名，图片URL将使用此域名
     */
    private String customDomain;

    /**
     * 是否启用OSS
     * 默认false，使用本地存储
     */
    private boolean enabled = false;
}
