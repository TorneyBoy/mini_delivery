package com.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delivery.config.WechatPayConfig;
import com.delivery.dto.response.PayResponse;
import com.delivery.entity.Bill;
import com.delivery.entity.Shop;
import com.delivery.enums.BillStatus;
import com.delivery.exception.BusinessException;
import com.delivery.mapper.BillMapper;
import com.delivery.mapper.ShopMapper;
import com.delivery.service.WechatPayService;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 微信支付服务实现
 * 只有在配置了微信支付相关参数时才启用
 */
@Slf4j
@Service
@ConditionalOnBean(JsapiServiceExtension.class)
@RequiredArgsConstructor
public class WechatPayServiceImpl implements WechatPayService {

    private final JsapiServiceExtension jsapiServiceExtension;
    private final WechatPayConfig wechatPayConfig;
    private final RSAAutoCertificateConfig rsaAutoCertificateConfig;
    private final BillMapper billMapper;
    private final ShopMapper shopMapper;

    @Override
    public PayResponse createJsapiOrder(Long billId, String openid) {
        // 查询账单
        Bill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw new BusinessException("账单不存在");
        }

        // 检查账单状态
        if (bill.getStatus() == BillStatus.PAID.getCode()) {
            throw new BusinessException("账单已支付");
        }

        // 查询店铺信息
        Shop shop = shopMapper.selectById(bill.getShopId());
        if (shop == null) {
            throw new BusinessException("店铺不存在");
        }

        // 构建订单号（使用账单号）
        String outTradeNo = bill.getBillNo();

        // 构建金额（转换为分）
        Long total = bill.getTotalAmount().multiply(new BigDecimal("100")).longValue();

        // 构建预支付请求
        PrepayRequest request = new PrepayRequest();
        request.setAppid(wechatPayConfig.getAppid());
        request.setMchid(wechatPayConfig.getMchId());
        request.setDescription("配送账单支付-" + bill.getBillNo());
        request.setNotifyUrl(wechatPayConfig.getNotifyUrl());
        request.setOutTradeNo(outTradeNo);

        // 设置金额
        Amount amount = new Amount();
        amount.setTotal(total.intValue());
        amount.setCurrency("CNY");
        request.setAmount(amount);

        // 设置支付者
        Payer payer = new Payer();
        payer.setOpenid(openid);
        request.setPayer(payer);

        try {
            // 调用JSAPI下单
            PrepayWithRequestPaymentResponse response = jsapiServiceExtension.prepayWithRequestPayment(request);

            log.info("创建微信支付订单成功，账单ID: {}, 订单号: {}", billId, outTradeNo);

            return PayResponse.builder()
                    .timeStamp(response.getTimeStamp())
                    .nonceStr(response.getNonceStr())
                    .packageVal(response.getPackageVal())
                    .signType(response.getSignType())
                    .paySign(response.getPaySign())
                    .billId(billId)
                    .totalAmount(total)
                    .billNo(bill.getBillNo())
                    .build();

        } catch (Exception e) {
            log.error("创建微信支付订单失败", e);
            throw new BusinessException("创建支付订单失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePayNotify(String notifyData) {
        log.info("收到微信支付回调通知");

        try {
            // 解析回调通知
            NotificationParser parser = new NotificationParser((NotificationConfig) rsaAutoCertificateConfig);

            // 这里需要从HTTP请求中获取相关头信息，实际使用时需要传入
            // 简化处理，实际项目中需要获取完整的请求头
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber("")
                    .nonce("")
                    .signature("")
                    .timestamp("")
                    .body(notifyData)
                    .build();

            Transaction transaction = parser.parse(requestParam, Transaction.class);

            // 验证支付结果
            if (transaction == null) {
                log.error("解析支付回调数据失败");
                return false;
            }

            // 获取订单号
            String outTradeNo = transaction.getOutTradeNo();
            String transactionId = transaction.getTransactionId();

            // 查询账单
            Bill bill = billMapper.selectOne(
                    new LambdaQueryWrapper<Bill>().eq(Bill::getBillNo, outTradeNo));

            if (bill == null) {
                log.error("账单不存在，订单号: {}", outTradeNo);
                return false;
            }

            // 检查是否已支付
            if (bill.getStatus() == BillStatus.PAID.getCode()) {
                log.info("账单已支付，跳过处理，账单ID: {}", bill.getId());
                return true;
            }

            // 更新账单状态
            bill.setStatus(BillStatus.PAID.getCode());
            bill.setPaidAt(LocalDateTime.now());
            bill.setWechatTransactionId(transactionId);
            billMapper.updateById(bill);

            log.info("账单支付成功，账单ID: {}, 微信交易号: {}", bill.getId(), transactionId);
            return true;

        } catch (Exception e) {
            log.error("处理支付回调失败", e);
            return false;
        }
    }

    @Override
    public boolean queryPayStatus(Long billId) {
        Bill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw new BusinessException("账单不存在");
        }

        // 如果已经是已支付状态，直接返回
        if (bill.getStatus() == BillStatus.PAID.getCode()) {
            return true;
        }

        try {
            // 查询微信支付订单状态
            QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
            queryRequest.setMchid(wechatPayConfig.getMchId());
            queryRequest.setOutTradeNo(bill.getBillNo());

            Transaction transaction = jsapiServiceExtension.queryOrderByOutTradeNo(queryRequest);

            if (transaction != null &&
                    Transaction.TradeStateEnum.SUCCESS == transaction.getTradeState()) {
                // 更新账单状态
                bill.setStatus(BillStatus.PAID.getCode());
                bill.setPaidAt(LocalDateTime.now());
                bill.setWechatTransactionId(transaction.getTransactionId());
                billMapper.updateById(bill);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("查询支付状态失败", e);
            return false;
        }
    }

    @Override
    public void closeOrder(Long billId) {
        Bill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw new BusinessException("账单不存在");
        }

        try {
            CloseOrderRequest closeRequest = new CloseOrderRequest();
            closeRequest.setMchid(wechatPayConfig.getMchId());
            closeRequest.setOutTradeNo(bill.getBillNo());
            jsapiServiceExtension.closeOrder(closeRequest);
            log.info("关闭订单成功，账单ID: {}", billId);
        } catch (Exception e) {
            log.error("关闭订单失败", e);
            throw new BusinessException("关闭订单失败: " + e.getMessage());
        }
    }
}
