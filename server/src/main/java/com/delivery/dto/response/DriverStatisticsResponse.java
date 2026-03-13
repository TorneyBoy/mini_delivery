package com.delivery.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 司机统计数据响应
 */
@Data
public class DriverStatisticsResponse {
    /**
     * 今日订单数
     */
    private Integer todayOrders;

    /**
     * 今日店铺数
     */
    private Integer todayShops;

    /**
     * 累计订单数
     */
    private Integer totalOrders;

    /**
     * 累计金额
     */
    private BigDecimal totalAmount;
}
