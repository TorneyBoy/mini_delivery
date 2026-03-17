package com.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账单实体
 */
@Data
@TableName("bill")
public class Bill implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 账单号
     */
    private String billNo;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 账单日期
     */
    private LocalDate billDate;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 状态 0-待支付 1-已支付
     */
    private Integer status;

    /**
     * 发送状态 0-未发送 1-已发送
     */
    private Integer sendStatus;

    /**
     * 发送时间
     */
    private LocalDateTime sentAt;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 微信交易号
     */
    private String wechatTransactionId;

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
