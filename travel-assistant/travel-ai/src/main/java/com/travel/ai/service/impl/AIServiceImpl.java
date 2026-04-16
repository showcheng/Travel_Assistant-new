package com.travel.ai.service.impl;

import com.travel.ai.dto.ChatRequest;
import com.travel.ai.dto.ChatResponse;
import com.travel.ai.service.AIService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI服务实现类
 * 使用LangChain4j和GLM-5模型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final ChatLanguageModel chatLanguageModel;

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            log.info("收到对话请求: sessionId={}, message={}",
                    request.getSessionId(), request.getMessage());

            // 生成或使用现有sessionId
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }

            // 调用GLM-5模型生成回复
            String aiResponse = chatLanguageModel.generate(request.getMessage());

            // 构建响应
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .response(aiResponse)
                    .intentType("GENERAL")
                    .tokens(aiResponse.length())
                    .timestamp(LocalDateTime.now())
                    .finished(true)
                    .build();

        } catch (Exception e) {
            log.error("AI对话处理失败", e);
            return ChatResponse.builder()
                    .sessionId(request.getSessionId())
                    .response(null)
                    .error("抱歉，AI服务暂时不可用，请稍后再试")
                    .timestamp(LocalDateTime.now())
                    .finished(true)
                    .build();
        }
    }

    @Override
    public String chatStream(ChatRequest request) {
        // TODO: 实现流式输出
        return "stream-not-implemented";
    }

    @Override
    public void clearHistory(String sessionId) {
        // TODO: 实现对话历史清除
        log.info("清除对话历史: sessionId={}", sessionId);
    }

    @Override
    public String getHistory(String sessionId, Integer limit) {
        // TODO: 实现对话历史获取
        log.info("获取对话历史: sessionId={}, limit={}", sessionId, limit);
        return "history-not-implemented";
    }
}
