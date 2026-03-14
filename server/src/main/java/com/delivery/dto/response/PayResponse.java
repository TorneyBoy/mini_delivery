package com.delivery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付响应DTO
 * 用于返回前端调起微信支付所需的参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayResponse {

    /**
     * 时间戳（秒）
     */
    private String timeStamp;

    /**
     * 随机字符串
     */
    private String nonceStr;

    /**
     * 订单详情扩展字符串
     */
    private String packageVal;

    /**
     * 签名方式
     */
    private String signType;

    /**
     * 签名
     */
    private String paySign;

    /**
     * 账单ID
     */
    private Long billId;

    /**
     * 账单金额（分）
     */
    private Long totalAmount;

    /**
     * 账单编号
     */
    private String billNo;
}
