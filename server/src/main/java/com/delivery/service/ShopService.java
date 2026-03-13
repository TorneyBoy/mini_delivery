package com.delivery.service;

import com.delivery.common.PageResult;
import com.delivery.dto.request.OrderCreateRequest;
import com.delivery.dto.response.BillResponse;
import com.delivery.dto.response.OrderResponse;
import com.delivery.dto.response.ProductResponse;
import com.delivery.dto.response.ShopResponse;

import java.util.List;

/**
 * 店铺服务接口
 */
public interface ShopService {

    /**
     * 获取店铺信息
     */
    ShopResponse getShopInfo(Long shopId);

    /**
     * 获取商品列表（根据店铺的价格体系）
     */
    PageResult<ProductResponse> getProductList(Long shopId, Integer page, Integer size, String category);

    /**
     * 获取商品详情
     */
    ProductResponse getProductDetail(Long shopId, Long productId);

    /**
     * 创建订单
     */
    OrderResponse createOrder(Long shopId, OrderCreateRequest request);

    /**
     * 获取订单列表
     */
    PageResult<OrderResponse> getOrderList(Long shopId, Integer page, Integer size, Integer status);

    /**
     * 获取订单详情
     */
    OrderResponse getOrderDetail(Long shopId, Long orderId);

    /**
     * 修改订单（仅限凌晨2点前的当日订单）
     */
    OrderResponse updateOrder(Long shopId, Long orderId, OrderCreateRequest request);

    /**
     * 确认收货
     */
    void confirmReceive(Long shopId, Long orderId);

    /**
     * 获取账单列表
     */
    PageResult<BillResponse> getBillList(Long shopId, Integer page, Integer size);

    /**
     * 获取账单详情
     */
    BillResponse getBillDetail(Long shopId, Long billId);

    /**
     * 获取商品分类列表
     */
    List<String> getCategoryList(Long shopId);

    /**
     * 支付账单
     */
    void payBill(Long shopId, Long billId);
}
