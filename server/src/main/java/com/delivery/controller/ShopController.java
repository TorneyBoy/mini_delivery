package com.delivery.controller;

import com.delivery.common.PageResult;
import com.delivery.common.Result;
import com.delivery.dto.request.OrderCreateRequest;
import com.delivery.dto.response.BillResponse;
import com.delivery.dto.response.OrderResponse;
import com.delivery.dto.response.ProductResponse;
import com.delivery.dto.response.ShopResponse;
import com.delivery.security.UserPrincipal;
import com.delivery.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 店铺控制器
 */
@Tag(name = "店铺", description = "店铺相关接口")
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class ShopController {

    private final ShopService shopService;

    // ==================== 店铺信息 ====================

    @Operation(summary = "获取店铺信息", description = "获取当前登录店铺的信息")
    @GetMapping("/info")
    @PreAuthorize("hasRole('SHOP')")
    public Result<ShopResponse> getShopInfo(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        ShopResponse response = shopService.getShopInfo(userPrincipal.getId());
        return Result.success(response);
    }

    // ==================== 商品浏览 ====================

    @Operation(summary = "获取商品列表", description = "分页获取商品列表")
    @GetMapping("/products")
    @PreAuthorize("hasRole('SHOP')")
    public Result<PageResult<ProductResponse>> getProductList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category) {
        PageResult<ProductResponse> result = shopService.getProductList(userPrincipal.getId(), page, size, category);
        return Result.success(result);
    }

    @Operation(summary = "获取商品详情", description = "根据ID获取商品详情")
    @GetMapping("/products/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public Result<ProductResponse> getProductDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        ProductResponse response = shopService.getProductDetail(userPrincipal.getId(), id);
        return Result.success(response);
    }

    @Operation(summary = "获取商品分类列表", description = "获取所有商品分类")
    @GetMapping("/categories")
    @PreAuthorize("hasRole('SHOP')")
    public Result<List<String>> getCategoryList(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<String> categories = shopService.getCategoryList(userPrincipal.getId());
        return Result.success(categories);
    }

    // ==================== 订单管理 ====================

    @Operation(summary = "创建订单", description = "创建新订单")
    @PostMapping("/orders")
    @PreAuthorize("hasRole('SHOP')")
    public Result<OrderResponse> createOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = shopService.createOrder(userPrincipal.getId(), request);
        return Result.success(response);
    }

    @Operation(summary = "获取订单列表", description = "分页获取订单列表")
    @GetMapping("/orders")
    @PreAuthorize("hasRole('SHOP')")
    public Result<PageResult<OrderResponse>> getOrderList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        PageResult<OrderResponse> result = shopService.getOrderList(userPrincipal.getId(), page, size, status);
        return Result.success(result);
    }

    @Operation(summary = "获取订单详情", description = "根据ID获取订单详情")
    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public Result<OrderResponse> getOrderDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        OrderResponse response = shopService.getOrderDetail(userPrincipal.getId(), id);
        return Result.success(response);
    }

    @Operation(summary = "修改订单", description = "修改订单（仅限凌晨2点前的当日订单）")
    @PutMapping("/orders/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public Result<OrderResponse> updateOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = shopService.updateOrder(userPrincipal.getId(), id, request);
        return Result.success(response);
    }

    @Operation(summary = "确认收货", description = "确认订单已收货")
    @PutMapping("/orders/{id}/receive")
    @PreAuthorize("hasRole('SHOP')")
    public Result<Void> confirmReceive(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        shopService.confirmReceive(userPrincipal.getId(), id);
        return Result.success();
    }

    // ==================== 账单管理 ====================

    @Operation(summary = "获取账单列表", description = "分页获取账单列表")
    @GetMapping("/bills")
    @PreAuthorize("hasRole('SHOP')")
    public Result<PageResult<BillResponse>> getBillList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<BillResponse> result = shopService.getBillList(userPrincipal.getId(), page, size);
        return Result.success(result);
    }

    @Operation(summary = "获取账单详情", description = "根据ID获取账单详情")
    @GetMapping("/bills/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public Result<BillResponse> getBillDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        BillResponse response = shopService.getBillDetail(userPrincipal.getId(), id);
        return Result.success(response);
    }

    @Operation(summary = "支付账单", description = "支付指定账单")
    @PostMapping("/bills/{id}/pay")
    @PreAuthorize("hasRole('SHOP')")
    public Result<Void> payBill(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        shopService.payBill(userPrincipal.getId(), id);
        return Result.success();
    }
}
