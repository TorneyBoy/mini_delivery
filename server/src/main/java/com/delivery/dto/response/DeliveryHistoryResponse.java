package com.delivery.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 送货历史记录响应
 */
@Data
public class DeliveryHistoryResponse {
    /**
     * 日期
     */
    private String date;

    /**
     * 订单数量
     */
    private Integer orderCount;

    /**
     * 店铺数量
     */
    private Integer shopCount;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;
}
