package com.travel.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话会话实体
 */
@Data
@TableName("conversation_session")
public class ConversationSession {

    /**
     * 会话ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会话唯一标识
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 会话状态
     */
    private String status;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiredAt;
}
