package com.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.entity.DeliveryOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 送货清单订单关联Mapper
 */
@Mapper
public interface DeliveryOrderMapper extends BaseMapper<DeliveryOrder> {
}
