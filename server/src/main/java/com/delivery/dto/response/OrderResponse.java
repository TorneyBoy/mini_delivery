package com.delivery.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应DTO
 */
@Data
@Schema(description = "订单响应")
public class OrderResponse {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "状态 0-待支付 1-待分配 2-待拣货 3-待送货 4-已完成 5-已取消")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "收货日期")
    private LocalDate deliveryDate;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "司机ID")
    private Long driverId;

    @Schema(description = "司机姓名")
    private String driverName;

    @Schema(description = "收货时间")
    private LocalDateTime receivedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "订单商品列表")
    private List<OrderItemResponse> items;

    /**
     * 订单商品响应
     */
    @Data
    @Schema(description = "订单商品响应")
    public static class OrderItemResponse {

        @Schema(description = "ID")
        private Long id;

        @Schema(description = "商品ID")
        private Long productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "数量")
        private BigDecimal quantity;

        @Schema(description = "单价")
        private BigDecimal unitPrice;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "小计")
        private BigDecimal subtotal;
    }
}
