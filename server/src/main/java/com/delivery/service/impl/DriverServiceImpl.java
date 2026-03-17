package com.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delivery.common.ResultCode;
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
import com.delivery.entity.*;
import com.delivery.enums.OrderStatus;
import com.delivery.exception.BusinessException;
import com.delivery.mapper.*;
import com.delivery.service.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 司机服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverMapper driverMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ShopMapper shopMapper;
    private final ProductMapper productMapper;
    private final PickingListMapper pickingListMapper;
    private final DeliveryListMapper deliveryListMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final ProductImageRequestMapper productImageRequestMapper;

    @Override
    public DriverResponse getDriverInfo(Long driverId) {
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        DriverResponse response = new DriverResponse();
        response.setId(driver.getId());
        response.setName(driver.getName());
        response.setPhone(driver.getPhone());
        response.setStatus(driver.getStatus());
        response.setCreatedAt(driver.getCreatedAt());
        return response;
    }

    @Override
    public List<OrderResponse> getPendingOrders(Long driverId) {
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 根据当前时间确定显示哪一天的订单
        // 凌晨2点之前显示当天的订单，凌晨2点之后显示第二天的订单
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();

        java.time.LocalDate targetDate;
        if (currentHour < 2) {
            // 凌晨0点-2点：显示当天的订单
            targetDate = now.toLocalDate();
        } else {
            // 凌晨2点之后：显示第二天的订单
            targetDate = now.toLocalDate().plusDays(1);
        }

        // 获取该司机所属分管理下所有待分配的订单，并按日期过滤
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.PENDING_ASSIGNMENT.getCode())
                        .eq(Order::getDeliveryDate, targetDate)
                        .exists("SELECT 1 FROM shop s WHERE s.id = `order`.shop_id AND s.branch_manager_id = {0}",
                                driver.getBranchManagerId())
                        .orderByAsc(Order::getDeliveryDate));

        return orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderItemMapper.selectList(
                            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
                    Shop shop = shopMapper.selectById(order.getShopId());
                    OrderResponse response = convertToOrderResponse(order, items, null);
                    if (shop != null) {
                        response.setShopName(shop.getName());
                        response.setShopAddress(shop.getAddress());
                        response.setShopPhone(shop.getPhone());
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void selectOrders(Long driverId, List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单ID列表不能为空");
        }

        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        for (Long orderId : orderIds) {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
            }

            if (!order.getStatus().equals(OrderStatus.PENDING_ASSIGNMENT.getCode())) {
                throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单已分配或状态不正确");
            }

            // 分配订单给司机
            order.setDriverId(driverId);
            order.setStatus(OrderStatus.PENDING_PICKUP.getCode());
            orderMapper.updateById(order);

            // 更新或创建拣货清单
            updatePickingList(driverId, order);
        }
    }

    @Override
    public List<OrderResponse> getSelectedOrders(Long driverId) {
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 获取该司机已选择待拣货的订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getDriverId, driverId)
                        .eq(Order::getStatus, OrderStatus.PENDING_PICKUP.getCode())
                        .orderByAsc(Order::getDeliveryDate));

        return orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderItemMapper.selectList(
                            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
                    Shop shop = shopMapper.selectById(order.getShopId());
                    OrderResponse response = convertToOrderResponse(order, items, null);
                    if (shop != null) {
                        response.setShopName(shop.getName());
                        response.setShopAddress(shop.getAddress());
                        response.setShopPhone(shop.getPhone());
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deselectOrders(Long driverId, List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单ID列表不能为空");
        }

        for (Long orderId : orderIds) {
            Order order = orderMapper.selectById(orderId);
            if (order == null || !driverId.equals(order.getDriverId())) {
                continue;
            }

            // 取消分配
            order.setDriverId(null);
            order.setStatus(OrderStatus.PENDING_ASSIGNMENT.getCode());
            orderMapper.updateById(order);

            // 从拣货清单中移除
            removeFromPickingList(driverId, orderId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSelectedOrder(Long driverId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        if (!driverId.equals(order.getDriverId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
        }

        if (!order.getStatus().equals(OrderStatus.PENDING_PICKUP.getCode())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态不正确，无法删除");
        }

        // 取消分配
        order.setDriverId(null);
        order.setStatus(OrderStatus.PENDING_ASSIGNMENT.getCode());
        orderMapper.updateById(order);

        // 从拣货清单中移除
        removeFromPickingList(driverId, orderId);
    }

    @Override
    public PickingListResponse getPickingList(Long driverId) {
        List<PickingList> pickingLists = pickingListMapper.selectList(
                new LambdaQueryWrapper<PickingList>()
                        .eq(PickingList::getDriverId, driverId)
                        .orderByAsc(PickingList::getStatus));

        PickingListResponse response = new PickingListResponse();
        List<PickingListResponse.PickingItem> items = pickingLists.stream()
                .map(this::convertToPickingItem)
                .collect(Collectors.toList());

        response.setItems(items);
        response.setAllCompleted(items.stream().allMatch(item -> item.getStatus() == 1));

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completePicking(Long driverId, Long pickingItemId) {
        PickingList pickingItem = pickingListMapper.selectOne(
                new LambdaQueryWrapper<PickingList>()
                        .eq(PickingList::getId, pickingItemId)
                        .eq(PickingList::getDriverId, driverId));

        if (pickingItem == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "拣货项不存在");
        }

        pickingItem.setStatus(1);
        pickingItem.setPickedQuantity(pickingItem.getTotalQuantity());
        pickingListMapper.updateById(pickingItem);

        // 检查是否所有拣货都完成
        checkAndCreateDeliveryLists(driverId);
    }

    @Override
    public List<DeliveryListResponse> getDeliveryList(Long driverId) {
        List<DeliveryList> deliveryLists = deliveryListMapper.selectList(
                new LambdaQueryWrapper<DeliveryList>()
                        .eq(DeliveryList::getDriverId, driverId)
                        .eq(DeliveryList::getStatus, 0)
                        .orderByAsc(DeliveryList::getCreatedAt));

        return deliveryLists.stream()
                .map(this::convertToDeliveryListResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeDelivery(Long driverId, Long deliveryListId, String deliveryPhoto) {
        DeliveryList deliveryList = deliveryListMapper.selectOne(
                new LambdaQueryWrapper<DeliveryList>()
                        .eq(DeliveryList::getId, deliveryListId)
                        .eq(DeliveryList::getDriverId, driverId));

        if (deliveryList == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "送货清单不存在");
        }

        deliveryList.setStatus(1);
        deliveryList.setDeliveryPhoto(deliveryPhoto);
        deliveryList.setCompletedAt(LocalDateTime.now());
        deliveryListMapper.updateById(deliveryList);

        // 更新订单状态为已完成
        List<DeliveryOrder> deliveryOrders = deliveryOrderMapper.selectList(
                new LambdaQueryWrapper<DeliveryOrder>().eq(DeliveryOrder::getDeliveryListId, deliveryListId));

        for (DeliveryOrder deliveryOrder : deliveryOrders) {
            Order order = orderMapper.selectById(deliveryOrder.getOrderId());
            if (order != null) {
                order.setStatus(OrderStatus.COMPLETED.getCode());
                order.setReceivedAt(LocalDateTime.now());
                orderMapper.updateById(order);
            }
        }
    }

    // ==================== 辅助方法 ====================

    private void updatePickingList(Long driverId, Order order) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        for (OrderItem orderItem : orderItems) {
            // 查找是否已有该商品的拣货项
            PickingList existingItem = pickingListMapper.selectOne(
                    new LambdaQueryWrapper<PickingList>()
                            .eq(PickingList::getDriverId, driverId)
                            .eq(PickingList::getProductId, orderItem.getProductId())
                            .eq(PickingList::getStatus, 0));

            if (existingItem != null) {
                // 累加数量
                existingItem.setTotalQuantity(existingItem.getTotalQuantity().add(orderItem.getQuantity()));
                pickingListMapper.updateById(existingItem);
            } else {
                // 创建新的拣货项
                Product product = productMapper.selectById(orderItem.getProductId());
                PickingList pickingItem = new PickingList();
                pickingItem.setDriverId(driverId);
                pickingItem.setProductId(orderItem.getProductId());
                pickingItem.setProductName(orderItem.getProductName());
                pickingItem.setTotalQuantity(orderItem.getQuantity());
                pickingItem.setPickedQuantity(BigDecimal.ZERO);
                pickingItem.setStatus(0);
                pickingListMapper.insert(pickingItem);
            }
        }
    }

    private void removeFromPickingList(Long driverId, Long orderId) {
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));

        for (OrderItem orderItem : orderItems) {
            PickingList existingItem = pickingListMapper.selectOne(
                    new LambdaQueryWrapper<PickingList>()
                            .eq(PickingList::getDriverId, driverId)
                            .eq(PickingList::getProductId, orderItem.getProductId())
                            .eq(PickingList::getStatus, 0));

            if (existingItem != null) {
                BigDecimal newQuantity = existingItem.getTotalQuantity().subtract(orderItem.getQuantity());
                if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    pickingListMapper.deleteById(existingItem.getId());
                } else {
                    existingItem.setTotalQuantity(newQuantity);
                    pickingListMapper.updateById(existingItem);
                }
            }
        }
    }

    private void checkAndCreateDeliveryLists(Long driverId) {
        // 检查是否所有拣货都完成
        Long pendingCount = pickingListMapper.selectCount(
                new LambdaQueryWrapper<PickingList>()
                        .eq(PickingList::getDriverId, driverId)
                        .eq(PickingList::getStatus, 0));

        if (pendingCount > 0) {
            return;
        }

        // 获取司机已拣货的所有订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getDriverId, driverId)
                        .eq(Order::getStatus, OrderStatus.PENDING_PICKUP.getCode()));

        if (orders.isEmpty()) {
            return;
        }

        // 按店铺分组创建送货清单
        Map<Long, List<Order>> ordersByShop = orders.stream()
                .collect(Collectors.groupingBy(Order::getShopId));

        for (Map.Entry<Long, List<Order>> entry : ordersByShop.entrySet()) {
            Long shopId = entry.getKey();
            List<Order> shopOrders = entry.getValue();

            // 创建送货清单
            DeliveryList deliveryList = new DeliveryList();
            deliveryList.setDriverId(driverId);
            deliveryList.setShopId(shopId);
            deliveryList.setStatus(0);
            deliveryListMapper.insert(deliveryList);

            // 关联订单
            for (Order order : shopOrders) {
                DeliveryOrder deliveryOrder = new DeliveryOrder();
                deliveryOrder.setDeliveryListId(deliveryList.getId());
                deliveryOrder.setOrderId(order.getId());
                deliveryOrderMapper.insert(deliveryOrder);

                // 更新订单状态
                order.setStatus(OrderStatus.PENDING_DELIVERY.getCode());
                orderMapper.updateById(order);
            }
        }

        // 清空拣货清单
        pickingListMapper.delete(
                new LambdaQueryWrapper<PickingList>().eq(PickingList::getDriverId, driverId));
    }

    private OrderResponse convertToOrderResponse(Order order, List<OrderItem> items, Driver driver) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setStatus(order.getStatus());
        response.setStatusDesc(OrderStatus.fromCode(order.getStatus()).getDesc());
        response.setTotalAmount(order.getTotalAmount());
        response.setDeliveryDate(order.getDeliveryDate());
        response.setDriverId(order.getDriverId());
        response.setDriverName(driver != null ? driver.getName() : null);
        response.setReceivedAt(order.getReceivedAt());
        response.setCreatedAt(order.getCreatedAt());

        if (items != null) {
            response.setItems(items.stream()
                    .map(item -> {
                        OrderResponse.OrderItemResponse itemResponse = new OrderResponse.OrderItemResponse();
                        itemResponse.setId(item.getId());
                        itemResponse.setProductId(item.getProductId());
                        itemResponse.setProductName(item.getProductName());
                        itemResponse.setQuantity(item.getQuantity());
                        itemResponse.setUnitPrice(item.getUnitPrice());
                        itemResponse.setSubtotal(item.getSubtotal());
                        return itemResponse;
                    })
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private PickingListResponse.PickingItem convertToPickingItem(PickingList pickingList) {
        PickingListResponse.PickingItem item = new PickingListResponse.PickingItem();
        item.setId(pickingList.getId());
        item.setProductId(pickingList.getProductId());
        item.setProductName(pickingList.getProductName());
        item.setTotalQuantity(pickingList.getTotalQuantity());
        item.setPickedQuantity(pickingList.getPickedQuantity());
        item.setStatus(pickingList.getStatus());

        Product product = productMapper.selectById(pickingList.getProductId());
        if (product != null) {
            item.setUnit(product.getUnit());
        }

        return item;
    }

    private DeliveryListResponse convertToDeliveryListResponse(DeliveryList deliveryList) {
        DeliveryListResponse response = new DeliveryListResponse();
        response.setId(deliveryList.getId());
        response.setShopId(deliveryList.getShopId());
        response.setStatus(deliveryList.getStatus());
        response.setDeliveryPhoto(deliveryList.getDeliveryPhoto());
        response.setCompletedAt(deliveryList.getCompletedAt());

        Shop shop = shopMapper.selectById(deliveryList.getShopId());
        if (shop != null) {
            response.setShopName(shop.getName());
            response.setShopAddress(shop.getAddress());
            response.setShopPhone(shop.getPhone());
        }

        // 获取送货清单关联的订单
        List<DeliveryOrder> deliveryOrders = deliveryOrderMapper.selectList(
                new LambdaQueryWrapper<DeliveryOrder>().eq(DeliveryOrder::getDeliveryListId, deliveryList.getId()));

        // 汇总商品
        Map<Long, DeliveryListResponse.DeliveryItem> itemMap = new HashMap<>();
        for (DeliveryOrder deliveryOrder : deliveryOrders) {
            List<OrderItem> orderItems = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, deliveryOrder.getOrderId()));

            for (OrderItem orderItem : orderItems) {
                DeliveryListResponse.DeliveryItem item = itemMap.get(orderItem.getProductId());
                if (item == null) {
                    item = new DeliveryListResponse.DeliveryItem();
                    item.setProductName(orderItem.getProductName());
                    item.setQuantity(orderItem.getQuantity());
                    Product product = productMapper.selectById(orderItem.getProductId());
                    if (product != null) {
                        item.setUnit(product.getUnit());
                    }
                    itemMap.put(orderItem.getProductId(), item);
                } else {
                    item.setQuantity(item.getQuantity().add(orderItem.getQuantity()));
                }
            }
        }

        response.setItems(new ArrayList<>(itemMap.values()));
        return response;
    }

    @Override
    public DriverStatisticsResponse getStatistics(Long driverId) {
        DriverStatisticsResponse response = new DriverStatisticsResponse();

        java.time.LocalDate today = java.time.LocalDate.now();

        // 今日订单数
        Long todayOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getDriverId, driverId)
                        .eq(Order::getStatus, OrderStatus.COMPLETED.getCode())
                        .apply("DATE(received_at) = {0}", today));
        response.setTodayOrders(todayOrders != null ? todayOrders.intValue() : 0);

        // 今日店铺数（去重）
        List<Order> todayOrderList = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getDriverId, driverId)
                        .eq(Order::getStatus, OrderStatus.COMPLETED.getCode())
                        .apply("DATE(received_at) = {0}", today));
        long todayShopCount = todayOrderList.stream()
                .map(Order::getShopId)
                .distinct()
                .count();
        response.setTodayShops((int) todayShopCount);

        // 累计订单数
        Long totalOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getDriverId, driverId)
                        .eq(Order::getStatus, OrderStatus.COMPLETED.getCode()));
        response.setTotalOrders(totalOrders != null ? totalOrders.intValue() : 0);

        // 累计金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Order> allOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getDriverId, driverId)
                        .eq(Order::getStatus, OrderStatus.COMPLETED.getCode()));
        for (Order order : allOrders) {
            if (order.getTotalAmount() != null) {
                totalAmount = totalAmount.add(order.getTotalAmount());
            }
        }
        response.setTotalAmount(totalAmount);

        return response;
    }

    @Override
    public List<DeliveryHistoryResponse> getDeliveryHistory(Long driverId) {
        // 获取所有已完成的订单，按日期分组
        List<Order> completedOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getDriverId, driverId)
                        .eq(Order::getStatus, OrderStatus.COMPLETED.getCode())
                        .isNotNull(Order::getReceivedAt)
                        .orderByDesc(Order::getReceivedAt));

        // 按日期分组
        java.util.Map<java.time.LocalDate, List<Order>> ordersByDate = completedOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getReceivedAt().toLocalDate(),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()));

        List<DeliveryHistoryResponse> historyList = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (java.util.Map.Entry<java.time.LocalDate, List<Order>> entry : ordersByDate.entrySet()) {
            DeliveryHistoryResponse history = new DeliveryHistoryResponse();
            history.setDate(entry.getKey().format(formatter));
            history.setOrderCount(entry.getValue().size());

            // 店铺数（去重）
            long shopCount = entry.getValue().stream()
                    .map(Order::getShopId)
                    .distinct()
                    .count();
            history.setShopCount((int) shopCount);

            // 总金额
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Order order : entry.getValue()) {
                if (order.getTotalAmount() != null) {
                    totalAmount = totalAmount.add(order.getTotalAmount());
                }
            }
            history.setTotalAmount(totalAmount);

            historyList.add(history);
        }

        return historyList;
    }

    @Override
    @Transactional
    public void submitProductImageRequest(Long driverId, ProductImageRequestDto request) {
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        ProductImageRequest imageRequest = new ProductImageRequest();
        imageRequest.setDriverId(driverId);
        imageRequest.setBranchManagerId(driver.getBranchManagerId());
        imageRequest.setProductId(request.getProductId());
        imageRequest.setProductName(request.getProductName());
        imageRequest.setCategory(request.getCategory());
        imageRequest.setImageUrl(request.getImageUrl());
        imageRequest.setDescription(request.getDescription());
        imageRequest.setStatus(0); // 待审核

        productImageRequestMapper.insert(imageRequest);
    }

    @Override
    public List<ProductImageRequestResponse> getMyProductImageRequests(Long driverId) {
        List<ProductImageRequest> requests = productImageRequestMapper.selectList(
                new LambdaQueryWrapper<ProductImageRequest>()
                        .eq(ProductImageRequest::getDriverId, driverId)
                        .orderByDesc(ProductImageRequest::getCreatedAt));

        return requests.stream()
                .map(this::convertToProductImageRequestResponse)
                .collect(Collectors.toList());
    }

    private ProductImageRequestResponse convertToProductImageRequestResponse(ProductImageRequest request) {
        ProductImageRequestResponse response = new ProductImageRequestResponse();
        response.setId(request.getId());
        response.setDriverId(request.getDriverId());
        response.setProductId(request.getProductId());
        response.setProductName(request.getProductName());
        response.setCategory(request.getCategory());
        response.setImageUrl(request.getImageUrl());
        response.setDescription(request.getDescription());
        response.setStatus(request.getStatus());
        response.setRejectReason(request.getRejectReason());
        response.setCreatedAt(request.getCreatedAt());
        response.setReviewedAt(request.getReviewedAt());

        // 获取司机名称
        Driver driver = driverMapper.selectById(request.getDriverId());
        if (driver != null) {
            response.setDriverName(driver.getName());
        }

        // 状态文本
        switch (request.getStatus()) {
            case 0:
                response.setStatusText("待审核");
                break;
            case 1:
                response.setStatusText("已通过");
                break;
            case 2:
                response.setStatusText("已拒绝");
                break;
            default:
                response.setStatusText("未知");
        }

        return response;
    }

    // ==================== 拣货修改订单功能 ====================

    @Override
    public List<OrderDetailResponse> getSelectedOrdersDetail(Long driverId) {
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 获取该司机已选择待拣货的订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getDriverId, driverId)
                        .eq(Order::getStatus, OrderStatus.PENDING_PICKUP.getCode())
                        .orderByAsc(Order::getDeliveryDate));

        return orders.stream()
                .map(order -> {
                    OrderDetailResponse response = new OrderDetailResponse();
                    response.setId(order.getId());
                    response.setOrderNo(order.getOrderNo());
                    response.setShopId(order.getShopId());
                    response.setStatus(order.getStatus());
                    response.setStatusDesc(OrderStatus.fromCode(order.getStatus()).getDesc());
                    response.setTotalAmount(order.getTotalAmount());

                    Shop shop = shopMapper.selectById(order.getShopId());
                    if (shop != null) {
                        response.setShopName(shop.getName());
                    }

                    // 获取订单商品
                    List<OrderItem> items = orderItemMapper.selectList(
                            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

                    List<OrderDetailResponse.OrderItemDetail> itemDetails = items.stream()
                            .map(item -> {
                                OrderDetailResponse.OrderItemDetail detail = new OrderDetailResponse.OrderItemDetail();
                                detail.setId(item.getId());
                                detail.setProductId(item.getProductId());
                                detail.setProductName(item.getProductName());
                                detail.setQuantity(item.getQuantity());
                                detail.setUnitPrice(item.getUnitPrice());
                                detail.setSubtotal(item.getSubtotal());

                                Product product = productMapper.selectById(item.getProductId());
                                if (product != null) {
                                    detail.setUnit(product.getUnit());
                                }
                                return detail;
                            })
                            .collect(Collectors.toList());

                    response.setItems(itemDetails);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modifyOrderItems(Long driverId, OrderItemModifyRequest request) {
        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要修改的订单");
        }

        if (request.getModifications() == null || request.getModifications().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请提供修改内容");
        }

        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证订单归属
        for (Long orderId : request.getOrderIds()) {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
            }
            if (!driverId.equals(order.getDriverId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
            }
            if (!OrderStatus.PENDING_PICKUP.getCode().equals(order.getStatus())) {
                throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态不正确，无法修改");
            }
        }

        // 处理每个修改项
        for (OrderItemModifyRequest.ModifyItem modifyItem : request.getModifications()) {
            if ("add".equals(modifyItem.getType())) {
                // 新增商品到订单
                addProductToOrders(driverId, request.getOrderIds(), modifyItem);
            } else if ("modify".equals(modifyItem.getType())) {
                // 修改现有商品数量
                modifyProductQuantity(request.getOrderIds(), modifyItem);
            }
        }

        // 重新计算订单金额
        for (Long orderId : request.getOrderIds()) {
            recalculateOrderAmount(orderId);
        }

        // 更新拣货清单
        refreshPickingList(driverId);
    }

    /**
     * 添加商品到订单
     */
    private void addProductToOrders(Long driverId, List<Long> orderIds, OrderItemModifyRequest.ModifyItem modifyItem) {
        Product product = productMapper.selectById(modifyItem.getProductId());
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在");
        }

        BigDecimal quantity = modifyItem.getQuantityChange();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "添加数量必须大于0");
        }

        BigDecimal unitPrice = modifyItem.getUnitPrice();
        if (unitPrice == null) {
            unitPrice = product.getOldPrice(); // 默认使用老用户价格
        }

        for (Long orderId : orderIds) {
            // 检查订单中是否已有该商品
            OrderItem existingItem = orderItemMapper.selectOne(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, orderId)
                            .eq(OrderItem::getProductId, modifyItem.getProductId()));

            if (existingItem != null) {
                // 已存在，增加数量
                existingItem.setQuantity(existingItem.getQuantity().add(quantity));
                existingItem.setSubtotal(existingItem.getQuantity().multiply(existingItem.getUnitPrice()));
                orderItemMapper.updateById(existingItem);
            } else {
                // 不存在，新增
                OrderItem newItem = new OrderItem();
                newItem.setOrderId(orderId);
                newItem.setProductId(modifyItem.getProductId());
                newItem.setProductName(product.getName());
                newItem.setQuantity(quantity);
                newItem.setUnitPrice(unitPrice);
                newItem.setSubtotal(quantity.multiply(unitPrice));
                orderItemMapper.insert(newItem);
            }
        }
    }

    /**
     * 修改商品数量
     */
    private void modifyProductQuantity(List<Long> orderIds, OrderItemModifyRequest.ModifyItem modifyItem) {
        BigDecimal quantityChange = modifyItem.getQuantityChange();
        if (quantityChange == null || quantityChange.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        for (Long orderId : orderIds) {
            OrderItem orderItem = orderItemMapper.selectOne(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, orderId)
                            .eq(OrderItem::getProductId, modifyItem.getProductId()));

            if (orderItem == null) {
                continue;
            }

            BigDecimal newQuantity = orderItem.getQuantity().add(quantityChange);

            if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                // 数量为0或负数，删除该商品项
                orderItemMapper.deleteById(orderItem.getId());
            } else {
                // 更新数量
                orderItem.setQuantity(newQuantity);
                orderItem.setSubtotal(newQuantity.multiply(orderItem.getUnitPrice()));
                orderItemMapper.updateById(orderItem);
            }
        }
    }

    /**
     * 重新计算订单金额
     */
    private void recalculateOrderAmount(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));

        BigDecimal totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            order.setTotalAmount(totalAmount);
            orderMapper.updateById(order);
        }
    }

    /**
     * 刷新拣货清单
     */
    private void refreshPickingList(Long driverId) {
        // 删除旧的拣货清单
        pickingListMapper.delete(
                new LambdaQueryWrapper<PickingList>().eq(PickingList::getDriverId, driverId));

        // 重新生成拣货清单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getDriverId, driverId)
                        .eq(Order::getStatus, OrderStatus.PENDING_PICKUP.getCode()));

        for (Order order : orders) {
            List<OrderItem> orderItems = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

            for (OrderItem orderItem : orderItems) {
                // 查找是否已有该商品的拣货项
                PickingList existingItem = pickingListMapper.selectOne(
                        new LambdaQueryWrapper<PickingList>()
                                .eq(PickingList::getDriverId, driverId)
                                .eq(PickingList::getProductId, orderItem.getProductId())
                                .eq(PickingList::getStatus, 0));

                if (existingItem != null) {
                    // 累加数量
                    existingItem.setTotalQuantity(existingItem.getTotalQuantity().add(orderItem.getQuantity()));
                    pickingListMapper.updateById(existingItem);
                } else {
                    // 创建新的拣货项
                    PickingList pickingItem = new PickingList();
                    pickingItem.setDriverId(driverId);
                    pickingItem.setProductId(orderItem.getProductId());
                    pickingItem.setProductName(orderItem.getProductName());
                    pickingItem.setTotalQuantity(orderItem.getQuantity());
                    pickingItem.setPickedQuantity(BigDecimal.ZERO);
                    pickingItem.setStatus(0);
                    pickingListMapper.insert(pickingItem);
                }
            }
        }
    }

    @Override
    public List<ProductResponse> getAvailableProducts(Long driverId) {
        log.info("获取可用商品列表，司机ID: {}", driverId);

        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            log.error("司机不存在，ID: {}", driverId);
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        log.info("司机信息: branchManagerId={}, name={}, phone={}",
                driver.getBranchManagerId(), driver.getName(), driver.getPhone());

        // 获取该司机所属分管理的商品库
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getBranchManagerId, driver.getBranchManagerId())
                        .eq(Product::getStatus, 1)
                        .orderByAsc(Product::getCategory)
                        .orderByAsc(Product::getName));

        log.info("查询到商品数量: {}", products.size());

        return products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    /**
     * 转换商品实体为响应DTO
     */
    private ProductResponse convertToProductResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setCategory(product.getCategory());
        response.setOldPrice(product.getOldPrice());
        response.setNewPrice(product.getNewPrice());
        response.setUnit(product.getUnit());
        response.setDescription(product.getDescription());
        response.setImageUrl(product.getImageUrl());
        response.setStatus(product.getStatus());
        response.setCreatedAt(product.getCreatedAt());
        return response;
    }
}
