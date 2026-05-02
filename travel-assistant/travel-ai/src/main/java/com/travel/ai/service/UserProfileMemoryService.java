package com.travel.ai.service;

import com.travel.ai.entity.UserProfile;
import java.util.List;
import java.util.Map;

/**
 * 用户画像记忆服务
 *
 * 提供用户偏好的蒸馏提取、有界存储和时间衰减管理。
 * 从对话中提取用户旅行偏好，维护结构化画像，并确保记忆在合理范围内。
 *
 * 论文 5.3.4: 用户画像记忆机制
 */
public interface UserProfileMemoryService {

    /**
     * 加载用户画像，不存在时返回 null。
     * 加载时自动应用时间衰减：较旧的偏好置信度会降低。
     *
     * @param userId 用户ID
     * @return 用户画像实体，不存在时返回 null
     */
    UserProfile getProfile(String userId);

    /**
     * 获取格式化的画像摘要，用于注入到 LLM 上下文中。
     * 返回人类可读的文本描述，方便 LLM 理解用户偏好。
     *
     * @param userId 用户ID
     * @return 格式化的画像文本，无画像时返回默认提示
     */
    String getProfileContext(String userId);

    /**
     * 从对话中蒸馏提取用户偏好并更新画像。
     * 优先使用 LLM 提取结构化偏好，LLM 失败时降级到正则匹配。
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param messages  对话消息列表，每条消息包含 role 和 content
     */
    void distillFromConversation(String userId, String sessionId, List<Map<String, String>> messages);

    /**
     * 更新单个偏好键值。
     * 如果键已存在，取更高的置信度值。
     *
     * @param userId     用户ID
     * @param key        偏好键
     * @param value      偏好值
     * @param confidence 置信度 (0-1)
     */
    void updatePreference(String userId, String key, String value, double confidence);

    /**
     * 移除一个偏好键。
     *
     * @param userId 用户ID
     * @param key    要移除的偏好键
     */
    void removePreference(String userId, String key);

    /**
     * 获取所有偏好作为 key-value 映射。
     *
     * @param userId 用户ID
     * @return 偏好键值对映射，无画像时返回空 Map
     */
    Map<String, String> getPreferences(String userId);

    /**
     * 检查并强制执行有界记忆限制 (profileJson 最大 2048 字符)。
     * 超限时优先使用 LLM 合并精简，LLM 失败时降级截断低置信度项。
     *
     * @param userId 用户ID
     */
    void enforceMemoryLimit(String userId);
}
