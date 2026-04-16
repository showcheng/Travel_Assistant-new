package com.travel.common.interceptor;

import com.travel.common.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户认证拦截器
 * 从请求头中提取用户ID并设置到上下文
 */
@Slf4j
@Component
public class UserAuthenticationInterceptor implements HandlerInterceptor {

    // 不需要认证的路径
    private static final String[] EXCLUDE_PATHS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/products",
            "/swagger",
            "/api-docs",
            "/actuator"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 检查是否为排除路径
        if (isExcludePath(path)) {
            log.debug("路径 {} 不需要认证", path);
            return true;
        }

        // 从请求头中获取用户ID
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            UserContext.setUserIdFromHeader(userIdHeader);
            log.debug("从请求头中提取用户ID: {}, 路径: {}", userIdHeader, path);
        } else {
            log.debug("请求头中没有X-User-Id，使用默认用户ID, 路径: {}", path);
            UserContext.setCurrentUserId(2L); // 设置默认测试用户ID
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        // 清除用户上下文，防止内存泄漏
        UserContext.clear();
    }

    /**
     * 检查是否为排除路径
     */
    private boolean isExcludePath(String path) {
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.contains(excludePath)) {
                return true;
            }
        }
        return false;
    }
}