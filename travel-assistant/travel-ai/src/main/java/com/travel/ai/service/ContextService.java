package com.travel.ai.service;

import com.travel.ai.dto.ChatRequest;
import com.travel.ai.dto.ChatResponse;

import java.util.List;
import java.util.Map;

/**
 * 上下文管理服务接口
 * 负责管理对话历史和上下文
 */
public interface ContextService {

    /**
     * 添加消息到上下文
     *
     * @param sessionId 会话ID
     * @param role 角色 (user/assistant/system)
     * @param content 消息内容
     */
    void addMessage(String sessionId, String role, String content);

    /**
     * 获取会话的对话历史
     *
     * @param sessionId 会话ID
     * @param limit 限制条数
     * @return 对话历史
     */
    List<Map<String, String>> getHistory(String sessionId, int limit);

    /**
     * 获取格式化的对话上下文（用于发送给AI）
     *
     * @param sessionId 会话ID
     * @param limit 限制条数
     * @return 格式化的上下文
     */
    String getFormattedContext(String sessionId, int limit);

    /**
     * 清除会话上下文
     *
     * @param sessionId 会话ID
     */
    void clearContext(String sessionId);

    /**
     * 获取会话的所有消息
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<Map<String, String>> getAllMessages(String sessionId);

    /**
     * 保存消息到数据库
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param role 角色
     * @param content 内容
     * @param intentType 意图类型
     */
    void saveMessage(String sessionId, Long userId, String role, String content, String intentType);

    /**
     * 简单的指代消解
     *
     * @param text 输入文本
     * @param context 对话上下文
     * @return 消解后的文本
     */
    String resolveReference(String text, List<Map<String, String>> context);

    /**
     * 检查上下文是否过期
     *
     * @param sessionId 会话ID
     * @param expireMinutes 过期时间（分钟）
     * @return 是否过期
     */
    boolean isContextExpired(String sessionId, int expireMinutes);

    /**
     * 获取最近N轮对话
     *
     * @param sessionId 会话ID
     * @param rounds 轮数
     * @return 最近N轮对话
     */
    List<Map<String, String>> getRecentRounds(String sessionId, int rounds);

    /**
     * 获取压缩后的对话上下文（用于发送给AI）
     *
     * 当对话历史超过阈值时，将较早的消息压缩为摘要，
     * 与最近的对话拼接后返回，保留完整语义上下文。
     *
     * @param sessionId 会话ID
     * @param recentRounds 保留的最近轮数
     * @return 压缩后的格式化上下文
     */
    String getCompressedContext(String sessionId, int recentRounds);

    /**
     * 构建完整上下文：压缩对话上下文 + 用户画像上下文
     *
     * 将对话历史与用户画像信息合并，提供完整的个性化上下文给 LLM。
     * 用户画像出现在对话上下文之前，方便 AI 优先理解用户偏好。
     * userId 为 null 或空时仅返回对话上下文。
     *
     * @param sessionId   会话ID
     * @param userId      用户ID（可为 null）
     * @param recentRounds 保留的最近对话轮数
     * @return 合并后的完整上下文字符串
     */
    String buildFullContext(String sessionId, String userId, int recentRounds);
}
