package com.delivery.config;

import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayConfig {

    private String appid;
    private String mchId;
    private String apiV3Key;
    private String privateKeyPath;
    private String merchantSerialNumber;
    private String notifyUrl;

    /**
     * 初始化微信支付配置
     * 只有在配置了微信支付相关参数时才初始化
     */
    @Bean
    @ConditionalOnProperty(prefix = "wechat.pay", name = "mchId")
    public RSAAutoCertificateConfig rsaAutoCertificateConfig() {
        log.info("初始化微信支付配置...");
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(merchantSerialNumber)
                .apiV3Key(apiV3Key)
                .build();
    }

    /**
     * JSAPI支付服务
     * 只有在配置了微信支付相关参数时才初始化
     */
    @Bean
    @ConditionalOnProperty(prefix = "wechat.pay", name = "mchId")
    public JsapiServiceExtension jsapiServiceExtension(RSAAutoCertificateConfig config) {
        log.info("初始化微信JSAPI支付服务...");
        return new JsapiServiceExtension.Builder()
                .config(config)
                .build();
    }
}
