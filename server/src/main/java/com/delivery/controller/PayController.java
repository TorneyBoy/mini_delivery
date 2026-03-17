package com.delivery.controller;

import com.delivery.dto.response.PayResponse;
import com.delivery.service.WechatPayService;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付控制器
 * 只有在配置了微信支付相关参数时才启用
 */
@Slf4j
@Tag(name = "支付", description = "微信支付相关接口")
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
@ConditionalOnBean(JsapiServiceExtension.class)
public class PayController {

    private final WechatPayService wechatPayService;

    /**
     * 创建支付订单
     * 店铺端调用，获取微信支付参数
     */
    @Operation(summary = "创建支付订单", description = "创建微信支付订单，返回前端调起支付所需参数")
    @PostMapping("/create/{billId}")
    public ResponseEntity<PayResponse> createPayOrder(
            @PathVariable Long billId,
            @RequestParam String openid) {
        PayResponse response = wechatPayService.createJsapiOrder(billId, openid);
        return ResponseEntity.ok(response);
    }

    /**
     * 微信支付回调通知
     * 微信服务器调用，通知支付结果
     */
    @Operation(summary = "支付回调", description = "微信支付结果回调通知")
    @PostMapping("/notify")
    public ResponseEntity<String> payNotify(HttpServletRequest request) {
        log.info("收到微信支付回调通知");

        try {
            // 读取请求体
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String notifyData = sb.toString();

            // 获取请求头信息
            String serial = request.getHeader("Wechatpay-Serial");
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");

            log.info("支付回调 - Serial: {}, Timestamp: {}", serial, timestamp);

            // 处理回调
            boolean result = wechatPayService.handlePayNotify(notifyData);

            if (result) {
                // 返回成功响应
                return ResponseEntity.ok("{\"code\":\"SUCCESS\",\"message\":\"成功\"}");
            } else {
                return ResponseEntity.status(500)
                        .body("{\"code\":\"FAIL\",\"message\":\"处理失败\"}");
            }

        } catch (Exception e) {
            log.error("处理支付回调异常", e);
            return ResponseEntity.status(500)
                    .body("{\"code\":\"FAIL\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 查询支付状态
     */
    @Operation(summary = "查询支付状态", description = "查询账单支付状态")
    @GetMapping("/status/{billId}")
    public ResponseEntity<Map<String, Object>> queryPayStatus(@PathVariable Long billId) {
        boolean paid = wechatPayService.queryPayStatus(billId);
        Map<String, Object> result = new HashMap<>();
        result.put("billId", billId);
        result.put("paid", paid);
        return ResponseEntity.ok(result);
    }

    /**
     * 关闭订单
     */
    @Operation(summary = "关闭订单", description = "关闭未支付的订单")
    @PostMapping("/close/{billId}")
    public ResponseEntity<Map<String, Object>> closeOrder(@PathVariable Long billId) {
        wechatPayService.closeOrder(billId);
        Map<String, Object> result = new HashMap<>();
        result.put("billId", billId);
        result.put("message", "订单已关闭");
        return ResponseEntity.ok(result);
    }
}
