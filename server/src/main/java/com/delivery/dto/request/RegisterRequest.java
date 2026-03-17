package com.delivery.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册请求DTO
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @NotBlank(message = "角色不能为空")
    @Schema(description = "角色 SHOP-店铺 DRIVER-司机", example = "SHOP")
    private String role;

    @NotNull(message = "分管理ID不能为空")
    @Schema(description = "分管理ID", example = "1")
    private Long branchManagerId;

    @NotBlank(message = "名称不能为空")
    @Schema(description = "店铺名称/司机姓名", example = "水果店A")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800000002")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "地址（店铺必填）", example = "北京市朝阳区xxx街道")
    private String address;

    @Schema(description = "纬度", example = "39.908823")
    private Double latitude;

    @Schema(description = "经度", example = "116.397470")
    private Double longitude;
}
