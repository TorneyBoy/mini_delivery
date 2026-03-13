package com.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.entity.PickingList;
import org.apache.ibatis.annotations.Mapper;

/**
 * 拣货清单Mapper
 */
@Mapper
public interface PickingListMapper extends BaseMapper<PickingList> {
}
