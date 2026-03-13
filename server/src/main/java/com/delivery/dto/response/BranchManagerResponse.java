package com.delivery.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分管理响应DTO
 */
@Data
@Schema(description = "分管理响应")
public class BranchManagerResponse {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "品牌名称")
    private String brandName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态 1-正常 0-禁用")
    private Integer status;

    @Schema(description = "店铺数量")
    private Integer shopCount;

    @Schema(description = "司机数量")
    private Integer driverCount;

    @Schema(description = "商品数量")
    private Integer productCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
