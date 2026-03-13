package com.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
@TableName("product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分管理ID
     */
    private Long branchManagerId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 类别
     */
    private String category;

    /**
     * 老用户价格
     */
    private BigDecimal oldPrice;

    /**
     * 新用户价格
     */
    private BigDecimal newPrice;

    /**
     * 单位（斤/箱）
     */
    private String unit;

    /**
     * 商品说明
     */
    private String description;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 状态 1-正常 0-下架
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

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
