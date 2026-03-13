package com.delivery.common;

import lombok.Getter;

/**
 * 响应状态码
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // 参数错误 4xx
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),

    // 业务错误 5xx
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 用户相关 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    ACCOUNT_DISABLED(1003, "账号已禁用"),
    PHONE_EXISTS(1004, "手机号已存在"),
    LOGIN_EXPIRED(1005, "登录已过期"),
    DATA_NOT_FOUND(1006, "数据不存在"),

    // 订单相关 2xxx
    ORDER_NOT_FOUND(2001, "订单不存在"),
    ORDER_STATUS_ERROR(2002, "订单状态错误"),
    ORDER_ALREADY_ASSIGNED(2003, "订单已分配"),
    ORDER_CANNOT_MODIFY(2004, "订单不可修改"),

    // 商品相关 3xxx
    PRODUCT_NOT_FOUND(3001, "商品不存在"),
    PRODUCT_OFF_SHELF(3002, "商品已下架"),

    // 账单相关 4xxx
    BILL_NOT_FOUND(4001, "账单不存在"),
    BILL_ALREADY_PAID(4002, "账单已支付"),
    PAYMENT_FAILED(4003, "支付失败"),
    BILL_STATUS_ERROR(4004, "账单状态错误"),

    // 权限相关 5xxx
    NO_PERMISSION(5001, "无权限操作"),
    ROLE_ERROR(5002, "角色错误");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
