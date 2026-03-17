package com.delivery.util;

import com.delivery.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 用户上下文工具类
 */
public class UserContext {

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();

            // 直接检查是否是 UserPrincipal 类型
            if (principal instanceof UserPrincipal) {
                return ((UserPrincipal) principal).getId();
            }

            // 尝试通过反射获取 id 属性（处理类加载器不一致的情况）
            try {
                Class<?> principalClass = principal.getClass();
                if (principalClass.getName().contains("UserPrincipal")) {
                    // 尝试通过 getter 方法获取 id
                    try {
                        java.lang.reflect.Method getIdMethod = principalClass.getMethod("getId");
                        Object id = getIdMethod.invoke(principal);
                        if (id instanceof Long) {
                            return (Long) id;
                        }
                    } catch (NoSuchMethodException e) {
                        // 忽略，尝试其他方式
                    }
                }
            } catch (Exception e) {
                // 忽略反射异常
            }

            // 兼容字符串类型
            try {
                return Long.parseLong(principal.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取当前登录用户角色
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null
                && !authentication.getAuthorities().isEmpty()) {
            return authentication.getAuthorities().iterator().next().getAuthority();
        }
        return null;
    }
}
