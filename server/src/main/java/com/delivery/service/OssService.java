package com.delivery.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import com.delivery.config.OssConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云OSS文件存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private final OssConfig ossConfig;
    private OSS ossClient;

    @PostConstruct
    public void init() {
        if (ossConfig.isEnabled()) {
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret());
            log.info("OSS客户端初始化成功，Bucket: {}", ossConfig.getBucketName());
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS客户端已关闭");
        }
    }

    /**
     * 上传文件到OSS
     *
     * @param file      要上传的文件
     * @param directory 目录路径，如 "upload/" 或 "images/"
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String directory) throws IOException {
        if (!ossConfig.isEnabled()) {
            throw new IllegalStateException("OSS服务未启用");
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;
        String objectKey = directory + filename;

        // 上传文件
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectResult result = ossClient.putObject(
                    ossConfig.getBucketName(),
                    objectKey,
                    inputStream);
            log.info("文件上传成功: {}", objectKey);
        }

        // 返回访问URL
        return getFileUrl(objectKey);
    }

    /**
     * 上传图片到OSS（默认存放在upload目录）
     *
     * @param file 图片文件
     * @return 图片访问URL
     */
    public String uploadImage(MultipartFile file) throws IOException {
        return uploadFile(file, "upload/");
    }

    /**
     * 获取文件访问URL
     *
     * @param objectKey 对象键
     * @return 访问URL
     */
    public String getFileUrl(String objectKey) {
        // 如果配置了自定义域名，使用自定义域名
        if (ossConfig.getCustomDomain() != null && !ossConfig.getCustomDomain().isEmpty()) {
            String domain = ossConfig.getCustomDomain();
            if (domain.endsWith("/")) {
                return domain + objectKey;
            }
            return domain + "/" + objectKey;
        }

        // 否则使用默认的OSS域名
        // 格式：https://bucket-name.endpoint/objectKey
        return String.format("https://%s.%s/%s",
                ossConfig.getBucketName(),
                ossConfig.getEndpoint(),
                objectKey);
    }

    /**
     * 删除文件
     *
     * @param objectKey 对象键
     */
    public void deleteFile(String objectKey) {
        if (ossConfig.isEnabled() && ossClient != null) {
            ossClient.deleteObject(ossConfig.getBucketName(), objectKey);
            log.info("文件删除成功: {}", objectKey);
        }
    }

    /**
     * 检查OSS服务是否可用
     */
    public boolean isAvailable() {
        return ossConfig.isEnabled() && ossClient != null;
    }
}
