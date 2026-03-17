package com.delivery.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 司机上传商品图片请求响应DTO
 */
@Data
public class ProductImageRequestResponse {

    private Long id;

    private Long driverId;

    private String driverName;

    private Long productId;

    private String productName;

    private String category;

    private String imageUrl;

    private String description;

    private Integer status;

    private String statusText;

    private String rejectReason;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}
