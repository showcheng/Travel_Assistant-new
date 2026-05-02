package com.travel.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户画像记忆实体
 *
 * 存储从对话中提取的用户偏好和旅行画像信息。
 * profileJson 字段包含完整的用户画像JSON，结构如下：
 * <pre>
 * {
 *   "preferences": [
 *     {"key": "travel_style", "value": "自然风光", "confidence": 0.9, "updatedAt": "2026-05-02T10:00:00"},
 *     {"key": "budget_range", "value": "3000-5000", "confidence": 0.7, "updatedAt": "2026-05-02T10:00:00"}
 *   ],
 *   "demographics": {
 *     "travel_companion": "家庭游(1大1小)",
 *     "dietary": "不吃辣"
 *   },
 *   "history": {
 *     "visited": ["北京","杭州"],
 *     "interested": ["云南","三亚"]
 *   },
 *   "feedback": [
 *     {"topic": "故宫行程", "sentiment": "negative", "note": "太赶，偏好慢节奏"}
 *   ]
 * }
 * </pre>
 *
 * 论文 5.3.4: 用户画像记忆机制
 */
@Data
@TableName("user_profile")
public class UserProfile {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户画像JSON字符串
     * 包含偏好、人口统计、历史记录、反馈等结构化信息
     */
    @TableField("profile_json")
    private String profileJson;

    /**
     * 贡献来源对话数量
     * 记录有多少次对话为该用户画像提供了信息
     */
    @TableField("source_count")
    private Integer sourceCount;

    /**
     * 最近一次贡献的会话ID
     */
    @TableField("last_conversation_id")
    private String lastConversationId;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 乐观锁版本号
     * 用于并发更新时的冲突检测
     */
    @Version
    @TableField("version")
    private Integer version;
}
