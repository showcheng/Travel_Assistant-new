package com.travel.ai.service;

import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.mapper.ConversationMessageMapper;
import com.travel.ai.mapper.ConversationSessionMapper;
import com.travel.ai.service.impl.ContextServiceImpl;
import com.travel.common.utils.RedisUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ContextServiceImpl.getCompressedContext 集成测试 (TDD)
 *
 * 验证对话摘要压缩在上下文管理服务中的集成行为：
 * - 短对话（不超过阈值）不触发压缩
 * - 长对话触发压缩，摘要 + 最近对话拼接返回
 * - 摘要缓存命中时跳过 LLM 调用
 * - Redis 异常时降级到普通上下文
 */
class CompressedContextIntegrationTest extends BaseServiceTest {

    @Mock
    private ConversationMessageMapper messageMapper;

    @Mock
    private ConversationSessionMapper sessionMapper;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Mock
    private UserProfileMemoryService userProfileMemoryService;

    private ContextServiceImpl createContextService() {
        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
        return new ContextServiceImpl(messageMapper, redisUtil, sessionMapper, compressor, userProfileMemoryService);
    }

    // ========== 短对话：不触发压缩 ==========

    @Test
    @DisplayName("短对话（8条消息）不触发压缩，返回普通格式化上下文")
    void testGetCompressedContext_ShortConversation_NoCompression() {
        ContextServiceImpl contextService = createContextService();

        String sessionId = "short-session";
        String cacheKey = "context:" + sessionId;

        // Mock Redis: 8 messages
        when(redisUtil.lSize(cacheKey)).thenReturn(8L);
        when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                .thenReturn(createMockMessages(8));
        when(sessionMapper.selectOne(any())).thenReturn(null);

        String result = contextService.getCompressedContext(sessionId, 3);

        assertNotNull(result, "应返回非空上下文");
        assertTrue(result.contains("对话历史"), "短对话应使用普通格式化上下文");
        assertFalse(result.contains("对话摘要"), "短对话不应包含摘要标记");
    }

    // ========== 长对话：触发压缩 ==========

    @Test
    @DisplayName("长对话（24条消息）触发压缩，返回摘要+最近对话")
    void testGetCompressedContext_LongConversation_TriggersCompression() {
        when(chatLanguageModel.generate(anyString())).thenReturn("用户询问了北京旅游相关问题");
        ContextServiceImpl contextService = createContextService();

        String sessionId = "long-session";
        String cacheKey = "context:" + sessionId;
        String summaryKey = "context:summary:" + sessionId;

        // 24 messages total, 3 recent rounds = 6 recent messages
        // olderCount = 24 - 6 = 18 > 10 => triggers compression
        int totalMessages = 24;
        int recentMessageCount = 6; // 3 rounds * 2
        int olderCount = totalMessages - recentMessageCount; // 18

        when(redisUtil.lSize(cacheKey)).thenReturn((long) totalMessages);
        // Older messages: lRange(cacheKey, 0, olderCount-1) = lRange(0, 17)
        when(redisUtil.lRange(eq(cacheKey), eq(0L), eq((long) olderCount - 1)))
                .thenReturn(createMockMessages(olderCount));
        // Recent messages: getHistory calls lRange(cacheKey, -6, -1)
        when(redisUtil.lRange(eq(cacheKey), eq((long) -recentMessageCount), eq(-1L)))
                .thenReturn(createMockMessages(recentMessageCount));
        // No cached summary
        when(redisUtil.get(summaryKey)).thenReturn(null);

        String result = contextService.getCompressedContext(sessionId, 3);

        assertNotNull(result, "应返回非空上下文");
        assertTrue(result.contains("对话摘要"), "长对话应包含摘要标记");
        assertTrue(result.contains("最近对话"), "长对话应包含最近对话标记");
        assertTrue(result.contains("北京旅游"), "摘要应包含关键内容");
    }

    // ========== 摘要缓存命中 ==========

    @Test
    @DisplayName("缓存命中时不调用LLM，直接使用缓存摘要")
    void testGetCompressedContext_CachedSummary_NoLLMCall() {
        ContextServiceImpl contextService = createContextService();

        String sessionId = "cached-session";
        String cacheKey = "context:" + sessionId;
        String summaryKey = "context:summary:" + sessionId;

        int totalMessages = 24;
        int recentMessageCount = 6;

        when(redisUtil.lSize(cacheKey)).thenReturn((long) totalMessages);
        when(redisUtil.lRange(eq(cacheKey), eq(0L), eq((long) (totalMessages - recentMessageCount) - 1)))
                .thenReturn(createMockMessages(totalMessages - recentMessageCount));
        when(redisUtil.lRange(eq(cacheKey), eq((long) -recentMessageCount), eq(-1L)))
                .thenReturn(createMockMessages(recentMessageCount));
        // Cached summary exists
        when(redisUtil.get(summaryKey)).thenReturn("缓存中的对话摘要");

        String result = contextService.getCompressedContext(sessionId, 3);

        assertNotNull(result);
        assertTrue(result.contains("缓存中的对话摘要"), "应使用缓存摘要");
        // LLM should NOT be called
        verify(chatLanguageModel, never()).generate(anyString());
    }

    // ========== 空会话 ==========

    @Test
    @DisplayName("空会话返回空字符串")
    void testGetCompressedContext_EmptySession() {
        ContextServiceImpl contextService = createContextService();

        String sessionId = "empty-session";
        String cacheKey = "context:" + sessionId;

        when(redisUtil.lSize(cacheKey)).thenReturn(0L);

        String result = contextService.getCompressedContext(sessionId, 3);

        assertEquals("", result, "空会话应返回空字符串");
    }

    @Test
    @DisplayName("会话不存在（Redis返回null）返回空字符串")
    void testGetCompressedContext_NullSession() {
        ContextServiceImpl contextService = createContextService();

        String sessionId = "null-session";
        String cacheKey = "context:" + sessionId;

        when(redisUtil.lSize(cacheKey)).thenReturn(null);

        String result = contextService.getCompressedContext(sessionId, 3);

        assertEquals("", result, "不存在的会话应返回空字符串");
    }

    // ========== 降级场景 ==========

    @Test
    @DisplayName("Redis异常时降级到普通格式化上下文")
    void testGetCompressedContext_RedisException_Fallback() {
        ContextServiceImpl contextService = createContextService();

        String sessionId = "error-session";
        String cacheKey = "context:" + sessionId;

        // First call in try block: lSize throws
        // Fallback getFormattedContext also calls lSize -> return 0 (empty)
        when(redisUtil.lSize(cacheKey))
                .thenThrow(new RuntimeException("Redis connection failed"))
                .thenReturn(0L);

        String result = contextService.getCompressedContext(sessionId, 3);

        assertNotNull(result, "异常降级应返回非空结果");
    }

    // ========== Helper methods ==========

    /**
     * 创建模拟消息列表
     */
    private List<Object> createMockMessages(int count) {
        List<Object> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, String> msg = new LinkedHashMap<>();
            msg.put("role", i % 2 == 0 ? "user" : "assistant");
            msg.put("content", "消息内容" + (i + 1) + "：北京旅游相关话题");
            msg.put("timestamp", "2026-05-02T10:00:0" + (i % 10));
            messages.add(msg);
        }
        return messages;
    }
}
