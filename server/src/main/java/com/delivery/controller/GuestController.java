package com.delivery.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delivery.common.PageResult;
import com.delivery.common.Result;
import com.delivery.entity.BranchManager;
import com.delivery.entity.Product;
import com.delivery.mapper.BranchManagerMapper;
import com.delivery.mapper.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 游客公开接口控制器
 * 提供无需登录即可访问的商品浏览接口，符合微信小程序隐私合规要求
 */
@Tag(name = "公开接口", description = "游客可访问的公开接口")
@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
public class GuestController {

    private final ProductMapper productMapper;
    private final BranchManagerMapper branchManagerMapper;

    /**
     * 获取分管理列表（用于游客选择供应商）
     * 仅返回基本的供应商信息，不包含敏感信息
     */
    @Operation(summary = "获取供应商列表", description = "获取所有可用的供应商（分管理）列表")
    @GetMapping("/suppliers")
    public Result<List<SupplierInfo>> getSupplierList() {
        List<BranchManager> managers = branchManagerMapper.selectList(
                new LambdaQueryWrapper<BranchManager>()
                        .eq(BranchManager::getStatus, 1)
                        .orderByAsc(BranchManager::getBrandName));

        List<SupplierInfo> result = managers.stream()
                .map(m -> {
                    SupplierInfo info = new SupplierInfo();
                    info.setId(m.getId());
                    info.setBrandName(m.getBrandName());
                    return info;
                })
                .collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 获取商品分类列表（游客可访问）
     */
    @Operation(summary = "获取商品分类", description = "获取指定供应商的商品分类列表")
    @GetMapping("/categories")
    public Result<List<String>> getCategoryList(
            @RequestParam Long branchManagerId) {

        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getBranchManagerId, branchManagerId)
                        .eq(Product::getStatus, 1)
                        .select(Product::getCategory)
                        .groupBy(Product::getCategory));

        List<String> categories = products.stream()
                .map(Product::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return Result.success(categories);
    }

    /**
     * 获取商品列表（游客可访问）
     * 游客模式下显示默认价格（老用户价）
     */
    @Operation(summary = "获取商品列表", description = "分页获取指定供应商的商品列表（游客模式）")
    @GetMapping("/products")
    public Result<PageResult<GuestProductResponse>> getProductList(
            @RequestParam Long branchManagerId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category) {

        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getBranchManagerId, branchManagerId)
                .eq(Product::getStatus, 1);

        // 只有当category有实际内容时才添加分类过滤
        if (category != null && !category.trim().isEmpty()) {
            wrapper.eq(Product::getCategory, category.trim());
        }

        wrapper.orderByDesc(Product::getCreatedAt);

        Page<Product> result = productMapper.selectPage(pageParam, wrapper);

        PageResult<GuestProductResponse> pageResult = PageResult.of(result.convert(product -> {
            GuestProductResponse response = new GuestProductResponse();
            response.setId(product.getId());
            response.setName(product.getName());
            response.setCategory(product.getCategory());
            // 游客模式显示老用户价作为参考价格
            response.setPrice(product.getOldPrice());
            response.setUnit(product.getUnit());
            response.setDescription(product.getDescription());
            response.setImageUrl(product.getImageUrl());
            return response;
        }));

        return Result.success(pageResult);
    }

    /**
     * 获取商品详情（游客可访问）
     */
    @Operation(summary = "获取商品详情", description = "获取商品详情（游客模式）")
    @GetMapping("/products/{id}")
    public Result<GuestProductResponse> getProductDetail(
            @RequestParam Long branchManagerId,
            @PathVariable Long id) {

        Product product = productMapper.selectOne(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getId, id)
                        .eq(Product::getBranchManagerId, branchManagerId)
                        .eq(Product::getStatus, 1));

        if (product == null) {
            return Result.fail(404, "商品不存在");
        }

        GuestProductResponse response = new GuestProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setCategory(product.getCategory());
        response.setPrice(product.getOldPrice());
        response.setUnit(product.getUnit());
        response.setDescription(product.getDescription());
        response.setImageUrl(product.getImageUrl());

        return Result.success(response);
    }

    /**
     * 供应商信息（简化版，不包含敏感信息）
     */
    @lombok.Data
    public static class SupplierInfo {
        private Long id;
        private String brandName;
    }

    /**
     * 游客商品响应（不包含敏感价格信息）
     */
    @lombok.Data
    public static class GuestProductResponse {
        private Long id;
        private String name;
        private String category;
        private java.math.BigDecimal price; // 参考价格
        private String unit;
        private String description;
        private String imageUrl;
    }
}
