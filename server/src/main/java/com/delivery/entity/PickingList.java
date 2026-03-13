package com.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拣货清单实体
 */
@Data
@TableName("picking_list")
public class PickingList implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 司机ID
     */
    private Long driverId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称（冗余存储）
     */
    private String productName;

    /**
     * 总数量
     */
    private BigDecimal totalQuantity;

    /**
     * 已拣数量
     */
    private BigDecimal pickedQuantity;

    /**
     * 状态 0-待拣货 1-已完成
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
