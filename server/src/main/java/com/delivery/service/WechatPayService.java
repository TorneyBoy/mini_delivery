package com.delivery.service;

import com.delivery.dto.response.PayResponse;

/**
 * 微信支付服务接口
 */
public interface WechatPayService {

    /**
     * 创建JSAPI支付订单
     * 
     * @param billId 账单ID
     * @param openid 用户openid（店铺管理员的openid）
     * @return 支付参数，用于前端调起支付
     */
    PayResponse createJsapiOrder(Long billId, String openid);

    /**
     * 处理支付回调通知
     * 
     * @param notifyData 回调通知数据
     * @return 处理结果
     */
    boolean handlePayNotify(String notifyData);

    /**
     * 查询订单支付状态
     * 
     * @param billId 账单ID
     * @return 支付状态
     */
    boolean queryPayStatus(Long billId);

    /**
     * 关闭订单
     * 
     * @param billId 账单ID
     */
    void closeOrder(Long billId);
}
