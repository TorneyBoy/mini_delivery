package com.delivery.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品响应DTO
 */
@Data
@Schema(description = "商品响应")
public class ProductResponse {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "类别")
    private String category;

    @Schema(description = "老用户价格")
    private BigDecimal oldPrice;

    @Schema(description = "新用户价格")
    private BigDecimal newPrice;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "商品说明")
    private String description;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "状态 1-正常 0-下架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
