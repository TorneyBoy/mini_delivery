package com.delivery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 自动任务日志实体
 */
@Data
@TableName("task_log")
public class TaskLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 执行时间
     */
    private LocalDateTime executedAt;

    /**
     * 状态 0-失败 1-成功
     */
    private Integer status;

    /**
     * 执行详情
     */
    private String detail;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
