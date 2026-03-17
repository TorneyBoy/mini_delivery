package com.delivery.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 美团商品信息响应DTO
 */
@Data
public class MeituanProductResponse {

    /**
     * 条形码
     */
    private String upcCode;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 规格
     */
    private String spec;

    /**
     * 类目
     */
    private String category;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 单位
     */
    private String unit;

    /**
     * 参考价格
     */
    private BigDecimal price;

    /**
     * 商品描述
     */
    private String description;
}
