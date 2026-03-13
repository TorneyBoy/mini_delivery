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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
