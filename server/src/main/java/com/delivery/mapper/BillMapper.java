package com.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.entity.Bill;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账单Mapper
 */
@Mapper
public interface BillMapper extends BaseMapper<Bill> {
}
