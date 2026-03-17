package com.delivery.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单详情响应（用于拣货修改）
 */
@Data
public class OrderDetailResponse {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 商品列表
     */
    private List<OrderItemDetail> items;

    @Data
    public static class OrderItemDetail {
        /**
         * 订单项ID
         */
        private Long id;

        /**
         * 商品ID
         */
        private Long productId;

        /**
         * 商品名称
         */
        private String productName;

        /**
         * 数量
         */
        private BigDecimal quantity;

        /**
         * 单价
         */
        private BigDecimal unitPrice;

        /**
         * 小计
         */
        private BigDecimal subtotal;

        /**
         * 单位
         */
        private String unit;
    }
}
