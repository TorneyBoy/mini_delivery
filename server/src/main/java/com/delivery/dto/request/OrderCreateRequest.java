package com.delivery.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建订单请求DTO
 */
@Data
@Schema(description = "创建订单请求")
public class OrderCreateRequest {

    @NotNull(message = "商品列表不能为空")
    @Schema(description = "商品列表")
    private List<OrderItemRequest> items;

    @NotNull(message = "收货日期不能为空")
    @Schema(description = "收货日期")
    private LocalDate deliveryDate;

    /**
     * 订单商品项
     */
    @Data
    @Schema(description = "订单商品项")
    public static class OrderItemRequest {

        @NotNull(message = "商品ID不能为空")
        @Schema(description = "商品ID")
        private Long productId;

        @NotNull(message = "数量不能为空")
        @Schema(description = "数量")
        private java.math.BigDecimal quantity;
    }
}
