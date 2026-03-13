package com.delivery.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {

    ADMIN("ADMIN", "总管理"),
    BRANCH_MANAGER("BRANCH_MANAGER", "分管理"),
    SHOP("SHOP", "店铺"),
    DRIVER("DRIVER", "司机");

    private final String code;
    private final String desc;

    UserRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role code: " + code);
    }
}
