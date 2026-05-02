package com.travel.ai.controller;

import com.travel.ai.service.SessionService;
import com.travel.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/sessions")
@RequiredArgsConstructor
@Tag(name = "会话管理", description = "对话会话管理接口")
public class SessionController {

    private final SessionService sessionService;

    /**
     * 创建新会话
     */
    @PostMapping
    @Operation(summary = "创建会话", description = "创建新的对话会话")
    public Result<String> createSession(@RequestParam(defaultValue = "1") Long userId) {
        try {
            String sessionId = sessionService.createSession(userId);
            return Result.success(sessionId);
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return Result.error("创建会话失败");
        }
    }

    /**
     * 获取会话信息
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "获取会话", description = "获取指定会话的详细信息")
    public Result<Object> getSession(@PathVariable String sessionId) {
        try {
            Object session = sessionService.getSession(sessionId);
            if (session == null) {
                return Result.error("会话不存在");
            }
            return Result.success(session);
        } catch (Exception e) {
            log.error("获取会话失败", e);
            return Result.error("获取会话失败");
        }
    }

    /**
     * 获取用户的所有会话
     */
    @GetMapping
    @Operation(summary = "获取会话列表", description = "获取用户的所有对话会话")
    public Result<List<Object>> getUserSessions(@RequestParam(defaultValue = "1") Long userId) {
        try {
            List<Object> sessions = sessionService.getUserSessions(userId);
            return Result.success(sessions);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return Result.error("获取会话列表失败");
        }
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/{sessionId}")
    @Operation(summary = "更新会话", description = "更新会话标题")
    public Result<Void> updateSession(
            @PathVariable String sessionId,
            @RequestParam String title) {
        try {
            sessionService.updateSession(sessionId, title);
            return Result.success();
        } catch (Exception e) {
            log.error("更新会话失败", e);
            return Result.error("更新会话失败");
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "删除会话", description = "删除指定的对话会话")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        try {
            sessionService.deleteSession(sessionId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return Result.error("删除会话失败");
        }
    }

    /**
     * 清理过期会话
     */
    @PostMapping("/clean")
    @Operation(summary = "清理过期会话", description = "清理过期的对话会话")
    public Result<Integer> cleanExpiredSessions(
            @RequestParam(defaultValue = "1440") int expireMinutes) {
        try {
            int count = sessionService.cleanExpiredSessions(expireMinutes);
            return Result.success(count);
        } catch (Exception e) {
            log.error("清理过期会话失败", e);
            return Result.error("清理过期会话失败");
        }
    }

    /**
     * 检查会话是否存在
     */
    @GetMapping("/{sessionId}/exists")
    @Operation(summary = "检查会话", description = "检查会话是否存在")
    public Result<Boolean> existsSession(@PathVariable String sessionId) {
        try {
            boolean exists = sessionService.existsSession(sessionId);
            return Result.success(exists);
        } catch (Exception e) {
            log.error("检查会话失败", e);
            return Result.error("检查会话失败");
        }
    }

    /**
     * 获取会话消息数量
     */
    @GetMapping("/{sessionId}/count")
    @Operation(summary = "获取消息数量", description = "获取会话的消息数量")
    public Result<Integer> getMessageCount(@PathVariable String sessionId) {
        try {
            Integer count = sessionService.getMessageCount(sessionId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取消息数量失败", e);
            return Result.error("获取消息数量失败");
        }
    }
}
