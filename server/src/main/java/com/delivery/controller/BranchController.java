package com.delivery.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delivery.common.PageResult;
import com.delivery.common.Result;
import com.delivery.common.ResultCode;
import com.delivery.dto.request.ShopCreateRequest;
import com.delivery.dto.request.DriverCreateRequest;
import com.delivery.dto.request.ProductCreateRequest;
import com.delivery.dto.request.ProductImageReviewDto;
import com.delivery.dto.request.ChangePasswordRequest;
import com.delivery.dto.response.OrderResponse;
import com.delivery.dto.response.ShopProductStatisticsResponse;
import com.delivery.dto.response.ShopResponse;
import com.delivery.dto.response.DriverResponse;
import com.delivery.dto.response.ProductResponse;
import com.delivery.dto.response.ProductImageRequestResponse;
import com.delivery.dto.response.MeituanProductResponse;
import com.delivery.entity.*;
import com.delivery.enums.BillStatus;
import com.delivery.enums.OrderStatus;
import com.delivery.exception.BusinessException;
import com.delivery.mapper.*;
import com.delivery.service.MeituanService;
import com.delivery.service.WechatMessageService;
import com.delivery.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分管理端控制器
 */
@Slf4j
@Tag(name = "分管理端接口", description = "分管理端相关接口")
@RestController
@RequestMapping("/api/branch")
@RequiredArgsConstructor
public class BranchController {

    private final ShopMapper shopMapper;
    private final DriverMapper driverMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final BillMapper billMapper;
    private final BillOrderMapper billOrderMapper;
    private final ProductImageRequestMapper productImageRequestMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final DeliveryListMapper deliveryListMapper;
    private final BranchManagerMapper branchManagerMapper;
    private final PasswordEncoder passwordEncoder;
    private final MeituanService meituanService;
    private final WechatMessageService wechatMessageService;

    /**
     * 获取当前登录的分管理ID，如果未登录则抛出异常
     */
    private Long requireCurrentBranchManagerId() {
        Long id = UserContext.getCurrentUserId();
        if (id == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return id;
    }

    /**
     * 获取统计数据
     */
    @Operation(summary = "获取统计数据", description = "获取分管理首页统计数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Long branchManagerId = requireCurrentBranchManagerId();

        Map<String, Object> statistics = new HashMap<>();

        // 统计店铺数量
        Long shopCount = shopMapper.selectCount(
                new LambdaQueryWrapper<Shop>().eq(Shop::getBranchManagerId, branchManagerId));
        statistics.put("shopCount", shopCount);

        // 统计司机数量
        Long driverCount = driverMapper.selectCount(
                new LambdaQueryWrapper<Driver>().eq(Driver::getBranchManagerId, branchManagerId));
        statistics.put("driverCount", driverCount);

        // 统计商品数量
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getBranchManagerId, branchManagerId));
        statistics.put("productCount", productCount);

        // 统计订单数量
        List<Long> shopIds = shopMapper.selectList(
                new LambdaQueryWrapper<Shop>().eq(Shop::getBranchManagerId, branchManagerId))
                .stream().map(Shop::getId).collect(Collectors.toList());

        Long orderCount = 0L;
        if (!shopIds.isEmpty()) {
            orderCount = orderMapper.selectCount(
                    new LambdaQueryWrapper<Order>().in(Order::getShopId, shopIds));
        }
        statistics.put("orderCount", orderCount);

        return Result.success(statistics);
    }

    /**
     * 获取数据中心数据
     */
    @Operation(summary = "获取数据中心数据", description = "获取分管理数据中心详细数据")
    @GetMapping("/data-center")
    public Result<Map<String, Object>> getDataCenter(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Long branchManagerId = requireCurrentBranchManagerId();

        // 解析日期，默认近三个月
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusMonths(3);

        Map<String, Object> result = new HashMap<>();

        // 获取该分管理下所有店铺ID
        List<Long> shopIds = shopMapper.selectList(
                new LambdaQueryWrapper<Shop>().eq(Shop::getBranchManagerId, branchManagerId))
                .stream().map(Shop::getId).collect(Collectors.toList());

        // 1. 基础统计数据
        Map<String, Object> overview = new HashMap<>();
        overview.put("shopCount", shopIds.size());

        Long driverCount = driverMapper.selectCount(
                new LambdaQueryWrapper<Driver>().eq(Driver::getBranchManagerId, branchManagerId));
        overview.put("driverCount", driverCount);

        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getBranchManagerId, branchManagerId));
        overview.put("productCount", productCount);

        // 订单统计
        Long totalOrders = 0L;
        Long completedOrders = 0L;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (!shopIds.isEmpty()) {
            totalOrders = orderMapper.selectCount(
                    new LambdaQueryWrapper<Order>().in(Order::getShopId, shopIds));

            List<Order> completed = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .in(Order::getShopId, shopIds)
                            .eq(Order::getStatus, OrderStatus.COMPLETED.getCode()));
            completedOrders = (long) completed.size();
            totalAmount = completed.stream()
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        overview.put("totalOrders", totalOrders);
        overview.put("completedOrders", completedOrders);
        overview.put("totalAmount", totalAmount);

        // 计算订单完成率
        BigDecimal completionRate = totalOrders > 0
                ? new BigDecimal(completedOrders).divide(new BigDecimal(totalOrders), 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal(100))
                : BigDecimal.ZERO;
        overview.put("completionRate", completionRate);

        result.put("overview", overview);

        // 2. 商品销量变化趋势
        List<Map<String, Object>> salesTrend = getSalesTrend(branchManagerId, shopIds, start, end);
        result.put("salesTrend", salesTrend);

        // 3. 商品订单量排行榜
        List<Map<String, Object>> productOrderRank = getProductOrderRank(shopIds, start, end, 10);
        result.put("productOrderRank", productOrderRank);

        // 4. 商品销量排行榜
        List<Map<String, Object>> productSalesRank = getProductSalesRank(shopIds, start, end, 10);
        result.put("productSalesRank", productSalesRank);

        // 5. 入账分布（按店铺）
        List<Map<String, Object>> revenueByShop = getRevenueByShop(shopIds, start, end, 10);
        result.put("revenueByShop", revenueByShop);

        // 6. 订单状态分布
        Map<String, Object> orderStatusDist = getOrderStatusDistribution(shopIds);
        result.put("orderStatusDist", orderStatusDist);

        // 7. 账单统计
        Map<String, Object> billStats = getBillStatistics(shopIds);
        result.put("billStats", billStats);

        // 8. 按送达日期整理的下单数据统计（预约订单统计）
        List<Map<String, Object>> orderStatsByDeliveryDate = getOrderStatsByDeliveryDate(branchManagerId, shopIds,
                start, end);
        result.put("orderStatsByDeliveryDate", orderStatsByDeliveryDate);

        // 9. 按送达日期整理的送达数据统计
        List<Map<String, Object>> deliveryStatsByDate = getDeliveryStatsByDate(branchManagerId, shopIds, start, end);
        result.put("deliveryStatsByDate", deliveryStatsByDate);

        return Result.success(result);
    }

    /**
     * 按送达日期整理的下单数据统计（预约订单统计）
     * 用于了解预约的订单，方便根据需要对接批发商
     */
    private List<Map<String, Object>> getOrderStatsByDeliveryDate(Long branchManagerId, List<Long> shopIds,
            LocalDate start, LocalDate end) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (shopIds.isEmpty()) {
            return result;
        }

        // 获取该分管理下所有商品
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getBranchManagerId, branchManagerId));

        // 按送达日期分组统计
        LocalDate current = start;
        while (!current.isAfter(end)) {
            final LocalDate deliveryDate = current;

            // 获取该送达日期的所有订单
            List<Order> orders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .in(Order::getShopId, shopIds)
                            .eq(Order::getDeliveryDate, deliveryDate));

            if (!orders.isEmpty()) {
                List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
                List<OrderItem> orderItems = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));

                // 按商品分组统计
                Map<Long, BigDecimal> productQuantityMap = orderItems.stream()
                        .collect(Collectors.groupingBy(OrderItem::getProductId,
                                Collectors.reducing(BigDecimal.ZERO, OrderItem::getQuantity, BigDecimal::add)));

                Map<String, Object> dateData = new HashMap<>();
                dateData.put("date", deliveryDate.toString());

                List<Map<String, Object>> productStats = new ArrayList<>();
                for (Product product : products) {
                    BigDecimal quantity = productQuantityMap.getOrDefault(product.getId(), BigDecimal.ZERO);
                    if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                        Map<String, Object> productStat = new HashMap<>();
                        productStat.put("productId", product.getId());
                        productStat.put("productName", product.getName());
                        productStat.put("unit", product.getUnit());
                        productStat.put("orderQuantity", quantity);
                        productStats.add(productStat);
                    }
                }

                // 按商品名称排序
                productStats.sort((a, b) -> {
                    String nameA = (String) a.get("productName");
                    String nameB = (String) b.get("productName");
                    if (nameA == null)
                        return 1;
                    if (nameB == null)
                        return -1;
                    return nameA.compareTo(nameB);
                });

                dateData.put("products", productStats);
                dateData.put("totalOrders", orders.size());
                result.add(dateData);
            }

            current = current.plusDays(1);
        }

        return result;
    }

    /**
     * 按送达日期整理的送达数据统计
     * 用于了解商品销量趋势和与预约订单的差距
     */
    private List<Map<String, Object>> getDeliveryStatsByDate(Long branchManagerId, List<Long> shopIds, LocalDate start,
            LocalDate end) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (shopIds.isEmpty()) {
            return result;
        }

        // 获取该分管理下所有商品
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getBranchManagerId, branchManagerId));

        // 获取该分管理下所有司机ID
        List<Long> driverIds = driverMapper.selectList(
                new LambdaQueryWrapper<Driver>().eq(Driver::getBranchManagerId, branchManagerId))
                .stream().map(Driver::getId).collect(Collectors.toList());

        if (driverIds.isEmpty()) {
            return result;
        }

        // 按送达日期（完成时间）分组统计
        LocalDate current = start;
        while (!current.isAfter(end)) {
            final LocalDate date = current;

            // 获取当天完成的送货清单
            List<DeliveryList> deliveryLists = deliveryListMapper.selectList(
                    new LambdaQueryWrapper<DeliveryList>()
                            .in(DeliveryList::getDriverId, driverIds)
                            .eq(DeliveryList::getStatus, 1) // 已完成
                            .ge(DeliveryList::getCompletedAt, date.atStartOfDay())
                            .lt(DeliveryList::getCompletedAt, date.plusDays(1).atStartOfDay()));

            if (!deliveryLists.isEmpty()) {
                // 获取这些送货清单关联的订单
                List<Long> deliveryListIds = deliveryLists.stream().map(DeliveryList::getId)
                        .collect(Collectors.toList());
                List<DeliveryOrder> deliveryOrders = deliveryOrderMapper.selectList(
                        new LambdaQueryWrapper<DeliveryOrder>().in(DeliveryOrder::getDeliveryListId, deliveryListIds));

                List<Long> orderIds = deliveryOrders.stream().map(DeliveryOrder::getOrderId)
                        .collect(Collectors.toList());

                if (!orderIds.isEmpty()) {
                    List<OrderItem> orderItems = orderItemMapper.selectList(
                            new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));

                    // 按商品分组统计
                    Map<Long, BigDecimal> productQuantityMap = orderItems.stream()
                            .collect(Collectors.groupingBy(OrderItem::getProductId,
                                    Collectors.reducing(BigDecimal.ZERO, OrderItem::getQuantity, BigDecimal::add)));

                    Map<String, Object> dateData = new HashMap<>();
                    dateData.put("date", date.toString());

                    List<Map<String, Object>> productStats = new ArrayList<>();
                    for (Product product : products) {
                        BigDecimal quantity = productQuantityMap.getOrDefault(product.getId(), BigDecimal.ZERO);
                        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                            Map<String, Object> productStat = new HashMap<>();
                            productStat.put("productId", product.getId());
                            productStat.put("productName", product.getName());
                            productStat.put("unit", product.getUnit());
                            productStat.put("deliveredQuantity", quantity);
                            productStats.add(productStat);
                        }
                    }

                    // 按商品名称排序
                    productStats.sort((a, b) -> {
                        String nameA = (String) a.get("productName");
                        String nameB = (String) b.get("productName");
                        if (nameA == null)
                            return 1;
                        if (nameB == null)
                            return -1;
                        return nameA.compareTo(nameB);
                    });

                    dateData.put("products", productStats);
                    dateData.put("totalDeliveries", deliveryLists.size());
                    result.add(dateData);
                }
            }

            current = current.plusDays(1);
        }

        return result;
    }

    /**
     * 获取销量变化趋势
     */
    private List<Map<String, Object>> getSalesTrend(Long branchManagerId, List<Long> shopIds, LocalDate start,
            LocalDate end) {
        List<Map<String, Object>> trend = new ArrayList<>();

        if (shopIds.isEmpty()) {
            return trend;
        }

        // 获取该分管理下所有商品
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getBranchManagerId, branchManagerId));

        for (Product product : products) {
            Map<String, Object> productTrend = new HashMap<>();
            productTrend.put("productId", product.getId());
            productTrend.put("productName", product.getName());

            List<Map<String, Object>> dailyData = new ArrayList<>();
            LocalDate current = start;

            while (!current.isAfter(end)) {
                final LocalDate date = current;

                // 获取当天该商品的销量
                List<Order> orders = orderMapper.selectList(
                        new LambdaQueryWrapper<Order>()
                                .in(Order::getShopId, shopIds)
                                .ge(Order::getCreatedAt, date.atStartOfDay())
                                .lt(Order::getCreatedAt, date.plusDays(1).atStartOfDay()));

                if (!orders.isEmpty()) {
                    List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
                    List<OrderItem> orderItems = orderItemMapper.selectList(
                            new LambdaQueryWrapper<OrderItem>()
                                    .eq(OrderItem::getProductId, product.getId())
                                    .in(OrderItem::getOrderId, orderIds));

                    BigDecimal dailySales = orderItems.stream()
                            .map(OrderItem::getQuantity)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Map<String, Object> dayData = new HashMap<>();
                    dayData.put("date", date.toString());
                    dayData.put("quantity", dailySales);
                    dailyData.add(dayData);
                } else {
                    Map<String, Object> dayData = new HashMap<>();
                    dayData.put("date", date.toString());
                    dayData.put("quantity", BigDecimal.ZERO);
                    dailyData.add(dayData);
                }

                current = current.plusDays(1);
            }

            productTrend.put("dailyData", dailyData);
            trend.add(productTrend);
        }

        return trend;
    }

    /**
     * 获取商品订单量排行榜
     */
    private List<Map<String, Object>> getProductOrderRank(List<Long> shopIds, LocalDate start, LocalDate end,
            int limit) {
        if (shopIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .in(Order::getShopId, shopIds)
                        .ge(Order::getCreatedAt, start.atStartOfDay())
                        .le(Order::getCreatedAt, end.plusDays(1).atStartOfDay()));

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));

        Map<Long, Long> productOrderCount = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getProductId, Collectors.counting()));

        return productOrderCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    Product product = productMapper.selectById(entry.getKey());
                    if (product != null) {
                        item.put("productId", product.getId());
                        item.put("productName", product.getName());
                        item.put("orderCount", entry.getValue());
                    }
                    return item;
                })
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 获取商品销量排行榜
     */
    private List<Map<String, Object>> getProductSalesRank(List<Long> shopIds, LocalDate start, LocalDate end,
            int limit) {
        if (shopIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .in(Order::getShopId, shopIds)
                        .ge(Order::getCreatedAt, start.atStartOfDay())
                        .le(Order::getCreatedAt, end.plusDays(1).atStartOfDay()));

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));

        Map<Long, BigDecimal> productSales = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getProductId,
                        Collectors.reducing(BigDecimal.ZERO, OrderItem::getQuantity, BigDecimal::add)));

        return productSales.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    Product product = productMapper.selectById(entry.getKey());
                    if (product != null) {
                        item.put("productId", product.getId());
                        item.put("productName", product.getName());
                        item.put("salesQuantity", entry.getValue());
                        item.put("unit", product.getUnit());
                    }
                    return item;
                })
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 获取按店铺的入账分布
     */
    private List<Map<String, Object>> getRevenueByShop(List<Long> shopIds, LocalDate start, LocalDate end, int limit) {
        if (shopIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .in(Order::getShopId, shopIds)
                        .eq(Order::getStatus, OrderStatus.COMPLETED.getCode())
                        .ge(Order::getCreatedAt, start.atStartOfDay())
                        .le(Order::getCreatedAt, end.plusDays(1).atStartOfDay()));

        Map<Long, BigDecimal> shopRevenue = orders.stream()
                .collect(Collectors.groupingBy(Order::getShopId,
                        Collectors.reducing(BigDecimal.ZERO,
                                o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO,
                                BigDecimal::add)));

        return shopRevenue.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    Shop shop = shopMapper.selectById(entry.getKey());
                    if (shop != null) {
                        item.put("shopId", shop.getId());
                        item.put("shopName", shop.getName());
                        item.put("revenue", entry.getValue());
                    }
                    return item;
                })
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 获取订单状态分布
     */
    private Map<String, Object> getOrderStatusDistribution(List<Long> shopIds) {
        Map<String, Object> dist = new HashMap<>();

        if (shopIds.isEmpty()) {
            return dist;
        }

        for (OrderStatus status : OrderStatus.values()) {
            Long count = orderMapper.selectCount(
                    new LambdaQueryWrapper<Order>()
                            .in(Order::getShopId, shopIds)
                            .eq(Order::getStatus, status.getCode()));
            dist.put(status.name().toLowerCase(), count);
        }

        return dist;
    }

    /**
     * 获取账单统计
     */
    private Map<String, Object> getBillStatistics(List<Long> shopIds) {
        Map<String, Object> stats = new HashMap<>();

        if (shopIds.isEmpty()) {
            stats.put("totalBills", 0);
            stats.put("pendingBills", 0);
            stats.put("paidBills", 0);
            stats.put("totalBillAmount", BigDecimal.ZERO);
            stats.put("paidBillAmount", BigDecimal.ZERO);
            return stats;
        }

        // 账单总数
        Long totalBills = billMapper.selectCount(
                new LambdaQueryWrapper<Bill>().in(Bill::getShopId, shopIds));
        stats.put("totalBills", totalBills);

        // 待支付账单数
        Long pendingBills = billMapper.selectCount(
                new LambdaQueryWrapper<Bill>()
                        .in(Bill::getShopId, shopIds)
                        .eq(Bill::getStatus, BillStatus.PENDING_PAYMENT.getCode()));
        stats.put("pendingBills", pendingBills);

        // 已支付账单数
        Long paidBills = billMapper.selectCount(
                new LambdaQueryWrapper<Bill>()
                        .in(Bill::getShopId, shopIds)
                        .eq(Bill::getStatus, BillStatus.PAID.getCode()));
        stats.put("paidBills", paidBills);

        // 总金额
        List<Bill> allBills = billMapper.selectList(
                new LambdaQueryWrapper<Bill>().in(Bill::getShopId, shopIds));
        BigDecimal totalAmount = allBills.stream()
                .map(Bill::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalBillAmount", totalAmount);

        // 已支付金额
        List<Bill> paidBillList = billMapper.selectList(
                new LambdaQueryWrapper<Bill>()
                        .in(Bill::getShopId, shopIds)
                        .eq(Bill::getStatus, BillStatus.PAID.getCode()));
        BigDecimal paidAmount = paidBillList.stream()
                .map(Bill::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("paidBillAmount", paidAmount);

        return stats;
    }

    // ==================== 店铺管理 ====================

    /**
     * 获取店铺列表
     */
    @Operation(summary = "获取店铺列表", description = "分页获取店铺列表")
    @GetMapping("/shops")
    public Result<PageResult<ShopResponse>> getShopList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        Long branchManagerId = requireCurrentBranchManagerId();

        Page<Shop> pageParam = new Page<>(page, size);
        Page<Shop> result = shopMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Shop>()
                        .eq(Shop::getBranchManagerId, branchManagerId)
                        .orderByDesc(Shop::getCreatedAt));

        return Result.success(PageResult.of(result.convert(this::convertToShopResponse)));
    }

    /**
     * 获取店铺详情
     */
    @Operation(summary = "获取店铺详情", description = "根据ID获取店铺详情")
    @GetMapping("/shops/{id}")
    public Result<ShopResponse> getShopDetail(@PathVariable Long id) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Shop shop = shopMapper.selectById(id);
        if (shop == null || !shop.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        ShopResponse response = convertToShopResponse(shop);

        // 统计订单数量
        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getShopId, id));
        response.setOrderCount(orderCount.intValue());

        return Result.success(response);
    }

    /**
     * 创建店铺
     */
    @Operation(summary = "创建店铺", description = "创建新店铺")
    @PostMapping("/shops")
    public Result<Void> createShop(@RequestBody ShopCreateRequest request) {
        Long branchManagerId = requireCurrentBranchManagerId();

        // 检查手机号是否已存在
        Long count = shopMapper.selectCount(
                new LambdaQueryWrapper<Shop>().eq(Shop::getPhone, request.getPhone()));
        if (count > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }

        Shop shop = new Shop();
        shop.setBranchManagerId(branchManagerId);
        shop.setName(request.getName());
        shop.setAddress(request.getAddress());
        shop.setPhone(request.getPhone());
        shop.setPassword(passwordEncoder.encode(request.getPassword()));
        shop.setShowPrice(1);
        shop.setPriceType(1);
        shop.setStatus(1);

        shopMapper.insert(shop);
        return Result.success();
    }

    /**
     * 更新店铺
     */
    @Operation(summary = "更新店铺", description = "更新店铺信息")
    @PutMapping("/shops/{id}")
    public Result<Void> updateShop(@PathVariable Long id, @RequestBody ShopCreateRequest request) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Shop shop = shopMapper.selectById(id);
        if (shop == null || !shop.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 检查手机号是否被其他店铺使用
        Long count = shopMapper.selectCount(
                new LambdaQueryWrapper<Shop>()
                        .eq(Shop::getPhone, request.getPhone())
                        .ne(Shop::getId, id));
        if (count > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }

        shop.setName(request.getName());
        shop.setAddress(request.getAddress());
        shop.setPhone(request.getPhone());

        if (request.getShowPrice() != null) {
            shop.setShowPrice(request.getShowPrice());
        }
        if (request.getPriceType() != null) {
            shop.setPriceType(request.getPriceType());
        }
        if (request.getStatus() != null) {
            shop.setStatus(request.getStatus());
        }

        shopMapper.updateById(shop);
        return Result.success();
    }

    /**
     * 删除店铺
     */
    @Operation(summary = "删除店铺", description = "删除店铺")
    @DeleteMapping("/shops/{id}")
    public Result<Void> deleteShop(@PathVariable Long id) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Shop shop = shopMapper.selectById(id);
        if (shop == null || !shop.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        shopMapper.deleteById(id);
        return Result.success();
    }

    // ==================== 司机管理 ====================

    /**
     * 获取司机列表
     */
    @Operation(summary = "获取司机列表", description = "分页获取司机列表")
    @GetMapping("/drivers")
    public Result<PageResult<DriverResponse>> getDriverList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        Long branchManagerId = requireCurrentBranchManagerId();

        Page<Driver> pageParam = new Page<>(page, size);
        Page<Driver> result = driverMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getBranchManagerId, branchManagerId)
                        .orderByDesc(Driver::getCreatedAt));

        return Result.success(PageResult.of(result.convert(this::convertToDriverResponse)));
    }

    /**
     * 获取司机详情
     */
    @Operation(summary = "获取司机详情", description = "根据ID获取司机详情")
    @GetMapping("/drivers/{id}")
    public Result<DriverResponse> getDriverDetail(@PathVariable Long id) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Driver driver = driverMapper.selectById(id);
        if (driver == null || !driver.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        return Result.success(convertToDriverResponse(driver));
    }

    /**
     * 创建司机
     */
    @Operation(summary = "创建司机", description = "创建新司机")
    @PostMapping("/drivers")
    public Result<Void> createDriver(@RequestBody DriverCreateRequest request) {
        Long branchManagerId = requireCurrentBranchManagerId();

        // 检查手机号是否已存在
        Long count = driverMapper.selectCount(
                new LambdaQueryWrapper<Driver>().eq(Driver::getPhone, request.getPhone()));
        if (count > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }

        Driver driver = new Driver();
        driver.setBranchManagerId(branchManagerId);
        driver.setName(request.getName());
        driver.setPhone(request.getPhone());
        driver.setPassword(passwordEncoder.encode(request.getPassword()));
        driver.setStatus(1);

        driverMapper.insert(driver);
        return Result.success();
    }

    /**
     * 更新司机
     */
    @Operation(summary = "更新司机", description = "更新司机信息")
    @PutMapping("/drivers/{id}")
    public Result<Void> updateDriver(@PathVariable Long id, @RequestBody DriverCreateRequest request) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Driver driver = driverMapper.selectById(id);
        if (driver == null || !driver.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 检查手机号是否被其他司机使用
        Long count = driverMapper.selectCount(
                new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getPhone, request.getPhone())
                        .ne(Driver::getId, id));
        if (count > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }

        driver.setName(request.getName());
        driver.setPhone(request.getPhone());

        if (request.getStatus() != null) {
            driver.setStatus(request.getStatus());
        }

        driverMapper.updateById(driver);
        return Result.success();
    }

    /**
     * 删除司机
     */
    @Operation(summary = "删除司机", description = "删除司机")
    @DeleteMapping("/drivers/{id}")
    public Result<Void> deleteDriver(@PathVariable Long id) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Driver driver = driverMapper.selectById(id);
        if (driver == null || !driver.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        driverMapper.deleteById(id);
        return Result.success();
    }

    // ==================== 商品管理 ====================

    /**
     * 获取商品列表
     */
    @Operation(summary = "获取商品列表", description = "分页获取商品列表")
    @GetMapping("/products")
    public Result<PageResult<ProductResponse>> getProductList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category) {

        Long branchManagerId = requireCurrentBranchManagerId();

        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getBranchManagerId, branchManagerId);

        if (category != null && !category.isEmpty()) {
            wrapper.eq(Product::getCategory, category);
        }

        wrapper.orderByDesc(Product::getCreatedAt);

        Page<Product> result = productMapper.selectPage(pageParam, wrapper);

        return Result.success(PageResult.of(result.convert(this::convertToProductResponse)));
    }

    /**
     * 获取商品详情
     */
    @Operation(summary = "获取商品详情", description = "根据ID获取商品详情")
    @GetMapping("/products/{id}")
    public Result<ProductResponse> getProductDetail(@PathVariable Long id) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Product product = productMapper.selectById(id);
        if (product == null || !product.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        return Result.success(convertToProductResponse(product));
    }

    /**
     * 创建商品
     */
    @Operation(summary = "创建商品", description = "创建新商品")
    @PostMapping("/products")
    public Result<Void> createProduct(@RequestBody ProductCreateRequest request) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Product product = new Product();
        product.setBranchManagerId(branchManagerId);
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setOldPrice(request.getOldPrice());
        product.setNewPrice(request.getNewPrice());
        product.setUnit(request.getUnit());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setStatus(1);

        productMapper.insert(product);
        return Result.success();
    }

    /**
     * 更新商品
     */
    @Operation(summary = "更新商品", description = "更新商品信息")
    @PutMapping("/products/{id}")
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody ProductCreateRequest request) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Product product = productMapper.selectById(id);
        if (product == null || !product.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setOldPrice(request.getOldPrice());
        product.setNewPrice(request.getNewPrice());
        product.setUnit(request.getUnit());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());

        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        productMapper.updateById(product);
        return Result.success();
    }

    /**
     * 删除商品
     */
    @Operation(summary = "删除商品", description = "删除商品")
    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Product product = productMapper.selectById(id);
        if (product == null || !product.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        productMapper.deleteById(id);
        return Result.success();
    }

    // ==================== 订单管理 ====================

    /**
     * 获取订单列表
     */
    @Operation(summary = "获取订单列表", description = "分页获取订单列表")
    @GetMapping("/orders")
    public Result<PageResult<OrderResponse>> getOrderList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {

        Long branchManagerId = requireCurrentBranchManagerId();

        // 获取分管理下所有店铺ID
        List<Long> shopIds = shopMapper.selectList(
                new LambdaQueryWrapper<Shop>().eq(Shop::getBranchManagerId, branchManagerId))
                .stream().map(Shop::getId).collect(Collectors.toList());

        if (shopIds.isEmpty()) {
            return Result.success(PageResult.of(new Page<>()));
        }

        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .in(Order::getShopId, shopIds);

        // 状态筛选
        if (status != null && !status.isEmpty()) {
            Integer statusCode = getOrderStatusCode(status);
            if (statusCode != null) {
                wrapper.eq(Order::getStatus, statusCode);
            }
        }

        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> result = orderMapper.selectPage(pageParam, wrapper);

        return Result.success(PageResult.of(result.convert(this::convertToOrderResponse)));
    }

    /**
     * 获取订单详情
     */
    @Operation(summary = "获取订单详情", description = "根据ID获取订单详情")
    @GetMapping("/orders/{id}")
    public Result<OrderResponse> getOrderDetail(@PathVariable Long id) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 验证订单是否属于该分管理
        Shop shop = shopMapper.selectById(order.getShopId());
        if (shop == null || !shop.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        return Result.success(convertToOrderResponse(order));
    }

    /**
     * 分配司机
     */
    @Operation(summary = "分配司机", description = "为订单分配司机")
    @PostMapping("/orders/{id}/assign")
    public Result<Void> assignDriver(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long branchManagerId = requireCurrentBranchManagerId();
        Long driverId = request.get("driverId");

        if (driverId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "司机ID不能为空");
        }

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 验证订单是否属于该分管理
        Shop shop = shopMapper.selectById(order.getShopId());
        if (shop == null || !shop.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 验证司机是否属于该分管理
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null || !driver.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "司机不存在或不属于您");
        }

        // 验证订单状态
        if (order.getStatus() != OrderStatus.PENDING_ASSIGNMENT.getCode()) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不允许分配司机");
        }

        order.setDriverId(driverId);
        order.setStatus(OrderStatus.PENDING_PICKUP.getCode());
        orderMapper.updateById(order);

        return Result.success();
    }

    /**
     * 重新分配司机
     */
    @Operation(summary = "重新分配司机", description = "重新为订单分配司机")
    @PutMapping("/orders/{id}/reassign")
    public Result<Void> reassignDriver(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long branchManagerId = requireCurrentBranchManagerId();
        Long driverId = request.get("driverId");

        if (driverId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "司机ID不能为空");
        }

        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 验证订单是否属于该分管理
        Shop shop = shopMapper.selectById(order.getShopId());
        if (shop == null || !shop.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 验证司机是否属于该分管理
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null || !driver.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "司机不存在或不属于您");
        }

        order.setDriverId(driverId);
        orderMapper.updateById(order);

        return Result.success();
    }

    // ==================== 转换方法 ====================

    private ShopResponse convertToShopResponse(Shop shop) {
        ShopResponse response = new ShopResponse();
        BeanUtils.copyProperties(shop, response);
        return response;
    }

    private DriverResponse convertToDriverResponse(Driver driver) {
        DriverResponse response = new DriverResponse();
        BeanUtils.copyProperties(driver, response);
        return response;
    }

    private ProductResponse convertToProductResponse(Product product) {
        ProductResponse response = new ProductResponse();
        BeanUtils.copyProperties(product, response);
        return response;
    }

    private OrderResponse convertToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        BeanUtils.copyProperties(order, response);

        // 设置状态描述
        OrderStatus orderStatus = OrderStatus.fromCode(order.getStatus());
        response.setStatusDesc(orderStatus.getDesc());

        // 获取店铺名称
        Shop shop = shopMapper.selectById(order.getShopId());
        if (shop != null) {
            response.setShopName(shop.getName());
        }

        // 获取司机名称
        if (order.getDriverId() != null) {
            Driver driver = driverMapper.selectById(order.getDriverId());
            if (driver != null) {
                response.setDriverName(driver.getName());
            }
        }

        // 获取送达照片
        if (order.getStatus() == OrderStatus.COMPLETED.getCode()) {
            DeliveryOrder deliveryOrder = deliveryOrderMapper.selectOne(
                    new LambdaQueryWrapper<DeliveryOrder>().eq(DeliveryOrder::getOrderId, order.getId()));
            if (deliveryOrder != null) {
                DeliveryList deliveryList = deliveryListMapper.selectById(deliveryOrder.getDeliveryListId());
                if (deliveryList != null) {
                    response.setDeliveryPhoto(deliveryList.getDeliveryPhoto());
                }
            }
        }

        // 获取订单商品列表
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        List<OrderResponse.OrderItemResponse> itemResponses = orderItems.stream().map(item -> {
            OrderResponse.OrderItemResponse itemResponse = new OrderResponse.OrderItemResponse();
            BeanUtils.copyProperties(item, itemResponse);

            // 获取商品单位
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                itemResponse.setUnit(product.getUnit());
            }

            // 计算小计
            if (item.getQuantity() != null && item.getUnitPrice() != null) {
                itemResponse.setSubtotal(item.getQuantity().multiply(item.getUnitPrice()));
            }

            return itemResponse;
        }).collect(Collectors.toList());

        response.setItems(itemResponses);

        return response;
    }

    private Integer getOrderStatusCode(String status) {
        switch (status) {
            case "PENDING":
                return OrderStatus.PENDING_PAYMENT.getCode();
            case "PENDING_ASSIGN":
                return OrderStatus.PENDING_ASSIGNMENT.getCode();
            case "PENDING_PICK":
                return OrderStatus.PENDING_PICKUP.getCode();
            case "PENDING_DELIVER":
                return OrderStatus.PENDING_DELIVERY.getCode();
            case "COMPLETED":
                return OrderStatus.COMPLETED.getCode();
            default:
                return null;
        }
    }

    // ==================== 商品图片审核 ====================

    /**
     * 获取待审核的商品图片请求列表
     */
    @Operation(summary = "获取待审核的商品图片请求", description = "获取分管理下司机提交的待审核商品图片请求")
    @GetMapping("/product-image-requests")
    public Result<List<ProductImageRequestResponse>> getProductImageRequests() {
        Long branchManagerId = requireCurrentBranchManagerId();

        List<ProductImageRequest> requests = productImageRequestMapper.selectList(
                new LambdaQueryWrapper<ProductImageRequest>()
                        .eq(ProductImageRequest::getBranchManagerId, branchManagerId)
                        .orderByDesc(ProductImageRequest::getCreatedAt));

        List<ProductImageRequestResponse> responses = requests.stream()
                .map(this::convertToProductImageRequestResponse)
                .collect(Collectors.toList());

        return Result.success(responses);
    }

    /**
     * 审核商品图片请求
     */
    @Operation(summary = "审核商品图片请求", description = "分管理审核司机提交的商品图片请求")
    @PostMapping("/product-image-request/review")
    public Result<Void> reviewProductImageRequest(@Valid @RequestBody ProductImageReviewDto reviewDto) {
        Long branchManagerId = requireCurrentBranchManagerId();

        ProductImageRequest request = productImageRequestMapper.selectById(reviewDto.getRequestId());
        if (request == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 验证是否属于当前分管理
        if (!request.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 验证是否已审核
        if (request.getStatus() != 0) {
            throw new BusinessException("该请求已审核");
        }

        // 更新审核状态
        if (reviewDto.getApproved()) {
            request.setStatus(1); // 已通过

            // 如果有商品ID，更新商品图片
            if (request.getProductId() != null) {
                Product product = productMapper.selectById(request.getProductId());
                if (product != null) {
                    product.setImageUrl(request.getImageUrl());
                    productMapper.updateById(product);
                }
            }
        } else {
            request.setStatus(2); // 已拒绝
            request.setRejectReason(reviewDto.getRejectReason());
        }

        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(branchManagerId);
        productImageRequestMapper.updateById(request);

        return Result.success();
    }

    /**
     * 转换为响应DTO
     */
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

    // ==================== 美团商品搜索 ====================

    /**
     * 通过条形码搜索商品
     */
    @Operation(summary = "扫码搜索商品", description = "通过条形码在美团商品库搜索商品")
    @GetMapping("/product/search-by-code")
    public Result<List<MeituanProductResponse>> searchProductByCode(@RequestParam String upcCode) {
        List<MeituanProductResponse> products = meituanService.searchByUpcCode(upcCode);
        return Result.success(products);
    }

    /**
     * 通过名称搜索商品
     */
    @Operation(summary = "搜索商品", description = "通过商品名称在美团商品库搜索商品")
    @GetMapping("/product/search-by-name")
    public Result<List<MeituanProductResponse>> searchProductByName(@RequestParam String productName) {
        List<MeituanProductResponse> products = meituanService.searchByName(productName);
        return Result.success(products);
    }

    // ==================== 门店商品订货量统计 ====================

    /**
     * 获取各门店商品订货量统计
     */
    @Operation(summary = "获取门店商品订货量统计", description = "获取各门店各商品的订货量总和和实际完成量总和")
    @GetMapping("/shop-product-statistics")
    public Result<List<ShopProductStatisticsResponse>> getShopProductStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Long branchManagerId = requireCurrentBranchManagerId();

        // 解析日期，默认近三个月
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusMonths(3);

        // 获取该分管理下所有店铺
        List<Shop> shops = shopMapper.selectList(
                new LambdaQueryWrapper<Shop>().eq(Shop::getBranchManagerId, branchManagerId));

        List<ShopProductStatisticsResponse> result = new ArrayList<>();

        for (Shop shop : shops) {
            ShopProductStatisticsResponse shopStats = new ShopProductStatisticsResponse();
            shopStats.setShopId(shop.getId());
            shopStats.setShopName(shop.getName());

            // 获取该店铺的所有订单（包括已完成和进行中的）
            List<Order> allOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getShopId, shop.getId())
                            .ge(Order::getCreatedAt, start.atStartOfDay())
                            .le(Order::getCreatedAt, end.plusDays(1).atStartOfDay()));

            // 获取该店铺已完成的订单
            List<Order> completedOrders = allOrders.stream()
                    .filter(o -> OrderStatus.COMPLETED.getCode().equals(o.getStatus()))
                    .collect(Collectors.toList());

            // 统计商品订货量
            Map<Long, ShopProductStatisticsResponse.ProductStatistics> productStatsMap = new HashMap<>();

            // 统计所有订单的商品订货量
            for (Order order : allOrders) {
                List<OrderItem> orderItems = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

                for (OrderItem item : orderItems) {
                    Long productId = item.getProductId();
                    ShopProductStatisticsResponse.ProductStatistics stats = productStatsMap.get(productId);

                    if (stats == null) {
                        stats = new ShopProductStatisticsResponse.ProductStatistics();
                        stats.setProductId(productId);
                        stats.setProductName(item.getProductName());
                        Product product = productMapper.selectById(productId);
                        if (product != null) {
                            stats.setUnit(product.getUnit());
                        }
                        stats.setOrderQuantity(BigDecimal.ZERO);
                        stats.setCompletedQuantity(BigDecimal.ZERO);
                        productStatsMap.put(productId, stats);
                    }

                    // 累加订货量
                    stats.setOrderQuantity(stats.getOrderQuantity().add(item.getQuantity()));
                }
            }

            // 统计已完成订单的商品完成量
            for (Order order : completedOrders) {
                List<OrderItem> orderItems = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

                for (OrderItem item : orderItems) {
                    Long productId = item.getProductId();
                    ShopProductStatisticsResponse.ProductStatistics stats = productStatsMap.get(productId);

                    if (stats != null) {
                        // 累加完成量
                        stats.setCompletedQuantity(stats.getCompletedQuantity().add(item.getQuantity()));
                    }
                }
            }

            // 转换为列表
            List<ShopProductStatisticsResponse.ProductStatistics> products = new ArrayList<>(productStatsMap.values());
            // 按商品名称排序
            products.sort((a, b) -> {
                if (a.getProductName() == null)
                    return 1;
                if (b.getProductName() == null)
                    return -1;
                return a.getProductName().compareTo(b.getProductName());
            });

            shopStats.setProducts(products);
            result.add(shopStats);
        }

        return Result.success(result);
    }

    // ==================== 注册申请管理 ====================

    /**
     * 获取待审核的注册申请列表
     */
    @Operation(summary = "获取注册申请列表", description = "获取待审核的店铺和司机注册申请")
    @GetMapping("/registration-requests")
    public Result<Map<String, Object>> getRegistrationRequests() {
        Long branchManagerId = requireCurrentBranchManagerId();

        Map<String, Object> result = new HashMap<>();

        // 获取待审核的店铺（status=0）
        List<Shop> pendingShops = shopMapper.selectList(
                new LambdaQueryWrapper<Shop>()
                        .eq(Shop::getBranchManagerId, branchManagerId)
                        .eq(Shop::getStatus, 0)
                        .orderByDesc(Shop::getCreatedAt));

        List<Map<String, Object>> shopRequests = pendingShops.stream().map(shop -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", shop.getId());
            item.put("type", "SHOP");
            item.put("name", shop.getName());
            item.put("phone", shop.getPhone());
            item.put("address", shop.getAddress());
            item.put("createdAt", shop.getCreatedAt());
            return item;
        }).collect(Collectors.toList());

        // 获取待审核的司机（status=0）
        List<Driver> pendingDrivers = driverMapper.selectList(
                new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getBranchManagerId, branchManagerId)
                        .eq(Driver::getStatus, 0)
                        .orderByDesc(Driver::getCreatedAt));

        List<Map<String, Object>> driverRequests = pendingDrivers.stream().map(driver -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", driver.getId());
            item.put("type", "DRIVER");
            item.put("name", driver.getName());
            item.put("phone", driver.getPhone());
            item.put("createdAt", driver.getCreatedAt());
            return item;
        }).collect(Collectors.toList());

        result.put("shops", shopRequests);
        result.put("drivers", driverRequests);
        result.put("totalPending", shopRequests.size() + driverRequests.size());

        return Result.success(result);
    }

    /**
     * 审核店铺注册申请
     */
    @Operation(summary = "审核店铺注册", description = "通过或拒绝店铺注册申请")
    @PostMapping("/shop/{id}/review")
    public Result<Void> reviewShopRegistration(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {

        Long branchManagerId = requireCurrentBranchManagerId();
        Boolean approved = (Boolean) params.get("approved");

        if (approved == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审核结果不能为空");
        }

        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "店铺不存在");
        }

        if (!shop.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        if (shop.getStatus() != 0) {
            throw new BusinessException("该申请已审核");
        }

        if (approved) {
            shop.setStatus(1); // 审核通过
            shopMapper.updateById(shop);
            log.info("店铺注册审核通过: {}", shop.getPhone());
        } else {
            // 审核拒绝，删除记录
            shopMapper.deleteById(id);
            log.info("店铺注册审核拒绝: {}", shop.getPhone());
        }

        return Result.success();
    }

    /**
     * 审核司机注册申请
     */
    @Operation(summary = "审核司机注册", description = "通过或拒绝司机注册申请")
    @PostMapping("/driver/{id}/review")
    public Result<Void> reviewDriverRegistration(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {

        Long branchManagerId = requireCurrentBranchManagerId();
        Boolean approved = (Boolean) params.get("approved");

        if (approved == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审核结果不能为空");
        }

        Driver driver = driverMapper.selectById(id);
        if (driver == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "司机不存在");
        }

        if (!driver.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        if (driver.getStatus() != 0) {
            throw new BusinessException("该申请已审核");
        }

        if (approved) {
            driver.setStatus(1); // 审核通过
            driverMapper.updateById(driver);
            log.info("司机注册审核通过: {}", driver.getPhone());
        } else {
            // 审核拒绝，删除记录
            driverMapper.deleteById(id);
            log.info("司机注册审核拒绝: {}", driver.getPhone());
        }

        return Result.success();
    }

    // ==================== 个人信息管理 ====================

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "分管理修改自己的密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long branchManagerId = requireCurrentBranchManagerId();

        BranchManager branchManager = branchManagerMapper.selectById(branchManagerId);
        if (branchManager == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(request.getOldPassword(), branchManager.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "原密码错误");
        }

        // 更新密码
        branchManager.setPassword(passwordEncoder.encode(request.getNewPassword()));
        branchManagerMapper.updateById(branchManager);

        log.info("分管理修改密码成功: {}", branchManager.getPhone());
        return Result.success();
    }

    // ==================== 账单统计管理 ====================

    /**
     * 获取今日未生成账单的店铺列表
     */
    @Operation(summary = "获取今日未生成账单店铺", description = "获取今日未生成账单的店铺列表")
    @GetMapping("/bills/ungenerated-shops")
    public Result<List<Map<String, Object>>> getUnGeneratedBillShops() {
        Long branchManagerId = requireCurrentBranchManagerId();
        LocalDate today = LocalDate.now();

        // 获取该分管理下所有正常状态的店铺
        List<Shop> shops = shopMapper.selectList(
                new LambdaQueryWrapper<Shop>()
                        .eq(Shop::getBranchManagerId, branchManagerId)
                        .eq(Shop::getStatus, 1));

        List<Map<String, Object>> result = new ArrayList<>();

        for (Shop shop : shops) {
            // 检查今日是否已生成账单
            Long billCount = billMapper.selectCount(
                    new LambdaQueryWrapper<Bill>()
                            .eq(Bill::getShopId, shop.getId())
                            .eq(Bill::getBillDate, today));

            if (billCount == 0) {
                // 检查今日是否有已完成的订单
                Long orderCount = orderMapper.selectCount(
                        new LambdaQueryWrapper<Order>()
                                .eq(Order::getShopId, shop.getId())
                                .eq(Order::getStatus, OrderStatus.COMPLETED.getCode())
                                .ge(Order::getReceivedAt, today.atStartOfDay())
                                .lt(Order::getReceivedAt, today.plusDays(1).atStartOfDay()));

                if (orderCount > 0) {
                    Map<String, Object> shopInfo = new HashMap<>();
                    shopInfo.put("id", shop.getId());
                    shopInfo.put("name", shop.getName());
                    shopInfo.put("phone", shop.getPhone());
                    shopInfo.put("address", shop.getAddress());
                    shopInfo.put("orderCount", orderCount);
                    result.add(shopInfo);
                }
            }
        }

        return Result.success(result);
    }

    /**
     * 获取历史账单列表（按店铺分组）
     */
    @Operation(summary = "获取历史账单列表", description = "获取历史账单列表，按店铺分组")
    @GetMapping("/bills/history")
    public Result<List<Map<String, Object>>> getBillHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Long branchManagerId = requireCurrentBranchManagerId();

        // 获取该分管理下所有店铺ID
        List<Shop> shops = shopMapper.selectList(
                new LambdaQueryWrapper<Shop>()
                        .eq(Shop::getBranchManagerId, branchManagerId)
                        .eq(Shop::getStatus, 1));

        List<Long> shopIds = shops.stream().map(Shop::getId).collect(Collectors.toList());

        if (shopIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 构建查询条件
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(3);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        LambdaQueryWrapper<Bill> queryWrapper = new LambdaQueryWrapper<Bill>()
                .in(Bill::getShopId, shopIds)
                .ge(Bill::getBillDate, start)
                .le(Bill::getBillDate, end)
                .orderByDesc(Bill::getBillDate);

        List<Bill> bills = billMapper.selectList(queryWrapper);

        // 按店铺分组
        Map<Long, List<Bill>> billsByShop = bills.stream()
                .collect(Collectors.groupingBy(Bill::getShopId));

        List<Map<String, Object>> result = new ArrayList<>();

        for (Shop shop : shops) {
            List<Bill> shopBills = billsByShop.get(shop.getId());
            if (shopBills != null && !shopBills.isEmpty()) {
                Map<String, Object> shopBillInfo = new HashMap<>();
                shopBillInfo.put("shopId", shop.getId());
                shopBillInfo.put("shopName", shop.getName());
                shopBillInfo.put("billCount", shopBills.size());
                shopBillInfo.put("totalAmount", shopBills.stream()
                        .map(Bill::getTotalAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                shopBillInfo.put("unpaidCount", shopBills.stream()
                        .filter(b -> b.getStatus() == 0).count());
                shopBillInfo.put("unpaidAmount", shopBills.stream()
                        .filter(b -> b.getStatus() == 0)
                        .map(Bill::getTotalAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                result.add(shopBillInfo);
            }
        }

        return Result.success(result);
    }

    /**
     * 获取店铺的历史账单详情列表
     */
    @Operation(summary = "获取店铺历史账单", description = "获取指定店铺的历史账单列表")
    @GetMapping("/bills/shop/{shopId}")
    public Result<List<Map<String, Object>>> getShopBillHistory(@PathVariable Long shopId) {
        Long branchManagerId = requireCurrentBranchManagerId();

        // 验证店铺归属
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null || !shop.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "店铺不存在");
        }

        List<Bill> bills = billMapper.selectList(
                new LambdaQueryWrapper<Bill>()
                        .eq(Bill::getShopId, shopId)
                        .orderByDesc(Bill::getBillDate));

        List<Map<String, Object>> result = new ArrayList<>();

        for (Bill bill : bills) {
            Map<String, Object> billInfo = new HashMap<>();
            billInfo.put("id", bill.getId());
            billInfo.put("billNo", bill.getBillNo());
            billInfo.put("billDate", bill.getBillDate());
            billInfo.put("totalAmount", bill.getTotalAmount());
            billInfo.put("status", bill.getStatus());
            billInfo.put("statusText", bill.getStatus() == 0 ? "待支付" : "已支付");
            billInfo.put("sendStatus", bill.getSendStatus());
            billInfo.put("sentAt", bill.getSentAt());
            billInfo.put("paidAt", bill.getPaidAt());
            billInfo.put("createdAt", bill.getCreatedAt());

            // 获取账单关联的订单数量
            Long orderCount = billOrderMapper.selectCount(
                    new LambdaQueryWrapper<BillOrder>().eq(BillOrder::getBillId, bill.getId()));
            billInfo.put("orderCount", orderCount);

            result.add(billInfo);
        }

        return Result.success(result);
    }

    /**
     * 获取账单详情（包含订单列表）
     */
    @Operation(summary = "获取账单详情", description = "获取账单详情及关联订单")
    @GetMapping("/bills/{billId}")
    public Result<Map<String, Object>> getBillDetail(@PathVariable Long billId) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Bill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }

        // 验证店铺归属
        Shop shop = shopMapper.selectById(bill.getShopId());
        if (shop == null || !shop.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", bill.getId());
        result.put("billNo", bill.getBillNo());
        result.put("billDate", bill.getBillDate());
        result.put("totalAmount", bill.getTotalAmount());
        result.put("status", bill.getStatus());
        result.put("statusText", bill.getStatus() == 0 ? "待支付" : "已支付");
        result.put("sendStatus", bill.getSendStatus());
        result.put("sentAt", bill.getSentAt());
        result.put("paidAt", bill.getPaidAt());
        result.put("createdAt", bill.getCreatedAt());
        result.put("shopName", shop.getName());

        // 获取关联订单
        List<BillOrder> billOrders = billOrderMapper.selectList(
                new LambdaQueryWrapper<BillOrder>().eq(BillOrder::getBillId, billId));

        List<Map<String, Object>> orders = new ArrayList<>();
        for (BillOrder billOrder : billOrders) {
            Order order = orderMapper.selectById(billOrder.getOrderId());
            if (order != null) {
                Map<String, Object> orderInfo = new HashMap<>();
                orderInfo.put("id", order.getId());
                orderInfo.put("orderNo", order.getOrderNo());
                orderInfo.put("totalAmount", order.getTotalAmount());
                orderInfo.put("deliveryDate", order.getDeliveryDate());
                orderInfo.put("receivedAt", order.getReceivedAt());
                orderInfo.put("status", order.getStatus());

                // 获取订单商品明细
                List<OrderItem> items = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

                List<Map<String, Object>> itemDetails = new ArrayList<>();
                for (OrderItem item : items) {
                    Product product = productMapper.selectById(item.getProductId());
                    Map<String, Object> itemInfo = new HashMap<>();
                    itemInfo.put("productName", product != null ? product.getName() : "未知商品");
                    itemInfo.put("quantity", item.getQuantity());
                    itemInfo.put("price", item.getPrice());
                    itemInfo.put("amount", item.getPrice().multiply(new BigDecimal(item.getQuantity())));
                    itemDetails.add(itemInfo);
                }
                orderInfo.put("items", itemDetails);

                orders.add(orderInfo);
            }
        }
        result.put("orders", orders);

        return Result.success(result);
    }

    /**
     * 手动生成账单
     */
    @Operation(summary = "手动生成账单", description = "为指定店铺手动生成今日账单")
    @PostMapping("/bills/generate")
    public Result<Map<String, Object>> generateBill(@RequestBody Map<String, Long> params) {
        Long branchManagerId = requireCurrentBranchManagerId();
        Long shopId = params.get("shopId");

        if (shopId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "店铺ID不能为空");
        }

        // 验证店铺归属
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null || !shop.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "店铺不存在");
        }

        LocalDate today = LocalDate.now();

        // 检查今日是否已生成账单
        Long existCount = billMapper.selectCount(
                new LambdaQueryWrapper<Bill>()
                        .eq(Bill::getShopId, shopId)
                        .eq(Bill::getBillDate, today));

        if (existCount > 0) {
            throw new BusinessException("今日账单已存在");
        }

        // 获取该店铺当日已收货的订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getShopId, shopId)
                        .eq(Order::getStatus, OrderStatus.COMPLETED.getCode())
                        .ge(Order::getReceivedAt, today.atStartOfDay())
                        .lt(Order::getReceivedAt, today.plusDays(1).atStartOfDay()));

        if (orders.isEmpty()) {
            throw new BusinessException("今日无已收货订单，无法生成账单");
        }

        // 计算总金额
        BigDecimal totalAmount = orders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 创建账单
        Bill bill = new Bill();
        bill.setBillNo("BIL" + System.currentTimeMillis());
        bill.setShopId(shopId);
        bill.setBillDate(today);
        bill.setTotalAmount(totalAmount);
        bill.setStatus(0); // 待支付
        bill.setSendStatus(0); // 未发送

        billMapper.insert(bill);

        // 关联订单
        for (Order order : orders) {
            BillOrder billOrder = new BillOrder();
            billOrder.setBillId(bill.getId());
            billOrder.setOrderId(order.getId());
            billOrderMapper.insert(billOrder);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("billId", bill.getId());
        result.put("billNo", bill.getBillNo());
        result.put("totalAmount", totalAmount);
        result.put("orderCount", orders.size());

        log.info("手动生成账单成功，shopId: {}, billId: {}", shopId, bill.getId());

        return Result.success(result);
    }

    /**
     * 发送账单通知
     */
    @Operation(summary = "发送账单通知", description = "发送账单通知给店铺")
    @PostMapping("/bills/{billId}/send")
    public Result<Void> sendBillNotice(@PathVariable Long billId) {
        Long branchManagerId = requireCurrentBranchManagerId();

        Bill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }

        // 验证店铺归属
        Shop shop = shopMapper.selectById(bill.getShopId());
        if (shop == null || !shop.getBranchManagerId().equals(branchManagerId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账单不存在");
        }

        if (bill.getSendStatus() != null && bill.getSendStatus() == 1) {
            throw new BusinessException("账单已发送");
        }

        // 发送微信订阅消息
        wechatMessageService.sendBillCreatedNotice(shop.getId(), bill.getId(),
                bill.getTotalAmount().toString());

        // 更新发送状态
        bill.setSendStatus(1);
        bill.setSentAt(LocalDateTime.now());
        billMapper.updateById(bill);

        log.info("发送账单通知成功，billId: {}, shopId: {}", billId, shop.getId());

        return Result.success();
    }
}
