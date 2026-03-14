package com.delivery.config;

import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置
 */
@Data
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
     */
    @Bean
    public RSAAutoCertificateConfig rsaAutoCertificateConfig() {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(merchantSerialNumber)
                .apiV3Key(apiV3Key)
                .build();
    }

    /**
     * JSAPI支付服务
     */
    @Bean
    public JsapiServiceExtension jsapiServiceExtension(RSAAutoCertificateConfig config) {
        return new JsapiServiceExtension.Builder()
                .config(config)
                .build();
    }
}
