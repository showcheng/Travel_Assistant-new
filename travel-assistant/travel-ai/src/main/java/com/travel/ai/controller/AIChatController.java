package com.travel.ai.controller;

import com.travel.ai.dto.ChatRequest;
import com.travel.ai.dto.ChatResponse;
import com.travel.ai.service.AIService;
import com.travel.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI对话控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
@Tag(name = "AI对话", description = "AI智能对话接口")
public class AIChatController {

    private final AIService aiService;

    /**
     * 发送消息并获取AI回复
     */
    @PostMapping("/send")
    @Operation(summary = "发送消息", description = "发送消息给AI并获取回复")
    public Result<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        try {
            log.info("收到对话请求: {}", request.getMessage());
            ChatResponse response = aiService.chat(request);

            if (response.getError() != null) {
                return Result.error(response.getError());
            }

            return Result.success(response);
        } catch (Exception e) {
            log.error("对话处理失败", e);
            return Result.error("对话处理失败: " + e.getMessage());
        }
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
            String history = aiService.getHistory(sessionId, limit);
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
            aiService.clearHistory(sessionId);
            return Result.success();
        } catch (Exception e) {
            log.error("清除会话历史失败", e);
            return Result.error("清除会话历史失败");
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查AI服务是否正常")
    public Result<String> health() {
        return Result.success("AI服务运行正常");
    }
}
