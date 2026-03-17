package com.delivery.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 分管理审核商品图片请求DTO
 */
@Data
public class ProductImageReviewDto {

    /**
     * 请求ID
     */
    @NotNull(message = "请求ID不能为空")
    private Long requestId;

    /**
     * 是否通过
     */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    /**
     * 拒绝原因（拒绝时必填）
     */
    private String rejectReason;
}
