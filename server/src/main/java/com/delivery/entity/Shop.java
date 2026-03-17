package com.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺实体
 */
@Data
@TableName("shop")
public class Shop implements Serializable {

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
     * 店铺名称
     */
    private String name;

    /**
     * 地址
     */
    private String address;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码
     */
    private String password;

    /**
     * 是否显示单价 1-显示 0-不显示
     */
    private Integer showPrice;

    /**
     * 价格类型 1-老用户价 2-新用户价
     */
    private Integer priceType;

    /**
     * 状态 1-正常 0-禁用
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
     * 微信openid（用于消息推送）
     */
    private String openid;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
