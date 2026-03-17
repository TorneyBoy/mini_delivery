package com.delivery.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建店铺请求DTO
 */
@Data
@Schema(description = "创建店铺请求")
public class ShopCreateRequest {

    @NotBlank(message = "店铺名称不能为空")
    @Schema(description = "店铺名称", example = "水果店A")
    private String name;

    @NotBlank(message = "地址不能为空")
    @Schema(description = "地址", example = "北京市朝阳区xxx街道")
    private String address;

    @Schema(description = "纬度", example = "39.908823")
    private Double latitude;

    @Schema(description = "经度", example = "116.397470")
    private Double longitude;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800000002")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "是否显示单价 1-显示 0-不显示", example = "1")
    private Integer showPrice = 1;

    @Schema(description = "价格类型 1-老用户价 2-新用户价", example = "1")
    private Integer priceType = 1;

    @Schema(description = "状态 1-正常 0-禁用", example = "1")
    private Integer status = 1;
}
