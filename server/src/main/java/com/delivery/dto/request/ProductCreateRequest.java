package com.delivery.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建商品请求DTO
 */
@Data
@Schema(description = "创建商品请求")
public class ProductCreateRequest {

    @NotBlank(message = "商品名称不能为空")
    @Schema(description = "商品名称", example = "红富士苹果")
    private String name;

    @Schema(description = "类别", example = "水果")
    private String category;

    @NotNull(message = "老用户价格不能为空")
    @Schema(description = "老用户价格", example = "5.50")
    private BigDecimal oldPrice;

    @NotNull(message = "新用户价格不能为空")
    @Schema(description = "新用户价格", example = "6.00")
    private BigDecimal newPrice;

    @Schema(description = "单位", example = "斤")
    private String unit;

    @Schema(description = "商品说明", example = "新鲜红富士苹果，口感脆甜")
    private String description;

    @Schema(description = "图片URL", example = "https://example.com/image.jpg")
    private String imageUrl;

    @Schema(description = "状态 1-正常 0-下架", example = "1")
    private Integer status = 1;
}
