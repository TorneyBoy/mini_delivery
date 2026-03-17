package com.delivery.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序配置
 * 只有在配置了真实的 appid 时才启用
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "wechat.miniapp")
public class WxMaConfig {

    private String appid;
    private String secret;
    private String token;
    private String aesKey;

    /**
     * 初始化微信小程序服务
     * 只有在配置了真实的 appid 时才初始化
     */
    @Bean
    public WxMaService wxMaService() {
        // 如果配置的是占位符或未配置，返回 null（服务将不可用）
        if (appid == null || appid.isEmpty() || "your-appid".equals(appid)) {
            log.warn("微信小程序 appid 未配置或使用占位符，微信消息服务将不可用");
            return null;
        }

        log.info("初始化微信小程序服务...");

        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(appid);
        config.setSecret(secret);
        config.setToken(token);
        config.setAesKey(aesKey);

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);

        return service;
    }
}
