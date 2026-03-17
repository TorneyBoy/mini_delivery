package com.delivery.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 送货清单响应DTO
 */
@Data
@Schema(description = "送货清单响应")
public class DeliveryListResponse {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "店铺地址")
    private String shopAddress;

    @Schema(description = "店铺手机号")
    private String shopPhone;

    @Schema(description = "状态 0-待送货 1-已完成")
    private Integer status;

    @Schema(description = "送达照片URL")
    private String deliveryPhoto;

    @Schema(description = "完成时间")
    private java.time.LocalDateTime completedAt;

    @Schema(description = "商品汇总列表")
    private List<DeliveryItem> items;

    /**
     * 送货商品项
     */
    @Data
    @Schema(description = "送货商品项")
    public static class DeliveryItem {

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "数量")
        private BigDecimal quantity;

        @Schema(description = "单位")
        private String unit;
    }
}
