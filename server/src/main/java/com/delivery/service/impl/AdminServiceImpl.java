package com.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delivery.common.PageResult;
import com.delivery.common.ResultCode;
import com.delivery.dto.request.BranchManagerCreateRequest;
import com.delivery.dto.response.BranchManagerResponse;
import com.delivery.entity.*;
import com.delivery.enums.BillStatus;
import com.delivery.enums.OrderStatus;
import com.delivery.exception.BusinessException;
import com.delivery.mapper.*;
import com.delivery.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 总管理服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final BranchManagerMapper branchManagerMapper;
    private final ShopMapper shopMapper;
    private final DriverMapper driverMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final BillMapper billMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void createBranchManager(BranchManagerCreateRequest request) {
        // 检查手机号是否已存在
        Long count = branchManagerMapper.selectCount(
                new LambdaQueryWrapper<BranchManager>().eq(BranchManager::getPhone, request.getPhone()));
        if (count > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }

        BranchManager branchManager = new BranchManager();
        branchManager.setBrandName(request.getBrandName());
        branchManager.setPhone(request.getPhone());
        branchManager.setPassword(passwordEncoder.encode(request.getPassword()));
        branchManager.setStatus(1);

        branchManagerMapper.insert(branchManager);
    }

    @Override
    public PageResult<BranchManagerResponse> getBranchManagerList(Integer page, Integer size) {
        Page<BranchManager> pageParam = new Page<>(page, size);
        Page<BranchManager> result = branchManagerMapper.selectPage(pageParam,
                new LambdaQueryWrapper<BranchManager>().orderByDesc(BranchManager::getCreatedAt));

        return PageResult.of(result.convert(this::convertToResponse));
    }

    @Override
    public BranchManagerResponse getBranchManagerDetail(Long id) {
        BranchManager branchManager = branchManagerMapper.selectById(id);
        if (branchManager == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        BranchManagerResponse response = convertToResponse(branchManager);

        // 统计店铺数量
        Long shopCount = shopMapper.selectCount(
                new LambdaQueryWrapper<Shop>().eq(Shop::getBranchManagerId, id));
        response.setShopCount(shopCount.intValue());

        // 统计司机数量
        Long driverCount = driverMapper.selectCount(
                new LambdaQueryWrapper<Driver>().eq(Driver::getBranchManagerId, id));
        response.setDriverCount(driverCount.intValue());

        // 统计商品数量
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getBranchManagerId, id));
        response.setProductCount(productCount.intValue());

        return response;
    }

    @Override
    public void toggleBranchManagerStatus(Long id) {
        BranchManager branchManager = branchManagerMapper.selectById(id);
        if (branchManager == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        branchManager.setStatus(branchManager.getStatus() == 1 ? 0 : 1);
        branchManagerMapper.updateById(branchManager);

        // 同步更新下属店铺和司机的状态
        Shop shop = new Shop();
        shop.setStatus(branchManager.getStatus());
        shopMapper.update(shop, new LambdaQueryWrapper<Shop>().eq(Shop::getBranchManagerId, id));

        Driver driver = new Driver();
        driver.setStatus(branchManager.getStatus());
        driverMapper.update(driver, new LambdaQueryWrapper<Driver>().eq(Driver::getBranchManagerId, id));
    }

    @Override
    public void updateBranchManager(Long id, BranchManagerCreateRequest request) {
        BranchManager branchManager = branchManagerMapper.selectById(id);
        if (branchManager == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查手机号是否被其他分管理使用
        Long count = branchManagerMapper.selectCount(
                new LambdaQueryWrapper<BranchManager>()
                        .eq(BranchManager::getPhone, request.getPhone())
                        .ne(BranchManager::getId, id));
        if (count > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }

        branchManager.setBrandName(request.getBrandName());
        branchManager.setPhone(request.getPhone());

        // 更新状态（如果提供了）
        if (request.getStatus() != null) {
            branchManager.setStatus(request.getStatus());

            // 同步更新下属店铺和司机的状态
            Shop shop = new Shop();
            shop.setStatus(request.getStatus());
            shopMapper.update(shop, new LambdaQueryWrapper<Shop>().eq(Shop::getBranchManagerId, id));

            Driver driver = new Driver();
            driver.setStatus(request.getStatus());
            driverMapper.update(driver, new LambdaQueryWrapper<Driver>().eq(Driver::getBranchManagerId, id));
        }

        branchManagerMapper.updateById(branchManager);
    }

    @Override
    public void deleteBranchManager(Long id) {
        BranchManager branchManager = branchManagerMapper.selectById(id);
        if (branchManager == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 删除下属店铺
        shopMapper.delete(new LambdaQueryWrapper<Shop>().eq(Shop::getBranchManagerId, id));

        // 删除下属司机
        driverMapper.delete(new LambdaQueryWrapper<Driver>().eq(Driver::getBranchManagerId, id));

        // 删除下属商品
        productMapper.delete(new LambdaQueryWrapper<Product>().eq(Product::getBranchManagerId, id));

        // 删除分管理
        branchManagerMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 统计分管理数量
        Long branchManagerCount = branchManagerMapper.selectCount(null);
        statistics.put("branchManagerCount", branchManagerCount);

        // 统计店铺数量
        Long shopCount = shopMapper.selectCount(null);
        statistics.put("shopCount", shopCount);

        // 统计司机数量
        Long driverCount = driverMapper.selectCount(null);
        statistics.put("driverCount", driverCount);

        // 统计商品数量
        Long productCount = productMapper.selectCount(null);
        statistics.put("productCount", productCount);

        return statistics;
    }

    @Override
    public Map<String, Object> getDataCenter(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();

        // 解析日期，默认近三个月
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusMonths(3);

        // 1. 基础统计数据
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalShops", shopMapper.selectCount(null));
        overview.put("totalDrivers", driverMapper.selectCount(null));
        overview.put("totalProducts", productMapper.selectCount(null));
        overview.put("totalBranchManagers", branchManagerMapper.selectCount(null));

        // 统计订单总数和交易额
        Long totalOrders = orderMapper.selectCount(null);
        overview.put("totalOrders", totalOrders);

        // 统计已完成订单的总交易额
        List<Order> completedOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.COMPLETED.getCode()));
        BigDecimal totalAmount = completedOrders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalAmount", totalAmount);

        result.put("overview", overview);

        // 2. 商品订单量排行榜（按订单数量）
        List<Map<String, Object>> productOrderRank = getProductOrderRank(start, end, 10);
        result.put("productOrderRank", productOrderRank);

        // 3. 商品销量排行榜（按销量）
        List<Map<String, Object>> productSalesRank = getProductSalesRank(start, end, 10);
        result.put("productSalesRank", productSalesRank);

        // 4. 每种商品销量变化趋势
        List<Map<String, Object>> salesTrend = getSalesTrend(start, end);
        result.put("salesTrend", salesTrend);

        // 5. 入账分布（按分管理）
        List<Map<String, Object>> revenueByBranch = getRevenueByBranch(start, end);
        result.put("revenueByBranch", revenueByBranch);

        // 6. 入账分布（按店铺）
        List<Map<String, Object>> revenueByShop = getRevenueByShop(start, end, 10);
        result.put("revenueByShop", revenueByShop);

        // 7. 店铺数量变化趋势
        List<Map<String, Object>> shopTrend = getShopTrend(start, end);
        result.put("shopTrend", shopTrend);

        // 8. 订单状态分布
        Map<String, Object> orderStatusDist = getOrderStatusDistribution();
        result.put("orderStatusDist", orderStatusDist);

        // 9. 账单统计
        Map<String, Object> billStats = getBillStatistics();
        result.put("billStats", billStats);

        return result;
    }

    /**
     * 获取商品订单量排行榜
     */
    private List<Map<String, Object>> getProductOrderRank(LocalDate start, LocalDate end, int limit) {
        // 获取时间范围内的订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getCreatedAt, start.atStartOfDay())
                        .le(Order::getCreatedAt, end.plusDays(1).atStartOfDay()));

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        // 统计每个商品的订单数量
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));

        Map<Long, Long> productOrderCount = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getProductId, Collectors.counting()));

        // 获取商品信息并排序
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
    private List<Map<String, Object>> getProductSalesRank(LocalDate start, LocalDate end, int limit) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getCreatedAt, start.atStartOfDay())
                        .le(Order::getCreatedAt, end.plusDays(1).atStartOfDay()));

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));

        // 统计每个商品的销量
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
     * 获取销量变化趋势
     */
    private List<Map<String, Object>> getSalesTrend(LocalDate start, LocalDate end) {
        List<Map<String, Object>> trend = new ArrayList<>();

        // 获取所有商品
        List<Product> products = productMapper.selectList(null);

        // 按天统计每个商品的销量
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
     * 获取按分管理的入账分布
     */
    private List<Map<String, Object>> getRevenueByBranch(LocalDate start, LocalDate end) {
        List<Map<String, Object>> result = new ArrayList<>();

        List<BranchManager> branchManagers = branchManagerMapper.selectList(null);

        for (BranchManager bm : branchManagers) {
            // 获取该分管理下所有店铺
            List<Long> shopIds = shopMapper.selectList(
                    new LambdaQueryWrapper<Shop>().eq(Shop::getBranchManagerId, bm.getId()))
                    .stream().map(Shop::getId).collect(Collectors.toList());

            if (shopIds.isEmpty())
                continue;

            // 统计这些店铺的已完成订单金额
            List<Order> orders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .in(Order::getShopId, shopIds)
                            .eq(Order::getStatus, OrderStatus.COMPLETED.getCode())
                            .ge(Order::getCreatedAt, start.atStartOfDay())
                            .le(Order::getCreatedAt, end.plusDays(1).atStartOfDay()));

            BigDecimal revenue = orders.stream()
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> item = new HashMap<>();
            item.put("branchManagerId", bm.getId());
            item.put("brandName", bm.getBrandName());
            item.put("revenue", revenue);
            item.put("shopCount", shopIds.size());
            result.add(item);
        }

        // 按收入排序
        result.sort((a, b) -> ((BigDecimal) b.get("revenue")).compareTo((BigDecimal) a.get("revenue")));

        return result;
    }

    /**
     * 获取按店铺的入账分布
     */
    private List<Map<String, Object>> getRevenueByShop(LocalDate start, LocalDate end, int limit) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.COMPLETED.getCode())
                        .ge(Order::getCreatedAt, start.atStartOfDay())
                        .le(Order::getCreatedAt, end.plusDays(1).atStartOfDay()));

        // 按店铺分组统计
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
     * 获取店铺数量变化趋势
     */
    private List<Map<String, Object>> getShopTrend(LocalDate start, LocalDate end) {
        List<Map<String, Object>> trend = new ArrayList<>();

        LocalDate current = start;
        while (!current.isAfter(end)) {
            final LocalDate date = current;

            // 统计截止到该日期的店铺数量
            Long count = shopMapper.selectCount(
                    new LambdaQueryWrapper<Shop>()
                            .le(Shop::getCreatedAt, date.plusDays(1).atStartOfDay()));

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("count", count);
            trend.add(dayData);

            current = current.plusDays(1);
        }

        return trend;
    }

    /**
     * 获取订单状态分布
     */
    private Map<String, Object> getOrderStatusDistribution() {
        Map<String, Object> dist = new HashMap<>();

        for (OrderStatus status : OrderStatus.values()) {
            Long count = orderMapper.selectCount(
                    new LambdaQueryWrapper<Order>().eq(Order::getStatus, status.getCode()));
            dist.put(status.name().toLowerCase(), count);
        }

        return dist;
    }

    /**
     * 获取账单统计
     */
    private Map<String, Object> getBillStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 账单总数
        Long totalBills = billMapper.selectCount(null);
        stats.put("totalBills", totalBills);

        // 待支付账单数
        Long pendingBills = billMapper.selectCount(
                new LambdaQueryWrapper<Bill>().eq(Bill::getStatus, BillStatus.PENDING_PAYMENT.getCode()));
        stats.put("pendingBills", pendingBills);

        // 已支付账单数
        Long paidBills = billMapper.selectCount(
                new LambdaQueryWrapper<Bill>().eq(Bill::getStatus, BillStatus.PAID.getCode()));
        stats.put("paidBills", paidBills);

        // 总金额
        List<Bill> allBills = billMapper.selectList(null);
        BigDecimal totalAmount = allBills.stream()
                .map(Bill::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalBillAmount", totalAmount);

        // 已支付金额
        List<Bill> paidBillList = billMapper.selectList(
                new LambdaQueryWrapper<Bill>().eq(Bill::getStatus, BillStatus.PAID.getCode()));
        BigDecimal paidAmount = paidBillList.stream()
                .map(Bill::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("paidBillAmount", paidAmount);

        return stats;
    }

    private BranchManagerResponse convertToResponse(BranchManager branchManager) {
        BranchManagerResponse response = new BranchManagerResponse();
        response.setId(branchManager.getId());
        response.setBrandName(branchManager.getBrandName());
        response.setPhone(branchManager.getPhone());
        response.setStatus(branchManager.getStatus());
        response.setCreatedAt(branchManager.getCreatedAt());
        return response;
    }
}
