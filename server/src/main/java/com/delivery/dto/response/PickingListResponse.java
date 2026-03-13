package com.delivery.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 拣货清单响应DTO
 */
@Data
@Schema(description = "拣货清单响应")
public class PickingListResponse {

    @Schema(description = "拣货清单列表")
    private List<PickingItem> items;

    @Schema(description = "是否全部完成")
    private Boolean allCompleted;

    /**
     * 拣货项
     */
    @Data
    @Schema(description = "拣货项")
    public static class PickingItem {

        @Schema(description = "ID")
        private Long id;

        @Schema(description = "商品ID")
        private Long productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "总数量")
        private BigDecimal totalQuantity;

        @Schema(description = "已拣数量")
        private BigDecimal pickedQuantity;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "状态 0-待拣货 1-已完成")
        private Integer status;
    }
}
