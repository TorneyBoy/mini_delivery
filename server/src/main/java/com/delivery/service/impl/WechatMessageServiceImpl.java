package com.delivery.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import com.delivery.entity.Driver;
import com.delivery.entity.Shop;
import com.delivery.mapper.DriverMapper;
import com.delivery.mapper.ShopMapper;
import com.delivery.service.WechatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信订阅消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatMessageServiceImpl implements WechatMessageService {

    private final WxMaService wxMaService;
    private final ShopMapper shopMapper;
    private final DriverMapper driverMapper;

    @Value("${wechat.miniapp.appid:}")
    private String appid;

    // 订阅消息模板ID（需要在微信公众平台申请）
    // 账单生成通知模板
    @Value("${wechat.message.template.bill-created:}")
    private String billCreatedTemplateId;

    // 订单分配通知模板
    @Value("${wechat.message.template.order-assigned:}")
    private String orderAssignedTemplateId;

    // 订单状态变更通知模板
    @Value("${wechat.message.template.order-status:}")
    private String orderStatusTemplateId;

    // 送货完成通知模板
    @Value("${wechat.message.template.delivery-complete:}")
    private String deliveryCompleteTemplateId;

    // 支付成功通知模板
    @Value("${wechat.message.template.payment-success:}")
    private String paymentSuccessTemplateId;

    @Override
    public void sendBillCreatedNotice(Long shopId, Long billId, String amount) {
        try {
            Shop shop = shopMapper.selectById(shopId);
            if (shop == null || shop.getOpenid() == null) {
                log.warn("店铺不存在或未绑定openid，跳过发送通知，shopId: {}", shopId);
                return;
            }

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

            WxMaSubscribeMessage message = WxMaSubscribeMessage.builder()
                    .toUser(shop.getOpenid())
                    .templateId(billCreatedTemplateId)
                    .page("pages/shop/bill-detail/bill-detail?id=" + billId)
                    .build();

            // 添加模板数据（根据实际模板配置）
            message.addData(new WxMaSubscribeMessage.MsgData("thing1", "货款账单"));
            message.addData(new WxMaSubscribeMessage.MsgData("amount2", amount + "元"));
            message.addData(new WxMaSubscribeMessage.MsgData("date3", today));
            message.addData(new WxMaSubscribeMessage.MsgData("thing4", "请及时查看并支付"));

            wxMaService.getMsgService().sendSubscribeMsg(message);
            log.info("发送账单生成通知成功，shopId: {}, billId: {}", shopId, billId);

        } catch (Exception e) {
            log.error("发送账单生成通知失败，shopId: {}, billId: {}", shopId, billId, e);
        }
    }

    @Override
    public void sendOrderAssignedNotice(Long driverId, Long orderId, String shopName) {
        try {
            Driver driver = driverMapper.selectById(driverId);
            if (driver == null || driver.getOpenid() == null) {
                log.warn("司机不存在或未绑定openid，跳过发送通知，driverId: {}", driverId);
                return;
            }

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

            WxMaSubscribeMessage message = WxMaSubscribeMessage.builder()
                    .toUser(driver.getOpenid())
                    .templateId(orderAssignedTemplateId)
                    .page("pages/driver/pending/pending")
                    .build();

            message.addData(new WxMaSubscribeMessage.MsgData("thing1", shopName));
            message.addData(new WxMaSubscribeMessage.MsgData("date2", today));
            message.addData(new WxMaSubscribeMessage.MsgData("thing3", "您有新的配送订单"));

            wxMaService.getMsgService().sendSubscribeMsg(message);
            log.info("发送订单分配通知成功，driverId: {}, orderId: {}", driverId, orderId);

        } catch (Exception e) {
            log.error("发送订单分配通知失败，driverId: {}, orderId: {}", driverId, orderId, e);
        }
    }

    @Override
    public void sendOrderStatusNotice(Long shopId, Long orderId, String status) {
        try {
            Shop shop = shopMapper.selectById(shopId);
            if (shop == null || shop.getOpenid() == null) {
                log.warn("店铺不存在或未绑定openid，跳过发送通知，shopId: {}", shopId);
                return;
            }

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

            WxMaSubscribeMessage message = WxMaSubscribeMessage.builder()
                    .toUser(shop.getOpenid())
                    .templateId(orderStatusTemplateId)
                    .page("pages/shop/order-detail/order-detail?id=" + orderId)
                    .build();

            message.addData(new WxMaSubscribeMessage.MsgData("thing1", "订单状态更新"));
            message.addData(new WxMaSubscribeMessage.MsgData("phrase2", status));
            message.addData(new WxMaSubscribeMessage.MsgData("date3", today));

            wxMaService.getMsgService().sendSubscribeMsg(message);
            log.info("发送订单状态通知成功，shopId: {}, orderId: {}", shopId, orderId);

        } catch (Exception e) {
            log.error("发送订单状态通知失败，shopId: {}, orderId: {}", shopId, orderId, e);
        }
    }

    @Override
    public void sendDeliveryCompleteNotice(Long shopId, Long orderId) {
        try {
            Shop shop = shopMapper.selectById(shopId);
            if (shop == null || shop.getOpenid() == null) {
                log.warn("店铺不存在或未绑定openid，跳过发送通知，shopId: {}", shopId);
                return;
            }

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

            WxMaSubscribeMessage message = WxMaSubscribeMessage.builder()
                    .toUser(shop.getOpenid())
                    .templateId(deliveryCompleteTemplateId)
                    .page("pages/shop/order-detail/order-detail?id=" + orderId)
                    .build();

            message.addData(new WxMaSubscribeMessage.MsgData("thing1", "订单已送达"));
            message.addData(new WxMaSubscribeMessage.MsgData("date2", today));
            message.addData(new WxMaSubscribeMessage.MsgData("thing3", "请确认收货"));

            wxMaService.getMsgService().sendSubscribeMsg(message);
            log.info("发送送货完成通知成功，shopId: {}, orderId: {}", shopId, orderId);

        } catch (Exception e) {
            log.error("发送送货完成通知失败，shopId: {}, orderId: {}", shopId, orderId, e);
        }
    }

    @Override
    public void sendPaymentSuccessNotice(Long shopId, Long billId, String amount) {
        try {
            Shop shop = shopMapper.selectById(shopId);
            if (shop == null || shop.getOpenid() == null) {
                log.warn("店铺不存在或未绑定openid，跳过发送通知，shopId: {}", shopId);
                return;
            }

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

            WxMaSubscribeMessage message = WxMaSubscribeMessage.builder()
                    .toUser(shop.getOpenid())
                    .templateId(paymentSuccessTemplateId)
                    .page("pages/shop/bill-detail/bill-detail?id=" + billId)
                    .build();

            message.addData(new WxMaSubscribeMessage.MsgData("thing1", "账单支付成功"));
            message.addData(new WxMaSubscribeMessage.MsgData("amount2", amount + "元"));
            message.addData(new WxMaSubscribeMessage.MsgData("date3", today));

            wxMaService.getMsgService().sendSubscribeMsg(message);
            log.info("发送支付成功通知成功，shopId: {}, billId: {}", shopId, billId);

        } catch (Exception e) {
            log.error("发送支付成功通知失败，shopId: {}, billId: {}", shopId, billId, e);
        }
    }
}
