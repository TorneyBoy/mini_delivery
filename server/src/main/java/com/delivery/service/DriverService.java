package com.delivery.service;

import com.delivery.dto.response.DeliveryListResponse;
import com.delivery.dto.response.OrderResponse;
import com.delivery.dto.response.PickingListResponse;

import java.util.List;

/**
 * 司机服务接口
 */
public interface DriverService {

    /**
     * 获取待分配订单列表
     */
    List<OrderResponse> getPendingOrders(Long driverId);

    /**
     * 选择订单
     */
    void selectOrders(Long driverId, List<Long> orderIds);

    /**
     * 取消选择订单
     */
    void deselectOrders(Long driverId, List<Long> orderIds);

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
    void completeDelivery(Long driverId, Long deliveryListId);
}
