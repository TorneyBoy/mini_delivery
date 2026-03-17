package com.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 送货清单实体
 */
@Data
@TableName("delivery_list")
public class DeliveryList implements Serializable {

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
     * 店铺ID
     */
    private Long shopId;

    /**
     * 状态 0-待送货 1-已完成
     */
    private Integer status;

    /**
     * 送达照片URL
     */
    private String deliveryPhoto;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

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
