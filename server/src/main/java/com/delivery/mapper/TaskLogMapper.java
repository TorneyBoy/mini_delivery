package com.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.entity.TaskLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务日志Mapper
 */
@Mapper
public interface TaskLogMapper extends BaseMapper<TaskLog> {
}
