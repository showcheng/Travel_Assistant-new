package com.travel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * AI回复内容
     */
    private String response;

    /**
     * 意图类型
     */
    private String intentType;

    /**
     * 使用的Token数
     */
    private Integer tokens;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 是否完成
     */
    private Boolean finished;

    /**
     * 错误信息
     */
    private String error;
}
