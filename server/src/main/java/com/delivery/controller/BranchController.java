package com.delivery.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delivery.common.PageResult;
import com.delivery.common.Result;
import com.delivery.common.ResultCode;
import com.delivery.dto.request.ShopCreateRequest;
import com.delivery.dto.request.DriverCreateRequest;
import com.delivery.dto.request.ProductCreateRequest;
import com.delivery.dto.response.OrderResponse;
import com.delivery.dto.response.ShopResponse;
import com.delivery.dto.response.DriverResponse;
import com.delivery.dto.response.ProductResponse;
import com.delivery.entity.*;
import com.delivery.enums.BillStatus;
import com.delivery.enums.OrderStatus;
import com.delivery.exception.BusinessException;
import com.delivery.mapper.*;
import com.delivery.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分管理端控制器
 */
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
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取统计数据
     */
    @Operation(summary = "获取统计数据", description = "获取分管理首页统计数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Long branchManagerId = UserContext.getCurrentUserId();

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

        Long branchManagerId = UserContext.getCurrentUserId();

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

        return Result.success(result);
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

        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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

        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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

        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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

        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();

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
        Long branchManagerId = UserContext.getCurrentUserId();
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
        Long branchManagerId = UserContext.getCurrentUserId();
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
}
