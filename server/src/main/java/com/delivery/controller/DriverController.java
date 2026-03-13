package com.delivery.controller;

import com.delivery.common.Result;
import com.delivery.dto.response.DeliveryHistoryResponse;
import com.delivery.dto.response.DeliveryListResponse;
import com.delivery.dto.response.DriverResponse;
import com.delivery.dto.response.DriverStatisticsResponse;
import com.delivery.dto.response.OrderResponse;
import com.delivery.dto.response.PickingListResponse;
import com.delivery.security.UserPrincipal;
import com.delivery.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 司机控制器
 */
@Tag(name = "司机", description = "司机相关接口")
@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class DriverController {

    private final DriverService driverService;

    // ==================== 个人信息 ====================

    @Operation(summary = "获取司机信息", description = "获取当前登录司机的个人信息")
    @GetMapping("/info")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<DriverResponse> getDriverInfo(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        DriverResponse driverInfo = driverService.getDriverInfo(userPrincipal.getId());
        return Result.success(driverInfo);
    }

    // ==================== 待分配订单 ====================

    @Operation(summary = "获取待分配订单列表", description = "获取该司机所属分管理下所有待分配的订单")
    @GetMapping("/pending-orders")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<List<OrderResponse>> getPendingOrders(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<OrderResponse> orders = driverService.getPendingOrders(userPrincipal.getId());
        return Result.success(orders);
    }

    @Operation(summary = "选择订单", description = "选择一个或多个订单进行配送")
    @PostMapping("/pending-orders/select")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<Void> selectOrders(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody List<Long> orderIds) {
        driverService.selectOrders(userPrincipal.getId(), orderIds);
        return Result.success();
    }

    @Operation(summary = "取消选择订单", description = "取消已选择的订单")
    @PostMapping("/pending-orders/deselect")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<Void> deselectOrders(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody List<Long> orderIds) {
        driverService.deselectOrders(userPrincipal.getId(), orderIds);
        return Result.success();
    }

    @Operation(summary = "获取已选择的订单列表", description = "获取司机已选择待拣货的订单")
    @GetMapping("/selected-orders")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<List<OrderResponse>> getSelectedOrders(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<OrderResponse> orders = driverService.getSelectedOrders(userPrincipal.getId());
        return Result.success(orders);
    }

    @Operation(summary = "删除已选择的订单", description = "从已选订单中删除指定订单")
    @DeleteMapping("/selected-orders/{orderId}")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<Void> removeSelectedOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long orderId) {
        driverService.removeSelectedOrder(userPrincipal.getId(), orderId);
        return Result.success();
    }

    // ==================== 拣货 ====================

    @Operation(summary = "获取拣货清单", description = "获取当前拣货清单")
    @GetMapping("/picking-list")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<PickingListResponse> getPickingList(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        PickingListResponse response = driverService.getPickingList(userPrincipal.getId());
        return Result.success(response);
    }

    @Operation(summary = "完成拣货", description = "标记某个商品拣货完成")
    @PutMapping("/picking-list/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<Void> completePicking(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        driverService.completePicking(userPrincipal.getId(), id);
        return Result.success();
    }

    // ==================== 送货 ====================

    @Operation(summary = "获取送货清单", description = "获取待送货清单列表")
    @GetMapping("/delivery-list")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<List<DeliveryListResponse>> getDeliveryList(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<DeliveryListResponse> list = driverService.getDeliveryList(userPrincipal.getId());
        return Result.success(list);
    }

    @Operation(summary = "完成送货", description = "标记送货完成")
    @PutMapping("/delivery-list/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<Void> completeDelivery(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        driverService.completeDelivery(userPrincipal.getId(), id);
        return Result.success();
    }

    // ==================== 个人中心 ====================

    @Operation(summary = "获取司机统计数据", description = "获取司机的送货统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<DriverStatisticsResponse> getStatistics(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        DriverStatisticsResponse statistics = driverService.getStatistics(userPrincipal.getId());
        return Result.success(statistics);
    }

    @Operation(summary = "获取送货历史记录", description = "获取司机的历史送货记录")
    @GetMapping("/delivery-history")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<List<DeliveryHistoryResponse>> getDeliveryHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<DeliveryHistoryResponse> history = driverService.getDeliveryHistory(userPrincipal.getId());
        return Result.success(history);
    }
}
