package com.delivery.controller;

import com.delivery.common.PageResult;
import com.delivery.common.Result;
import com.delivery.dto.request.BranchManagerCreateRequest;
import com.delivery.dto.response.BranchManagerResponse;
import com.delivery.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 总管理控制器
 */
@Tag(name = "总管理", description = "总管理相关接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "创建分管理", description = "创建新的分管理账号")
    @PostMapping("/branch-managers")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> createBranchManager(@Valid @RequestBody BranchManagerCreateRequest request) {
        adminService.createBranchManager(request);
        return Result.success();
    }

    @Operation(summary = "获取分管理列表", description = "分页获取分管理列表")
    @GetMapping("/branch-managers")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<BranchManagerResponse>> getBranchManagerList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<BranchManagerResponse> result = adminService.getBranchManagerList(page, size);
        return Result.success(result);
    }

    @Operation(summary = "获取分管理详情", description = "根据ID获取分管理详情")
    @GetMapping("/branch-managers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<BranchManagerResponse> getBranchManagerDetail(@PathVariable Long id) {
        BranchManagerResponse response = adminService.getBranchManagerDetail(id);
        return Result.success(response);
    }

    @Operation(summary = "切换分管理状态", description = "启用/禁用分管理账号")
    @PutMapping("/branch-managers/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleBranchManagerStatus(@PathVariable Long id) {
        adminService.toggleBranchManagerStatus(id);
        return Result.success();
    }

    @Operation(summary = "更新分管理", description = "更新分管理信息")
    @PutMapping("/branch-managers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateBranchManager(@PathVariable Long id, @RequestBody BranchManagerCreateRequest request) {
        adminService.updateBranchManager(id, request);
        return Result.success();
    }

    @Operation(summary = "删除分管理", description = "删除分管理及其下属店铺和司机")
    @DeleteMapping("/branch-managers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteBranchManager(@PathVariable Long id) {
        adminService.deleteBranchManager(id);
        return Result.success();
    }

    @Operation(summary = "获取统计数据", description = "获取总管理首页统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = adminService.getStatistics();
        return Result.success(statistics);
    }

    @Operation(summary = "获取数据中心数据", description = "获取总管理数据中心详细数据")
    @GetMapping("/data-center")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getDataCenter(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> data = adminService.getDataCenter(startDate, endDate);
        return Result.success(data);
    }
}
