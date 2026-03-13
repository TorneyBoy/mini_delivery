package com.delivery.enums;

import lombok.Getter;

/**
 * 账单状态枚举
 */
@Getter
public enum BillStatus {

    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付");

    private final Integer code;
    private final String desc;

    BillStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static BillStatus fromCode(Integer code) {
        for (BillStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}
