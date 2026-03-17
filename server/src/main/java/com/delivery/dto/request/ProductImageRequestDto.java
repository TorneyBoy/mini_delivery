package com.delivery.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 司机上传商品图片请求DTO
 */
@Data
public class ProductImageRequestDto {

    /**
     * 商品ID（可选，如果是对现有商品上传图片）
     */
    private Long productId;

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    private String productName;

    /**
     * 类别
     */
    private String category;

    /**
     * 图片URL
     */
    @NotBlank(message = "图片不能为空")
    private String imageUrl;

    /**
     * 描述说明
     */
    private String description;
}
