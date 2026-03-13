package com.delivery.controller;

import com.delivery.common.Result;
import com.delivery.common.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Tag(name = "文件上传接口", description = "文件上传相关接口")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    @Value("${file.upload.path:/data/upload/}")
    private String uploadPath;

    /**
     * 上传图片
     */
    @Operation(summary = "上传图片", description = "上传商品图片")
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new Result<>(400, "请选择要上传的文件", null);
        }

        try {
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "unknown";
            }

            // 获取文件扩展名
            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > 0) {
                extension = originalFilename.substring(lastDotIndex);
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return new Result<>(400, "只能上传图片文件", null);
            }

            // 生成新文件名
            String newFilename = UUID.randomUUID().toString() + extension;

            // 创建上传目录（使用绝对路径）
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 保存文件（使用绝对路径）
            Path filePath = uploadDir.resolve(newFilename).toAbsolutePath().normalize();
            file.transferTo(filePath);

            // 返回文件URL
            Map<String, String> result = new HashMap<>();
            result.put("url", "/upload/" + newFilename);
            result.put("filename", newFilename);

            return Result.success(result);
        } catch (IOException e) {
            return new Result<>(500, "文件上传失败: " + e.getMessage(), null);
        }
    }
}
