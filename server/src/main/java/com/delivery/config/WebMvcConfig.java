package com.delivery.config;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:/data/upload/}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源访问路径（使用绝对路径）
        String absolutePath = Paths.get(uploadPath).toAbsolutePath().normalize().toString();
        // 确保路径以文件分隔符结尾
        if (!absolutePath.endsWith(System.getProperty("file.separator"))) {
            absolutePath += System.getProperty("file.separator");
        }
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + absolutePath);
    }
}
