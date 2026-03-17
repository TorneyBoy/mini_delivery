package com.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.entity.ProductImageRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 司机上传商品图片请求Mapper
 */
@Mapper
public interface ProductImageRequestMapper extends BaseMapper<ProductImageRequest> {
}
