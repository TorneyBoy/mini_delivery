package com.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 司机上传商品图片请求实体
 */
@Data
@TableName("product_image_request")
public class ProductImageRequest implements Serializable {

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
     * 分管理ID
     */
    private Long branchManagerId;

    /**
     * 商品ID（可选，如果是对现有商品上传图片）
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 类别
     */
    private String category;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 描述说明
     */
    private String description;

    /**
     * 状态 0-待审核 1-已通过 2-已拒绝
     */
    private Integer status;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 审核人ID
     */
    private Long reviewedBy;

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
