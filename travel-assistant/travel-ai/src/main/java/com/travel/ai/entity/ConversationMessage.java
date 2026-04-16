package com.travel.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话消息实体
 */
@Data
@TableName("conversation_message")
public class ConversationMessage {

    /**
     * 消息ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 消息角色
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * Token数量
     */
    private Integer tokens;

    /**
     * 意图类型
     */
    private String intentType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
