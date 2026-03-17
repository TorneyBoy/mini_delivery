package com.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delivery.common.ResultCode;
import com.delivery.dto.request.LoginRequest;
import com.delivery.dto.request.RegisterRequest;
import com.delivery.dto.response.LoginResponse;
import com.delivery.entity.Admin;
import com.delivery.entity.BranchManager;
import com.delivery.entity.Driver;
import com.delivery.entity.Shop;
import com.delivery.enums.UserRole;
import com.delivery.exception.BusinessException;
import com.delivery.mapper.AdminMapper;
import com.delivery.mapper.BranchManagerMapper;
import com.delivery.mapper.DriverMapper;
import com.delivery.mapper.ShopMapper;
import com.delivery.security.JwtTokenProvider;
import com.delivery.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdminMapper adminMapper;
    private final BranchManagerMapper branchManagerMapper;
    private final ShopMapper shopMapper;
    private final DriverMapper driverMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(LoginRequest request) {
        String phone = request.getPhone();
        String password = request.getPassword();

        // 尝试作为总管理登录
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getPhone, phone));
        if (admin != null) {
            if (!passwordEncoder.matches(password, admin.getPassword())) {
                throw new BusinessException(ResultCode.PASSWORD_ERROR);
            }
            String token = jwtTokenProvider.generateToken(admin.getId(), UserRole.ADMIN.getCode(), phone);
            return LoginResponse.of(token, admin.getId(), phone, UserRole.ADMIN.getCode(), UserRole.ADMIN.getDesc());
        }

        // 尝试作为分管理登录
        BranchManager branchManager = branchManagerMapper.selectOne(
                new LambdaQueryWrapper<BranchManager>().eq(BranchManager::getPhone, phone));
        if (branchManager != null) {
            if (!passwordEncoder.matches(password, branchManager.getPassword())) {
                throw new BusinessException(ResultCode.PASSWORD_ERROR);
            }
            if (branchManager.getStatus() != 1) {
                throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
            }
            String token = jwtTokenProvider.generateToken(branchManager.getId(), UserRole.BRANCH_MANAGER.getCode(),
                    phone);
            return LoginResponse.of(token, branchManager.getId(), phone, UserRole.BRANCH_MANAGER.getCode(),
                    UserRole.BRANCH_MANAGER.getDesc(), branchManager.getBrandName(), branchManager.getBrandName());
        }

        // 尝试作为店铺登录
        Shop shop = shopMapper.selectOne(
                new LambdaQueryWrapper<Shop>().eq(Shop::getPhone, phone));
        if (shop != null) {
            if (!passwordEncoder.matches(password, shop.getPassword())) {
                throw new BusinessException(ResultCode.PASSWORD_ERROR);
            }
            if (shop.getStatus() == 0) {
                throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "您的账号正在等待审核，请耐心等待");
            }
            if (shop.getStatus() != 1) {
                throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
            }
            String token = jwtTokenProvider.generateToken(shop.getId(), UserRole.SHOP.getCode(), phone);
            return LoginResponse.of(token, shop.getId(), phone, UserRole.SHOP.getCode(), UserRole.SHOP.getDesc());
        }

        // 尝试作为司机登录
        Driver driver = driverMapper.selectOne(
                new LambdaQueryWrapper<Driver>().eq(Driver::getPhone, phone));
        if (driver != null) {
            if (!passwordEncoder.matches(password, driver.getPassword())) {
                throw new BusinessException(ResultCode.PASSWORD_ERROR);
            }
            if (driver.getStatus() == 0) {
                throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "您的账号正在等待审核，请耐心等待");
            }
            if (driver.getStatus() != 1) {
                throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
            }
            String token = jwtTokenProvider.generateToken(driver.getId(), UserRole.DRIVER.getCode(), phone);
            return LoginResponse.of(token, driver.getId(), phone, UserRole.DRIVER.getCode(), UserRole.DRIVER.getDesc());
        }

        throw new BusinessException(ResultCode.USER_NOT_FOUND);
    }

    @Override
    public void register(RegisterRequest request) {
        String role = request.getRole();
        Long branchManagerId = request.getBranchManagerId();
        String phone = request.getPhone();

        // 验证分管理是否存在
        BranchManager branchManager = branchManagerMapper.selectById(branchManagerId);
        if (branchManager == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "分管理不存在");
        }

        if ("SHOP".equals(role)) {
            // 注册店铺
            // 检查手机号是否已存在
            Long count = shopMapper.selectCount(
                    new LambdaQueryWrapper<Shop>().eq(Shop::getPhone, phone));
            if (count > 0) {
                throw new BusinessException(ResultCode.PHONE_EXISTS, "手机号已被注册");
            }

            Shop shop = new Shop();
            shop.setBranchManagerId(branchManagerId);
            shop.setName(request.getName());
            shop.setAddress(request.getAddress());
            shop.setLatitude(request.getLatitude());
            shop.setLongitude(request.getLongitude());
            shop.setPhone(phone);
            shop.setPassword(passwordEncoder.encode(request.getPassword()));
            shop.setShowPrice(1);
            shop.setPriceType(1);
            // 注册时状态设为0（待审核），需要分管理审核通过后才能使用
            shop.setStatus(0);

            shopMapper.insert(shop);
            log.info("店铺注册申请已提交: {}, 等待分管理审核", phone);

        } else if ("DRIVER".equals(role)) {
            // 注册司机
            // 检查手机号是否已存在
            Long count = driverMapper.selectCount(
                    new LambdaQueryWrapper<Driver>().eq(Driver::getPhone, phone));
            if (count > 0) {
                throw new BusinessException(ResultCode.PHONE_EXISTS, "手机号已被注册");
            }

            Driver driver = new Driver();
            driver.setBranchManagerId(branchManagerId);
            driver.setName(request.getName());
            driver.setPhone(phone);
            driver.setPassword(passwordEncoder.encode(request.getPassword()));
            // 注册时状态设为0（待审核），需要分管理审核通过后才能使用
            driver.setStatus(0);

            driverMapper.insert(driver);
            log.info("司机注册申请已提交: {}, 等待分管理审核", phone);

        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的角色类型");
        }
    }
}
