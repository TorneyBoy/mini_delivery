package com.delivery.service;

/**
 * 微信订阅消息服务接口
 */
public interface WechatMessageService {

    /**
     * 发送账单生成通知
     * 
     * @param shopId 店铺ID
     * @param billId 账单ID
     * @param amount 账单金额
     */
    void sendBillCreatedNotice(Long shopId, Long billId, String amount);

    /**
     * 发送订单分配通知
     * 
     * @param driverId 司机ID
     * @param orderId  订单ID
     * @param shopName 店铺名称
     */
    void sendOrderAssignedNotice(Long driverId, Long orderId, String shopName);

    /**
     * 发送订单状态变更通知
     * 
     * @param shopId  店铺ID
     * @param orderId 订单ID
     * @param status  订单状态
     */
    void sendOrderStatusNotice(Long shopId, Long orderId, String status);

    /**
     * 发送送货完成通知
     * 
     * @param shopId  店铺ID
     * @param orderId 订单ID
     */
    void sendDeliveryCompleteNotice(Long shopId, Long orderId);

    /**
     * 发送支付成功通知
     * 
     * @param shopId 店铺ID
     * @param billId 账单ID
     * @param amount 支付金额
     */
    void sendPaymentSuccessNotice(Long shopId, Long billId, String amount);
}
