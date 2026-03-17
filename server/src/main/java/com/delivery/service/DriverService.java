package com.delivery.service;

import com.delivery.dto.request.OrderItemModifyRequest;
import com.delivery.dto.request.ProductImageRequestDto;
import com.delivery.dto.response.DeliveryHistoryResponse;
import com.delivery.dto.response.DeliveryListResponse;
import com.delivery.dto.response.DriverResponse;
import com.delivery.dto.response.DriverStatisticsResponse;
import com.delivery.dto.response.OrderDetailResponse;
import com.delivery.dto.response.OrderResponse;
import com.delivery.dto.response.PickingListResponse;
import com.delivery.dto.response.ProductImageRequestResponse;
import com.delivery.dto.response.ProductResponse;

import java.util.List;

/**
 * 司机服务接口
 */
public interface DriverService {

    /**
     * 获取司机信息
     */
    DriverResponse getDriverInfo(Long driverId);

    /**
     * 获取待分配订单列表
     */
    List<OrderResponse> getPendingOrders(Long driverId);

    /**
     * 获取已选择的订单列表（待拣货）
     */
    List<OrderResponse> getSelectedOrders(Long driverId);

    /**
     * 选择订单
     */
    void selectOrders(Long driverId, List<Long> orderIds);

    /**
     * 取消选择订单
     */
    void deselectOrders(Long driverId, List<Long> orderIds);

    /**
     * 删除已选择的订单
     */
    void removeSelectedOrder(Long driverId, Long orderId);

    /**
     * 获取拣货清单
     */
    PickingListResponse getPickingList(Long driverId);

    /**
     * 完成拣货
     */
    void completePicking(Long driverId, Long pickingItemId);

    /**
     * 获取送货清单
     */
    List<DeliveryListResponse> getDeliveryList(Long driverId);

    /**
     * 完成送货
     */
    void completeDelivery(Long driverId, Long deliveryListId, String deliveryPhoto);

    /**
     * 获取司机统计数据
     */
    DriverStatisticsResponse getStatistics(Long driverId);

    /**
     * 获取司机历史送货记录
     */
    List<DeliveryHistoryResponse> getDeliveryHistory(Long driverId);

    /**
     * 提交商品图片请求
     */
    void submitProductImageRequest(Long driverId, ProductImageRequestDto request);

    /**
     * 获取司机的商品图片请求列表
     */
    List<ProductImageRequestResponse> getMyProductImageRequests(Long driverId);

    /**
     * 获取已选订单的详细信息（用于拣货修改）
     */
    List<OrderDetailResponse> getSelectedOrdersDetail(Long driverId);

    /**
     * 修改订单商品
     */
    void modifyOrderItems(Long driverId, OrderItemModifyRequest request);

    /**
     * 获取商品库列表（用于拣货时添加商品）
     */
    List<ProductResponse> getAvailableProducts(Long driverId);
}
