package com.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delivery.common.PageResult;
import com.delivery.common.ResultCode;
import com.delivery.dto.request.DriverCreateRequest;
import com.delivery.dto.request.ProductCreateRequest;
import com.delivery.dto.request.ShopCreateRequest;
import com.delivery.dto.response.DriverResponse;
import com.delivery.dto.response.ProductResponse;
import com.delivery.dto.response.ShopResponse;
import com.delivery.entity.Driver;
import com.delivery.entity.Product;
import com.delivery.entity.Shop;
import com.delivery.exception.BusinessException;
import com.delivery.mapper.DriverMapper;
import com.delivery.mapper.ProductMapper;
import com.delivery.mapper.ShopMapper;
import com.delivery.service.BranchManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 分管理服务实现
 */
@Service
@RequiredArgsConstructor
public class BranchManagerServiceImpl implements BranchManagerService {

    private final ShopMapper shopMapper;
    private final DriverMapper driverMapper;
    private final ProductMapper productMapper;
    private final PasswordEncoder passwordEncoder;

    // ==================== 店铺管理 ====================

    @Override
    public void createShop(Long branchManagerId, ShopCreateRequest request) {
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
        shop.setShowPrice(request.getShowPrice());
        shop.setPriceType(request.getPriceType());
        shop.setStatus(1);

        shopMapper.insert(shop);
    }

    @Override
    public PageResult<ShopResponse> getShopList(Long branchManagerId, Integer page, Integer size) {
        Page<Shop> pageParam = new Page<>(page, size);
        Page<Shop> result = shopMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Shop>()
                        .eq(Shop::getBranchManagerId, branchManagerId)
                        .orderByDesc(Shop::getCreatedAt));

        return PageResult.of(result.convert(this::convertToShopResponse));
    }

    @Override
    public ShopResponse getShopDetail(Long branchManagerId, Long shopId) {
        Shop shop = shopMapper.selectOne(
                new LambdaQueryWrapper<Shop>()
                        .eq(Shop::getId, shopId)
                        .eq(Shop::getBranchManagerId, branchManagerId));
        if (shop == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToShopResponse(shop);
    }

    @Override
    public void updateShop(Long branchManagerId, Long shopId, ShopCreateRequest request) {
        Shop shop = shopMapper.selectOne(
                new LambdaQueryWrapper<Shop>()
                        .eq(Shop::getId, shopId)
                        .eq(Shop::getBranchManagerId, branchManagerId));
        if (shop == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        shop.setName(request.getName());
        shop.setAddress(request.getAddress());
        if (StringUtils.hasText(request.getPassword())) {
            shop.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        shop.setShowPrice(request.getShowPrice());
        shop.setPriceType(request.getPriceType());

        shopMapper.updateById(shop);
    }

    @Override
    public void deleteShop(Long branchManagerId, Long shopId) {
        Shop shop = shopMapper.selectOne(
                new LambdaQueryWrapper<Shop>()
                        .eq(Shop::getId, shopId)
                        .eq(Shop::getBranchManagerId, branchManagerId));
        if (shop == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        shopMapper.deleteById(shopId);
    }

    // ==================== 司机管理 ====================

    @Override
    public void createDriver(Long branchManagerId, DriverCreateRequest request) {
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
    }

    @Override
    public PageResult<DriverResponse> getDriverList(Long branchManagerId, Integer page, Integer size) {
        Page<Driver> pageParam = new Page<>(page, size);
        Page<Driver> result = driverMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getBranchManagerId, branchManagerId)
                        .orderByDesc(Driver::getCreatedAt));

        return PageResult.of(result.convert(this::convertToDriverResponse));
    }

    @Override
    public DriverResponse getDriverDetail(Long branchManagerId, Long driverId) {
        Driver driver = driverMapper.selectOne(
                new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getId, driverId)
                        .eq(Driver::getBranchManagerId, branchManagerId));
        if (driver == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToDriverResponse(driver);
    }

    @Override
    public void updateDriver(Long branchManagerId, Long driverId, DriverCreateRequest request) {
        Driver driver = driverMapper.selectOne(
                new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getId, driverId)
                        .eq(Driver::getBranchManagerId, branchManagerId));
        if (driver == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        driver.setName(request.getName());
        if (StringUtils.hasText(request.getPassword())) {
            driver.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        driverMapper.updateById(driver);
    }

    @Override
    public void deleteDriver(Long branchManagerId, Long driverId) {
        Driver driver = driverMapper.selectOne(
                new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getId, driverId)
                        .eq(Driver::getBranchManagerId, branchManagerId));
        if (driver == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        driverMapper.deleteById(driverId);
    }

    // ==================== 商品管理 ====================

    @Override
    public void createProduct(Long branchManagerId, ProductCreateRequest request) {
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
    }

    @Override
    public PageResult<ProductResponse> getProductList(Long branchManagerId, Integer page, Integer size,
            String category) {
        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getBranchManagerId, branchManagerId);

        if (StringUtils.hasText(category)) {
            wrapper.eq(Product::getCategory, category);
        }

        wrapper.orderByDesc(Product::getCreatedAt);

        Page<Product> result = productMapper.selectPage(pageParam, wrapper);

        return PageResult.of(result.convert(this::convertToProductResponse));
    }

    @Override
    public ProductResponse getProductDetail(Long branchManagerId, Long productId) {
        Product product = productMapper.selectOne(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getId, productId)
                        .eq(Product::getBranchManagerId, branchManagerId));
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return convertToProductResponse(product);
    }

    @Override
    public void updateProduct(Long branchManagerId, Long productId, ProductCreateRequest request) {
        Product product = productMapper.selectOne(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getId, productId)
                        .eq(Product::getBranchManagerId, branchManagerId));
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setOldPrice(request.getOldPrice());
        product.setNewPrice(request.getNewPrice());
        product.setUnit(request.getUnit());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());

        productMapper.updateById(product);
    }

    @Override
    public void deleteProduct(Long branchManagerId, Long productId) {
        Product product = productMapper.selectOne(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getId, productId)
                        .eq(Product::getBranchManagerId, branchManagerId));
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        productMapper.deleteById(productId);
    }

    // ==================== 转换方法 ====================

    private ShopResponse convertToShopResponse(Shop shop) {
        ShopResponse response = new ShopResponse();
        response.setId(shop.getId());
        response.setName(shop.getName());
        response.setAddress(shop.getAddress());
        response.setPhone(shop.getPhone());
        response.setShowPrice(shop.getShowPrice());
        response.setPriceType(shop.getPriceType());
        response.setStatus(shop.getStatus());
        response.setCreatedAt(shop.getCreatedAt());
        return response;
    }

    private DriverResponse convertToDriverResponse(Driver driver) {
        DriverResponse response = new DriverResponse();
        response.setId(driver.getId());
        response.setName(driver.getName());
        response.setPhone(driver.getPhone());
        response.setStatus(driver.getStatus());
        response.setCreatedAt(driver.getCreatedAt());
        return response;
    }

    private ProductResponse convertToProductResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setCategory(product.getCategory());
        response.setOldPrice(product.getOldPrice());
        response.setNewPrice(product.getNewPrice());
        response.setUnit(product.getUnit());
        response.setDescription(product.getDescription());
        response.setImageUrl(product.getImageUrl());
        response.setStatus(product.getStatus());
        response.setCreatedAt(product.getCreatedAt());
        return response;
    }
}
