package com.travel.ai.service;

import com.travel.ai.dto.ChatRequest;
import com.travel.ai.dto.ChatResponse;

import java.util.List;

/**
 * 会话管理服务接口
 * 负责管理用户对话会话的生命周期
 */
public interface SessionService {

    /**
     * 创建新会话
     *
     * @param userId 用户ID
     * @return 会话ID
     */
    String createSession(Long userId);

    /**
     * 获取会话信息
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    Object getSession(String sessionId);

    /**
     * 获取用户的所有会话
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Object> getUserSessions(Long userId);

    /**
     * 更新会话
     *
     * @param sessionId 会话ID
     * @param title 会话标题
     */
    void updateSession(String sessionId, String title);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     */
    void deleteSession(String sessionId);

    /**
     * 清理过期会话
     *
     * @param expireMinutes 过期时间（分钟）
     * @return 清理的会话数量
     */
    int cleanExpiredSessions(int expireMinutes);

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return 是否存在
     */
    boolean existsSession(String sessionId);

    /**
     * 增加会话消息计数
     *
     * @param sessionId 会话ID
     */
    void incrementMessageCount(String sessionId);

    /**
     * 获取会话消息数量
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    Integer getMessageCount(String sessionId);
}
