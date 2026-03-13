package com.delivery.enums;

import lombok.Getter;

/**
 * 价格类型枚举
 */
@Getter
public enum PriceType {

    OLD_USER(1, "老用户价"),
    NEW_USER(2, "新用户价");

    private final Integer code;
    private final String desc;

    PriceType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PriceType fromCode(Integer code) {
        for (PriceType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown price type code: " + code);
    }
}
