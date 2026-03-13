package com.delivery.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺响应DTO
 */
@Data
@Schema(description = "店铺响应")
public class ShopResponse {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "店铺名称")
    private String name;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "是否显示单价 1-显示 0-不显示")
    private Integer showPrice;

    @Schema(description = "价格类型 1-老用户价 2-新用户价")
    private Integer priceType;

    @Schema(description = "状态 1-正常 0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "订单数量")
    private Integer orderCount;

    @Schema(description = "累计金额")
    private String totalAmount;
}
