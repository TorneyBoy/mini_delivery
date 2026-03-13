package com.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.entity.DeliveryList;
import org.apache.ibatis.annotations.Mapper;

/**
 * 送货清单Mapper
 */
@Mapper
public interface DeliveryListMapper extends BaseMapper<DeliveryList> {
}
