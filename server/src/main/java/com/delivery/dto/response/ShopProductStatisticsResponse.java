package com.delivery.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 门店商品统计响应
 */
@Data
public class ShopProductStatisticsResponse {

    /**
     * 门店ID
     */
    private Long shopId;

    /**
     * 门店名称
     */
    private String shopName;

    /**
     * 商品统计列表
     */
    private List<ProductStatistics> products;

    /**
     * 商品统计项
     */
    @Data
    public static class ProductStatistics {
        /**
         * 商品ID
         */
        private Long productId;

        /**
         * 商品名称
         */
        private String productName;

        /**
         * 单位
         */
        private String unit;

        /**
         * 订货量总和
         */
        private BigDecimal orderQuantity;

        /**
         * 实际完成量总和
         */
        private BigDecimal completedQuantity;
    }
}
