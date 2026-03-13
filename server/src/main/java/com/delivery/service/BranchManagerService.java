package com.delivery.service;

import com.delivery.common.PageResult;
import com.delivery.dto.request.DriverCreateRequest;
import com.delivery.dto.request.ProductCreateRequest;
import com.delivery.dto.request.ShopCreateRequest;
import com.delivery.dto.response.DriverResponse;
import com.delivery.dto.response.ProductResponse;
import com.delivery.dto.response.ShopResponse;

/**
 * 分管理服务接口
 */
public interface BranchManagerService {

    // ==================== 店铺管理 ====================

    /**
     * 创建店铺
     */
    void createShop(Long branchManagerId, ShopCreateRequest request);

    /**
     * 获取店铺列表
     */
    PageResult<ShopResponse> getShopList(Long branchManagerId, Integer page, Integer size);

    /**
     * 获取店铺详情
     */
    ShopResponse getShopDetail(Long branchManagerId, Long shopId);

    /**
     * 更新店铺
     */
    void updateShop(Long branchManagerId, Long shopId, ShopCreateRequest request);

    /**
     * 删除店铺
     */
    void deleteShop(Long branchManagerId, Long shopId);

    // ==================== 司机管理 ====================

    /**
     * 创建司机
     */
    void createDriver(Long branchManagerId, DriverCreateRequest request);

    /**
     * 获取司机列表
     */
    PageResult<DriverResponse> getDriverList(Long branchManagerId, Integer page, Integer size);

    /**
     * 获取司机详情
     */
    DriverResponse getDriverDetail(Long branchManagerId, Long driverId);

    /**
     * 更新司机
     */
    void updateDriver(Long branchManagerId, Long driverId, DriverCreateRequest request);

    /**
     * 删除司机
     */
    void deleteDriver(Long branchManagerId, Long driverId);

    // ==================== 商品管理 ====================

    /**
     * 创建商品
     */
    void createProduct(Long branchManagerId, ProductCreateRequest request);

    /**
     * 获取商品列表
     */
    PageResult<ProductResponse> getProductList(Long branchManagerId, Integer page, Integer size, String category);

    /**
     * 获取商品详情
     */
    ProductResponse getProductDetail(Long branchManagerId, Long productId);

    /**
     * 更新商品
     */
    void updateProduct(Long branchManagerId, Long productId, ProductCreateRequest request);

    /**
     * 删除商品
     */
    void deleteProduct(Long branchManagerId, Long productId);
}
