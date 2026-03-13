package com.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delivery.common.PageResult;
import com.delivery.common.ResultCode;
import com.delivery.dto.request.BranchManagerCreateRequest;
import com.delivery.dto.response.BranchManagerResponse;
import com.delivery.entity.BranchManager;
import com.delivery.entity.Driver;
import com.delivery.entity.Product;
import com.delivery.entity.Shop;
import com.delivery.exception.BusinessException;
import com.delivery.mapper.BranchManagerMapper;
import com.delivery.mapper.DriverMapper;
import com.delivery.mapper.ProductMapper;
import com.delivery.mapper.ShopMapper;
import com.delivery.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
