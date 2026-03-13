package com.delivery.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 加密密码
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    public static void main(String[] args) {
        // 生成密码123456的BCrypt加密值
        String password = "123456";
        String encoded = encode(password);
        System.out.println("原始密码: " + password);
        System.out.println("加密后密码: " + encoded);
        System.out.println("验证结果: " + matches(password, encoded));
    }
}
