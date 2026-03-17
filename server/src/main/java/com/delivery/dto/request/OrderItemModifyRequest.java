package com.delivery.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单商品修改请求
 */
@Data
public class OrderItemModifyRequest {

    /**
     * 要修改的订单ID列表
     */
    private List<Long> orderIds;

    /**
     * 修改项列表
     */
    private List<ModifyItem> modifications;

    @Data
    public static class ModifyItem {
        /**
         * 商品ID
         */
        private Long productId;

        /**
         * 商品名称（新增商品时需要）
         */
        private String productName;

        /**
         * 数量变化值（正数增加，负数减少）
         */
        private BigDecimal quantityChange;

        /**
         * 操作类型：modify-修改现有商品, add-新增商品
         */
        private String type;

        /**
         * 单价（新增商品时需要）
         */
        private BigDecimal unitPrice;
    }
}
