package com.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("`order`")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 司机ID
     */
    private Long driverId;

    /**
     * 状态 0-待支付 1-待分配 2-待拣货 3-待送货 4-已完成 5-已取消
     */
    private Integer status;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 收货日期
     */
    private LocalDate deliveryDate;

    /**
     * 收货时间
     */
    private LocalDateTime receivedAt;

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

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
