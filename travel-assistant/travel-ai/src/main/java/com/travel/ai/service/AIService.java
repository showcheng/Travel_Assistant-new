package com.travel.ai.service;

import com.travel.ai.dto.ChatRequest;
import com.travel.ai.dto.ChatResponse;

/**
 * AI服务接口
 */
public interface AIService {

    /**
     * 处理对话请求
     *
     * @param request 对话请求
     * @return AI回复
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式对话 (返回SSE事件)
     *
     * @param request 对话请求
     * @return 流式响应
     */
    String chatStream(ChatRequest request);

    /**
     * 清除对话历史
     *
     * @param sessionId 会话ID
     */
    void clearHistory(String sessionId);

    /**
     * 获取对话历史
     *
     * @param sessionId 会话ID
     * @param limit     限制条数
     * @return 对话历史
     */
    String getHistory(String sessionId, Integer limit);
}
