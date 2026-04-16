package com.travel.ai.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 对话请求DTO
 */
@Data
public class ChatRequest {

    /**
     * 用户消息
     */
    @NotBlank(message = "消息不能为空")
    @Size(max = 1000, message = "消息长度不能超过1000字符")
    private String message;

    /**
     * 会话ID (可选)
     */
    private String sessionId;

    /**
     * 用户ID (可选，从认证上下文获取)
     */
    private Long userId;

    /**
     * 流式输出标志
     */
    private Boolean stream = false;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 最大Token数
     */
    private Integer maxTokens;
}
