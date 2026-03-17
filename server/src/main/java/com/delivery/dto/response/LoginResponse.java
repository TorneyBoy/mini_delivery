package com.delivery.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "角色描述")
    private String roleDesc;

    @Schema(description = "品牌名称（仅分管理有）")
    private String brandName;

    @Schema(description = "用户名称")
    private String name;

    public static LoginResponse of(String token, Long userId, String phone, String role, String roleDesc) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(userId);
        response.setPhone(phone);
        response.setRole(role);
        response.setRoleDesc(roleDesc);
        return response;
    }

    public static LoginResponse of(String token, Long userId, String phone, String role, String roleDesc,
            String brandName, String name) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(userId);
        response.setPhone(phone);
        response.setRole(role);
        response.setRoleDesc(roleDesc);
        response.setBrandName(brandName);
        response.setName(name);
        return response;
    }
}
