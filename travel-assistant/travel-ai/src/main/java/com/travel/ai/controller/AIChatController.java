package com.travel.ai.controller;

import com.travel.ai.client.ExternalServiceClient;
import com.travel.ai.dto.ChatRequest;
import com.travel.ai.dto.ChatResponse;
import com.travel.ai.enums.IntentType;
import com.travel.ai.service.AIService;
import com.travel.ai.service.ContextService;
import com.travel.ai.service.IntentService;
import com.travel.ai.service.RAGChatService;
import com.travel.ai.service.SessionService;
import com.travel.ai.service.UserProfileMemoryService;
import com.travel.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI对话控制器 (集成版)
 * 集成会话管理、上下文管理和意图识别
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
@Tag(name = "AI对话", description = "AI智能对话接口（集成版）")
public class AIChatController {

    private final AIService aiService;
    private final SessionService sessionService;
    private final ContextService contextService;
    private final IntentService intentService;
    private final RAGChatService ragChatService;
    private final ExternalServiceClient externalServiceClient;
    private final UserProfileMemoryService userProfileMemoryService;

    /**
     * 发送消息并获取AI回复 (集成版)
     */
    @PostMapping("/send")
    @Operation(summary = "发送消息", description = "发送消息给AI并获取智能回复，支持上下文管理")
    public Result<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        try {
            log.info("收到对话请求: sessionId={}, message={}", request.getSessionId(), request.getMessage());

            // 1. 处理会话ID
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = sessionService.createSession(request.getUserId() != null ?
                    request.getUserId() : 1L);
            } else if (!sessionService.existsSession(sessionId)) {
                sessionId = sessionService.createSession(request.getUserId() != null ?
                    request.getUserId() : 1L);
            }

            // 2. 识别意图
            IntentType intent = intentService.recognizeIntent(request.getMessage());
            log.info("识别意图: intent={}", intent);

            // 3. 检查是否需要调用外部服务
            if (intentService.requiresExternalService(intent)) {
                return handleExternalServiceRequest(sessionId, request, intent);
            }

            // 4. 添加用户消息到上下文
            contextService.addMessage(sessionId, "user", request.getMessage());

            // 5. 通过RAG管道处理消息 (意图识别 + 知识库检索 + 增强Prompt + AI生成)
            String userIdStr = request.getUserId() != null ? request.getUserId().toString() : null;
            ChatResponse response = ragChatService.chatWithRAG(request.getMessage(), sessionId, userIdStr);

            // 6. 添加AI回复到上下文
            contextService.addMessage(sessionId, "assistant", response.getResponse());

            // 7. 保存到数据库
            contextService.saveMessage(sessionId, 1L, "user", request.getMessage(), intent.getCode());
            contextService.saveMessage(sessionId, 1L, "assistant", response.getResponse(), intent.getCode());

            // 8. 更新会话消息计数
            sessionService.incrementMessageCount(sessionId);

            // 9. 异步蒸馏用户偏好 (至少需要4条消息 = 2轮对话)
            distillProfileAsync(sessionId, request.getUserId());

            log.info("对话完成: sessionId={}, intent={}, tokens={}", sessionId, intent, response.getTokens());
            return Result.success(response);

        } catch (Exception e) {
            log.error("对话处理失败", e);
            return Result.error("对话处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理外部服务请求
     */
    private Result<ChatResponse> handleExternalServiceRequest(
            String sessionId,
            ChatRequest request,
            IntentType intent) {

        try {
            // 提取实体
            Map<String, Object> entities = intentService.extractEntities(request.getMessage(), intent);

            // 根据意图类型调用相应服务
            switch (intent) {
                case PRODUCT_RECOMMENDATION:
                    return handleProductRecommendation(sessionId, request, entities);

                case ORDER_QUERY:
                    return handleOrderQuery(sessionId, request, entities);

                default:
                    // 使用通用对话
                    return handleGeneralChat(sessionId, request);
            }

        } catch (Exception e) {
            log.error("外部服务调用失败", e);
            return handleGeneralChat(sessionId, request);
        }
    }

    /**
     * 处理产品推荐请求
     */
    private Result<ChatResponse> handleProductRecommendation(
            String sessionId,
            ChatRequest request,
            Map<String, Object> entities) {

        String keyword = entities.containsKey("location")
                ? entities.get("location").toString()
                : "";
        String recommendation = externalServiceClient.getProducts(keyword);

        ChatResponse response = ChatResponse.builder()
                .sessionId(sessionId)
                .response(recommendation)
                .intentType("product_recommendation")
                .tokens(recommendation.length())
                .timestamp(LocalDateTime.now())
                .finished(true)
                .build();

        return Result.success(response);
    }

    /**
     * 处理订单查询请求
     */
    private Result<ChatResponse> handleOrderQuery(
            String sessionId,
            ChatRequest request,
            Map<String, Object> entities) {

        Long userId = request.getUserId() != null ? request.getUserId() : 1L;
        String orderInfo = externalServiceClient.getOrders(userId);

        ChatResponse response = ChatResponse.builder()
                .sessionId(sessionId)
                .response(orderInfo)
                .intentType("order_query")
                .tokens(orderInfo.length())
                .timestamp(LocalDateTime.now())
                .finished(true)
                .build();

        return Result.success(response);
    }

    /**
     * 处理通用对话
     */
    private Result<ChatResponse> handleGeneralChat(String sessionId, ChatRequest request) {
        try {
            String aiResponse = aiService.chatStream(request.getMessage());

            ChatResponse response = ChatResponse.builder()
                    .sessionId(sessionId)
                    .response(aiResponse)
                    .intentType("general")
                    .tokens(aiResponse.length())
                    .timestamp(LocalDateTime.now())
                    .finished(true)
                    .build();

            return Result.success(response);

        } catch (Exception e) {
            log.error("通用对话失败", e);
            throw e;
        }
    }

    /**
     * 构建完整提示词
     */
    private String buildFullPrompt(String context, String userMessage) {
        StringBuilder prompt = new StringBuilder();

        if (context != null && !context.isEmpty()) {
            prompt.append(context).append("\n");
        }

        prompt.append("用户: ").append(userMessage).append("\n");
        prompt.append("助手:");

        return prompt.toString();
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/history/{sessionId}")
    @Operation(summary = "获取会话历史", description = "获取指定会话的对话历史")
    public Result<String> getHistory(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            String history = contextService.getFormattedContext(sessionId, limit);
            return Result.success(history);
        } catch (Exception e) {
            log.error("获取会话历史失败", e);
            return Result.error("获取会话历史失败");
        }
    }

    /**
     * 清除会话历史
     */
    @DeleteMapping("/history/{sessionId}")
    @Operation(summary = "清除会话历史", description = "清除指定会话的对话历史")
    public Result<Void> clearHistory(@PathVariable String sessionId) {
        try {
            contextService.clearContext(sessionId);
            return Result.success();
        } catch (Exception e) {
            log.error("清除会话历史失败", e);
            return Result.error("清除会话历史失败");
        }
    }

    /**
     * 获取会话信息
     */
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "获取会话信息", description = "获取会话的详细信息")
    public Result<Object> getSessionInfo(@PathVariable String sessionId) {
        try {
            Object session = sessionService.getSession(sessionId);
            if (session == null) {
                return Result.error("会话不存在");
            }
            return Result.success(session);
        } catch (Exception e) {
            log.error("获取会话信息失败", e);
            return Result.error("获取会话信息失败");
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查AI服务是否正常")
    public Result<Map<String, Object>> health() {
        Map<String, Object> healthInfo = Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "features", Map.of(
                        "session_management", true,
                        "context_management", true,
                        "intent_recognition", true,
                        "websocket", true,
                        "user_profile_memory", true
                )
        );
        return Result.success(healthInfo);
    }

    /**
     * Asynchronously distill user preferences from the conversation.
     *
     * Only runs when:
     * - userId is not null
     * - The conversation has at least 4 messages (2 full rounds)
     *
     * Failures are logged but do not affect the chat response.
     */
    private void distillProfileAsync(String sessionId, Long userId) {
        if (userId == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, String>> messages = contextService.getAllMessages(sessionId);
                if (messages == null || messages.size() < 4) {
                    log.debug("跳过画像蒸馏，消息不足4条: sessionId={}, count={}",
                            sessionId, messages != null ? messages.size() : 0);
                    return;
                }

                userProfileMemoryService.distillFromConversation(
                        userId.toString(), sessionId, messages);
                log.info("异步画像蒸馏完成: userId={}, sessionId={}", userId, sessionId);
            } catch (Exception e) {
                log.warn("异步画像蒸馏失败，不影响对话: userId={}, sessionId={}", userId, sessionId, e);
            }
        });
    }
}
