package com.delivery.service;

import com.delivery.dto.request.LoginRequest;
import com.delivery.dto.request.RegisterRequest;
import com.delivery.dto.response.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册
     */
    void register(RegisterRequest request);
}
