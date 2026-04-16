package com.travel.common.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文工具类
 * 用于在微服务中获取当前登录用户信息
 */
@Slf4j
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     */
    public static void setCurrentUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
        log.debug("设置当前用户ID: {}", userId);
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null) {
            log.warn("当前线程没有设置用户ID，返回默认用户ID: 2");
            return 2L; // 返回默认测试用户ID
        }
        return userId;
    }

    /**
     * 清除当前用户ID
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        log.debug("清除当前用户ID");
    }

    /**
     * 从请求头中提取用户ID并设置到上下文
     */
    public static void setUserIdFromHeader(String userIdHeader) {
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                setCurrentUserId(userId);
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID格式: {}", userIdHeader);
            }
        }
    }
}