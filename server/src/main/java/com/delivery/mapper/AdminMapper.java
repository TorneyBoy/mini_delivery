package com.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 总管理Mapper
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
