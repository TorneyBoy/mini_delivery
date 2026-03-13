package com.delivery.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delivery.common.PageResult;
import com.delivery.common.ResultCode;
import com.delivery.dto.request.OrderCreateRequest;
import com.delivery.dto.response.BillResponse;
import com.delivery.dto.response.OrderResponse;
import com.delivery.dto.response.ProductResponse;
import com.delivery.dto.response.ShopResponse;
import com.delivery.entity.*;
import com.delivery.enums.BillStatus;
import com.delivery.enums.OrderStatus;
import com.delivery.enums.PriceType;
import com.delivery.exception.BusinessException;
import com.delivery.mapper.*;
import com.delivery.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 店铺服务实现
 */
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final BillMapper billMapper;
    private final BillOrderMapper billOrderMapper;
    private final DriverMapper driverMapper;

    @Override
    public ShopResponse getShopInfo(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        ShopResponse response = new ShopResponse();
        response.setId(shop.getId());
        response.setName(shop.getName());
        response.setAddress(shop.getAddress());
        response.setPhone(shop.getPhone());
        response.setShowPrice(shop.getShowPrice());
        response.setPriceType(shop.getPriceType());
        response.setStatus(shop.getStatus());

        return response;
    }

    @Override
    public PageResult<ProductResponse> getProductList(Long shopId, Integer page, Integer size, String category) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getBranchManagerId, shop.getBranchManagerId())
                .eq(Product::getStatus, 1);

        if (StringUtils.hasText(category)) {
            wrapper.eq(Product::getCategory, category);
        }

        wrapper.orderByDesc(Product::getCreatedAt);

        Page<Product> result = productMapper.selectPage(pageParam, wrapper);

        return PageResult.of(result.convert(product -> {
            ProductResponse response = new ProductResponse();
            response.setId(product.getId());
            response.setName(product.getName());
            response.setCategory(product.getCategory());
            // 根据店铺的价格体系显示对应价格
            if (shop.getPriceType().equals(PriceType.OLD_USER.getCode())) {
                response.setOldPrice(product.getOldPrice());
                response.setNewPrice(product.getOldPrice()); // 前端显示用
            } else {
                response.setOldPrice(product.getNewPrice());
                response.setNewPrice(product.getNewPrice());
            }
            response.setUnit(product.getUnit());
            response.setDescription(product.getDescription());
            response.setImageUrl(product.getImageUrl());
            response.setStatus(product.getStatus());
            response.setCreatedAt(product.getCreatedAt());
            return response;
        }));
    }

    @Override
    public ProductResponse getProductDetail(Long shopId, Long productId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        Product product = productMapper.selectOne(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getId, productId)
                        .eq(Product::getBranchManagerId, shop.getBranchManagerId())
                        .eq(Product::getStatus, 1));

        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setCategory(product.getCategory());
        if (shop.getPriceType().equals(PriceType.OLD_USER.getCode())) {
            response.setOldPrice(product.getOldPrice());
            response.setNewPrice(product.getOldPrice());
        } else {
            response.setOldPrice(product.getNewPrice());
            response.setNewPrice(product.getNewPrice());
        }
        response.setUnit(product.getUnit());
        response.setDescription(product.getDescription());
        response.setImageUrl(product.getImageUrl());
        response.setStatus(product.getStatus());
        response.setCreatedAt(product.getCreatedAt());

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(Long shopId, OrderCreateRequest request) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 计算总金额并创建订单商品
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productMapper.selectOne(
                    new LambdaQueryWrapper<Product>()
                            .eq(Product::getId, itemRequest.getProductId())
                            .eq(Product::getBranchManagerId, shop.getBranchManagerId())
                            .eq(Product::getStatus, 1));

            if (product == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND, "商品不存在: " + itemRequest.getProductId());
            }

            // 根据店铺价格体系确定单价
            BigDecimal unitPrice = shop.getPriceType().equals(PriceType.OLD_USER.getCode())
                    ? product.getOldPrice()
                    : product.getNewPrice();

            BigDecimal subtotal = unitPrice.multiply(itemRequest.getQuantity());
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(subtotal);

            orderItems.add(orderItem);
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setShopId(shopId);
        order.setStatus(OrderStatus.PENDING_ASSIGNMENT.getCode());
        order.setTotalAmount(totalAmount);
        order.setDeliveryDate(request.getDeliveryDate());

        orderMapper.insert(order);

        // 保存订单商品
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItemMapper.insert(orderItem);
        }

        return convertToOrderResponse(order, orderItems, null);
    }

    @Override
    public PageResult<OrderResponse> getOrderList(Long shopId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId);

        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }

        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> pageParam = new Page<>(page, size);
        Page<Order> result = orderMapper.selectPage(pageParam, wrapper);

        return PageResult.of(result.convert(order -> {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            Driver driver = order.getDriverId() != null ? driverMapper.selectById(order.getDriverId()) : null;
            return convertToOrderResponse(order, items, driver);
        }));
    }

    @Override
    public OrderResponse getOrderDetail(Long shopId, Long orderId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getShopId, shopId));

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        Driver driver = order.getDriverId() != null ? driverMapper.selectById(order.getDriverId()) : null;

        return convertToOrderResponse(order, items, driver);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrder(Long shopId, Long orderId, OrderCreateRequest request) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getShopId, shopId));

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        // 检查是否可以修改（仅限当日订单在凌晨2点前）
        if (!canModifyOrder(order)) {
            throw new BusinessException(ResultCode.ORDER_CANNOT_MODIFY);
        }

        Shop shop = shopMapper.selectById(shopId);

        // 删除原有订单商品
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));

        // 重新计算并创建订单商品
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productMapper.selectById(itemRequest.getProductId());
            if (product == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
            }

            BigDecimal unitPrice = shop.getPriceType().equals(PriceType.OLD_USER.getCode())
                    ? product.getOldPrice()
                    : product.getNewPrice();

            BigDecimal subtotal = unitPrice.multiply(itemRequest.getQuantity());
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(subtotal);

            orderItemMapper.insert(orderItem);
            orderItems.add(orderItem);
        }

        // 更新订单
        order.setTotalAmount(totalAmount);
        order.setDeliveryDate(request.getDeliveryDate());
        orderMapper.updateById(order);

        Driver driver = order.getDriverId() != null ? driverMapper.selectById(order.getDriverId()) : null;
        return convertToOrderResponse(order, orderItems, driver);
    }

    @Override
    public void confirmReceive(Long shopId, Long orderId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getShopId, shopId));

        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        if (!order.getStatus().equals(OrderStatus.PENDING_DELIVERY.getCode())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态不正确，无法确认收货");
        }

        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setReceivedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public PageResult<BillResponse> getBillList(Long shopId, Integer page, Integer size) {
        Page<Bill> pageParam = new Page<>(page, size);
        Page<Bill> result = billMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Bill>()
                        .eq(Bill::getShopId, shopId)
                        .orderByDesc(Bill::getBillDate));

        return PageResult.of(result.convert(this::convertToBillResponse));
    }

    @Override
    public BillResponse getBillDetail(Long shopId, Long billId) {
        Bill bill = billMapper.selectOne(
                new LambdaQueryWrapper<Bill>()
                        .eq(Bill::getId, billId)
                        .eq(Bill::getShopId, shopId));

        if (bill == null) {
            throw new BusinessException(ResultCode.BILL_NOT_FOUND);
        }

        BillResponse response = convertToBillResponse(bill);

        // 获取账单关联的订单
        List<BillOrder> billOrders = billOrderMapper.selectList(
                new LambdaQueryWrapper<BillOrder>().eq(BillOrder::getBillId, billId));

        List<BillResponse.BillOrderResponse> orderResponses = new ArrayList<>();
        for (BillOrder billOrder : billOrders) {
            Order order = orderMapper.selectById(billOrder.getOrderId());
            if (order != null) {
                List<OrderItem> items = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

                BillResponse.BillOrderResponse orderResponse = new BillResponse.BillOrderResponse();
                orderResponse.setOrderId(order.getId());
                orderResponse.setOrderNo(order.getOrderNo());
                orderResponse.setOrderAmount(order.getTotalAmount());
                orderResponse
                        .setItems(items.stream().map(this::convertToOrderItemResponse).collect(Collectors.toList()));

                orderResponses.add(orderResponse);
            }
        }

        response.setOrders(orderResponses);
        return response;
    }

    @Override
    public List<String> getCategoryList(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 使用DISTINCT查询分类，避免GROUP BY的问题
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .select(Product::getCategory)
                        .eq(Product::getBranchManagerId, shop.getBranchManagerId())
                        .eq(Product::getStatus, 1)
                        .isNotNull(Product::getCategory));

        return products.stream()
                .map(Product::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    // ==================== 辅助方法 ====================

    private String generateOrderNo() {
        return "ORD" + IdUtil.getSnowflakeNextIdStr();
    }

    private boolean canModifyOrder(Order order) {
        LocalDate today = LocalDate.now();
        LocalDate deliveryDate = order.getDeliveryDate();
        LocalTime currentTime = LocalDateTime.now().toLocalTime();
        LocalTime deadline = LocalTime.of(2, 0);

        // 预定订单（收货日期在今天之后）：可修改
        if (deliveryDate.isAfter(today)) {
            return true;
        }

        // 今日订单：凌晨2点前可修改
        if (deliveryDate.equals(today)) {
            return currentTime.isBefore(deadline);
        }

        // 历史订单：不可修改
        return false;
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
                    .map(this::convertToOrderItemResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private OrderResponse.OrderItemResponse convertToOrderItemResponse(OrderItem item) {
        OrderResponse.OrderItemResponse response = new OrderResponse.OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProductId());
        response.setProductName(item.getProductName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setSubtotal(item.getSubtotal());
        return response;
    }

    private BillResponse convertToBillResponse(Bill bill) {
        BillResponse response = new BillResponse();
        response.setId(bill.getId());
        response.setBillNo(bill.getBillNo());
        response.setBillDate(bill.getBillDate());
        response.setTotalAmount(bill.getTotalAmount());
        response.setStatus(bill.getStatus());
        response.setStatusDesc(BillStatus.fromCode(bill.getStatus()).getDesc());
        response.setPaidAt(bill.getPaidAt());
        response.setWechatTransactionId(bill.getWechatTransactionId());
        response.setCreatedAt(bill.getCreatedAt());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payBill(Long shopId, Long billId) {
        Bill bill = billMapper.selectOne(
                new LambdaQueryWrapper<Bill>()
                        .eq(Bill::getId, billId)
                        .eq(Bill::getShopId, shopId));

        if (bill == null) {
            throw new BusinessException(ResultCode.BILL_NOT_FOUND);
        }

        if (!bill.getStatus().equals(BillStatus.PENDING_PAYMENT.getCode())) {
            throw new BusinessException(ResultCode.BILL_STATUS_ERROR, "账单状态不正确，无法支付");
        }

        // 更新账单状态为已支付
        bill.setStatus(BillStatus.PAID.getCode());
        bill.setPaidAt(LocalDateTime.now());
        // 生成模拟的微信交易号
        bill.setWechatTransactionId("WX" + IdUtil.getSnowflakeNextIdStr());
        billMapper.updateById(bill);
    }
}
