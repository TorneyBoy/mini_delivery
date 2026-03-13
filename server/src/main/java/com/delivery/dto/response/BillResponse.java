package com.delivery.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 账单响应DTO
 */
@Data
@Schema(description = "账单响应")
public class BillResponse {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "账单号")
    private String billNo;

    @Schema(description = "账单日期")
    private LocalDate billDate;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "状态 0-待支付 1-已支付")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "支付时间")
    private LocalDateTime paidAt;

    @Schema(description = "微信交易号")
    private String wechatTransactionId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "账单订单列表")
    private List<BillOrderResponse> orders;

    /**
     * 账单订单响应
     */
    @Data
    @Schema(description = "账单订单响应")
    public static class BillOrderResponse {

        @Schema(description = "订单ID")
        private Long orderId;

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "订单金额")
        private BigDecimal orderAmount;

        @Schema(description = "订单商品列表")
        private List<OrderResponse.OrderItemResponse> items;
    }
}
