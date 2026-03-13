package com.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.entity.BillOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账单订单关联Mapper
 */
@Mapper
public interface BillOrderMapper extends BaseMapper<BillOrder> {
}
