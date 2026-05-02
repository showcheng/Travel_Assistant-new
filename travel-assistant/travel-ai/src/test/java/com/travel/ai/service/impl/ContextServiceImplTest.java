package com.travel.ai.service.impl;

import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.entity.ConversationMessage;
import com.travel.ai.entity.ConversationSession;
import com.travel.ai.mapper.ConversationMessageMapper;
import com.travel.ai.mapper.ConversationSessionMapper;
import com.travel.ai.service.ConversationCompressor;
import com.travel.ai.service.UserProfileMemoryService;
import com.travel.common.utils.RedisUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ContextServiceImpl unit tests (TDD).
 *
 * Tests the integration of conversation summary compression into the
 * context management service. Uses Mockito mocks for all dependencies.
 *
 * Compression approach: getCompressedContext reads history, compresses
 * older messages via ConversationCompressor, and caches summary in Redis.
 * ContextServiceImpl takes 4 constructor params: (messageMapper, redisUtil, sessionMapper, compressor).
 */
class ContextServiceImplTest extends BaseServiceTest {

    @Mock
    private ConversationMessageMapper messageMapper;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private ConversationSessionMapper sessionMapper;

    @Mock
    private ConversationCompressor compressor;

    @Mock
    private UserProfileMemoryService userProfileMemoryService;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    // =====================================================================
    // Helper methods
    // =====================================================================

    /**
     * Creates a ContextServiceImpl with mocked compressor.
     */
    private ContextServiceImpl createService() {
        return new ContextServiceImpl(messageMapper, redisUtil, sessionMapper, compressor, userProfileMemoryService);
    }

    /**
     * Creates a ContextServiceImpl with a real ConversationCompressor backed
     * by a mock ChatLanguageModel (for end-to-end compression tests).
     */
    private ContextServiceImpl createServiceWithRealCompressor() {
        ConversationCompressor realCompressor = new ConversationCompressor(chatLanguageModel);
        return new ContextServiceImpl(messageMapper, redisUtil, sessionMapper, realCompressor, userProfileMemoryService);
    }

    /**
     * Builds a list of Map messages (role + content) simulating conversation rounds.
     * Each round is one user message + one assistant message.
     */
    private List<Object> buildMessageMaps(int rounds) {
        List<Object> messages = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", "User question " + (i + 1) + " about Beijing travel");
            userMsg.put("timestamp", LocalDateTime.now().minusMinutes(rounds - i).toString());
            messages.add(userMsg);

            Map<String, String> assistantMsg = new LinkedHashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", "Assistant answer " + (i + 1) + " with travel details");
            assistantMsg.put("timestamp", LocalDateTime.now().minusMinutes(rounds - i).toString());
            messages.add(assistantMsg);
        }
        return messages;
    }

    // =====================================================================
    // addMessage tests (existing behavior)
    // =====================================================================

    @Nested
    @DisplayName("addMessage: basic message storage")
    class AddMessageTests {

        @Test
        @DisplayName("addMessage stores message in Redis and sets expiry")
        void testAddMessage_StoresToRedis() {
            String sessionId = "session-1";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(1L);

            ContextServiceImpl service = createService();
            service.addMessage(sessionId, "user", "Hello");

            verify(redisUtil).lRightPush(eq(cacheKey), any(Map.class));
            verify(redisUtil).expire(eq(cacheKey), eq(24L), eq(TimeUnit.HOURS));
        }

        @Test
        @DisplayName("addMessage trims list when exceeding limit")
        void testAddMessage_TrimsWhenOverLimit() {
            String sessionId = "session-1";
            String cacheKey = "context:" + sessionId;

            // Simulate the list already having 11 elements after push
            when(redisUtil.lSize(cacheKey)).thenReturn(11L);

            ContextServiceImpl service = createService();
            service.addMessage(sessionId, "user", "Message over limit");

            verify(redisUtil).lLeftPop(cacheKey);
        }

        @Test
        @DisplayName("addMessage does not trigger compression when history <= 20 messages")
        void testAddMessage_UnderThreshold_NoCompression() {
            String sessionId = "session-nocompress";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(10L);

            ContextServiceImpl service = createService();
            service.addMessage(sessionId, "user", "Short history message");

            verify(compressor, never()).compressIfNeeded(anyList());
            verify(sessionMapper, never()).updateById(any(ConversationSession.class));
        }

        @Test
        @DisplayName("addMessage triggers compression when history exceeds 20 messages and stores summary in DB")
        void testAddMessage_OverThreshold_TriggersCompression() {
            String sessionId = "session-compress";
            String cacheKey = "context:" + sessionId;

            List<Object> allMessages = buildMessageMaps(11);
            when(redisUtil.lSize(cacheKey)).thenReturn((long) allMessages.size());
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(allMessages.subList(0, 12));

            when(compressor.compressIfNeeded(anyList())).thenReturn("Summary of earlier conversation about Beijing travel");

            ConversationSession session = new ConversationSession();
            session.setSessionId(sessionId);
            session.setMessageCount(22);
            when(sessionMapper.selectOne(any())).thenReturn(session);
            when(sessionMapper.updateById(any(ConversationSession.class))).thenReturn(1);

            ContextServiceImpl service = createService();
            service.addMessage(sessionId, "user", "New question after long history");

            verify(compressor).compressIfNeeded(anyList());

            ArgumentCaptor<ConversationSession> sessionCaptor = ArgumentCaptor.forClass(ConversationSession.class);
            verify(sessionMapper).updateById(sessionCaptor.capture());
            assertEquals("Summary of earlier conversation about Beijing travel",
                    sessionCaptor.getValue().getSummary());
        }

        @Test
        @DisplayName("addMessage does not update session when compressor returns null")
        void testAddMessage_CompressorReturnsNull_NoUpdate() {
            String sessionId = "session-null-summary";
            String cacheKey = "context:" + sessionId;

            List<Object> allMessages = buildMessageMaps(11);
            when(redisUtil.lSize(cacheKey)).thenReturn((long) allMessages.size());
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(allMessages.subList(0, 12));
            when(compressor.compressIfNeeded(anyList())).thenReturn(null);

            ContextServiceImpl service = createService();
            service.addMessage(sessionId, "user", "Trigger compression");

            verify(compressor).compressIfNeeded(anyList());
            verify(sessionMapper, never()).updateById(any(ConversationSession.class));
        }

        @Test
        @DisplayName("addMessage compression handles Redis read failure gracefully")
        void testAddMessage_CompressionRedisFailure() {
            String sessionId = "session-redis-fail";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey))
                    .thenReturn(22L)
                    .thenThrow(new RuntimeException("Redis connection lost"));

            ContextServiceImpl service = createService();
            assertDoesNotThrow(() ->
                    service.addMessage(sessionId, "user", "Message during Redis failure"));
        }
    }

    // =====================================================================
    // getFormattedContext with session summary
    // =====================================================================

    @Nested
    @DisplayName("getFormattedContext: includes summary when available")
    class FormattedContextWithSummaryTests {

        @Test
        @DisplayName("getFormattedContext includes summary from DB when session has one")
        void testGetFormattedContext_IncludesSummary() {
            String sessionId = "session-with-summary";
            String cacheKey = "context:" + sessionId;

            List<Object> recentMessages = buildMessageMaps(3);
            when(redisUtil.lSize(cacheKey)).thenReturn(6L);
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(recentMessages);

            ConversationSession session = new ConversationSession();
            session.setSessionId(sessionId);
            session.setSummary("Earlier: user asked about Beijing flights and hotels");
            when(sessionMapper.selectOne(any())).thenReturn(session);

            ContextServiceImpl service = createService();
            String context = service.getFormattedContext(sessionId, 6);

            assertNotNull(context);
            assertTrue(context.contains("Earlier: user asked about Beijing flights and hotels"),
                    "Context should include the stored summary");
            assertTrue(context.contains("User question"),
                    "Context should still include recent messages");
        }

        @Test
        @DisplayName("getFormattedContext works normally when no summary exists")
        void testGetFormattedContext_NoSummary() {
            String sessionId = "session-no-summary";
            String cacheKey = "context:" + sessionId;

            List<Object> recentMessages = buildMessageMaps(2);
            when(redisUtil.lSize(cacheKey)).thenReturn(4L);
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(recentMessages);

            when(sessionMapper.selectOne(any())).thenReturn(null);

            ContextServiceImpl service = createService();
            String context = service.getFormattedContext(sessionId, 4);

            assertNotNull(context);
            assertTrue(context.contains("User question"));
        }

        @Test
        @DisplayName("getFormattedContext with empty history returns empty string")
        void testGetFormattedContext_EmptyHistory() {
            String sessionId = "session-empty";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(0L);

            ContextServiceImpl service = createService();
            String context = service.getFormattedContext(sessionId, 10);

            assertEquals("", context);
        }
    }

    // =====================================================================
    // getCompressedContext tests
    // =====================================================================

    @Nested
    @DisplayName("getCompressedContext: summary compression integration")
    class GetCompressedContextTests {

        @Test
        @DisplayName("Short conversation (8 messages) does not trigger compression")
        void testGetCompressedContext_ShortConversation_NoCompression() {
            String sessionId = "short-session";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(8L);
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(buildMessageMaps(4));
            when(sessionMapper.selectOne(any())).thenReturn(null);

            ContextServiceImpl service = createService();
            String result = service.getCompressedContext(sessionId, 3);

            assertNotNull(result);
            assertTrue(result.contains("对话历史"), "Short conversation should use formatted context");
            assertFalse(result.contains("对话摘要"), "Short conversation should not have summary");
            verify(compressor, never()).compressIfNeeded(anyList());
        }

        @Test
        @DisplayName("Long conversation triggers compression and returns summary + recent")
        void testGetCompressedContext_LongConversation_TriggersCompression() {
            String sessionId = "long-session";
            String cacheKey = "context:" + sessionId;
            String summaryKey = "context:summary:" + sessionId;

            int totalMessages = 24;
            int recentMessageCount = 6; // 3 rounds * 2
            int olderCount = totalMessages - recentMessageCount; // 18

            when(redisUtil.lSize(cacheKey)).thenReturn((long) totalMessages);
            when(redisUtil.lRange(eq(cacheKey), eq(0L), eq((long) olderCount - 1)))
                    .thenReturn(buildMessageMaps(olderCount / 2));
            when(redisUtil.lRange(eq(cacheKey), eq((long) -recentMessageCount), eq(-1L)))
                    .thenReturn(buildMessageMaps(recentMessageCount / 2));
            when(redisUtil.get(summaryKey)).thenReturn(null);
            when(compressor.compressIfNeeded(anyList())).thenReturn("Summary of earlier conversation");

            ContextServiceImpl service = createService();
            String result = service.getCompressedContext(sessionId, 3);

            assertNotNull(result);
            assertTrue(result.contains("对话摘要"), "Long conversation should include summary");
            assertTrue(result.contains("最近对话"), "Long conversation should include recent messages");
            verify(compressor).compressIfNeeded(anyList());
            verify(redisUtil).set(eq(summaryKey), eq("Summary of earlier conversation"));
        }

        @Test
        @DisplayName("Cached summary is used without calling compressor")
        void testGetCompressedContext_CachedSummary_NoCompressorCall() {
            String sessionId = "cached-session";
            String cacheKey = "context:" + sessionId;
            String summaryKey = "context:summary:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(24L);
            when(redisUtil.lRange(eq(cacheKey), eq(0L), eq(17L)))
                    .thenReturn(buildMessageMaps(9));
            when(redisUtil.lRange(eq(cacheKey), eq(-6L), eq(-1L)))
                    .thenReturn(buildMessageMaps(3));
            when(redisUtil.get(summaryKey)).thenReturn("Cached summary here");

            ContextServiceImpl service = createService();
            String result = service.getCompressedContext(sessionId, 3);

            assertTrue(result.contains("Cached summary here"));
            verify(compressor, never()).compressIfNeeded(anyList());
        }

        @Test
        @DisplayName("Empty session returns empty string")
        void testGetCompressedContext_EmptySession() {
            String sessionId = "empty-session";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(0L);

            ContextServiceImpl service = createService();
            assertEquals("", service.getCompressedContext(sessionId, 3));
        }

        @Test
        @DisplayName("Non-existent session (null size) returns empty string")
        void testGetCompressedContext_NullSession() {
            String sessionId = "null-session";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(null);

            ContextServiceImpl service = createService();
            assertEquals("", service.getCompressedContext(sessionId, 3));
        }

        @Test
        @DisplayName("Redis exception falls back to formatted context")
        void testGetCompressedContext_RedisException_Fallback() {
            String sessionId = "error-session";
            String cacheKey = "context:" + sessionId;

            // First lSize throws, fallback getFormattedContext also calls lSize -> return 0
            when(redisUtil.lSize(cacheKey))
                    .thenThrow(new RuntimeException("Redis error"))
                    .thenReturn(0L);

            ContextServiceImpl service = createService();
            String result = service.getCompressedContext(sessionId, 3);

            assertNotNull(result, "Fallback should return non-null result");
        }
    }

    // =====================================================================
    // Compression fallback on LLM failure (end-to-end with real compressor)
    // =====================================================================

    @Nested
    @DisplayName("End-to-end compression with real ConversationCompressor")
    class EndToEndCompressionTests {

        @Test
        @DisplayName("LLM call fails: fallback to first-sentence extraction")
        void testCompressHistory_FallbackOnLLMFailure() {
            String sessionId = "session-llm-fail";
            String cacheKey = "context:" + sessionId;
            String summaryKey = "context:summary:" + sessionId;

            // 24 messages total, 3 rounds = 6 recent, 18 older
            when(redisUtil.lSize(cacheKey)).thenReturn(24L);
            when(redisUtil.lRange(eq(cacheKey), eq(0L), eq(17L)))
                    .thenReturn(buildMessageMaps(9));
            when(redisUtil.lRange(eq(cacheKey), eq(-6L), eq(-1L)))
                    .thenReturn(buildMessageMaps(3));
            when(redisUtil.get(summaryKey)).thenReturn(null);

            // LLM throws exception
            when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("API timeout"));

            ContextServiceImpl service = createServiceWithRealCompressor();
            String result = service.getCompressedContext(sessionId, 3);

            assertNotNull(result);
            assertTrue(result.contains("最近对话"), "Should still include recent messages");
        }

        @Test
        @DisplayName("LLM returns valid summary: summary is cached and used")
        void testCompressHistory_LLMReturnsValidSummary() {
            String sessionId = "session-llm-ok";
            String cacheKey = "context:" + sessionId;
            String summaryKey = "context:summary:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(24L);
            when(redisUtil.lRange(eq(cacheKey), eq(0L), eq(17L)))
                    .thenReturn(buildMessageMaps(9));
            when(redisUtil.lRange(eq(cacheKey), eq(-6L), eq(-1L)))
                    .thenReturn(buildMessageMaps(3));
            when(redisUtil.get(summaryKey)).thenReturn(null);

            when(chatLanguageModel.generate(anyString()))
                    .thenReturn("User discussed Beijing travel plans, asked about flights and hotels.");

            ContextServiceImpl service = createServiceWithRealCompressor();
            String result = service.getCompressedContext(sessionId, 3);

            assertNotNull(result);
            assertTrue(result.contains("Beijing travel plans"),
                    "Summary should preserve key entities from the conversation");
            verify(redisUtil).set(eq(summaryKey), contains("Beijing travel plans"));
        }
    }

    // =====================================================================
    // saveMessage tests
    // =====================================================================

    @Nested
    @DisplayName("saveMessage: database persistence")
    class SaveMessageTests {

        @Test
        @DisplayName("saveMessage persists message to database via mapper")
        void testSaveMessage_PersistsToDB() {
            when(messageMapper.insert(any(ConversationMessage.class))).thenReturn(1);

            ContextServiceImpl service = createService();
            service.saveMessage("session-1", 100L, "user", "Hello", "greeting");

            verify(messageMapper).insert(any(ConversationMessage.class));
        }

        @Test
        @DisplayName("saveMessage handles database error gracefully")
        void testSaveMessage_HandlesDBError() {
            when(messageMapper.insert(any(ConversationMessage.class))).thenThrow(new RuntimeException("DB error"));

            ContextServiceImpl service = createService();
            assertDoesNotThrow(() ->
                    service.saveMessage("session-1", 100L, "user", "Hello", "greeting"));
        }
    }

    // =====================================================================
    // clearContext tests
    // =====================================================================

    @Test
    @DisplayName("clearContext deletes Redis cache for session")
    void testClearContext_DeletesCache() {
        String sessionId = "session-clear";
        String cacheKey = "context:" + sessionId;

        when(redisUtil.delete(cacheKey)).thenReturn(true);

        ContextServiceImpl service = createService();
        service.clearContext(sessionId);

        verify(redisUtil).delete(cacheKey);
    }

    // =====================================================================
    // resolveReference tests
    // =====================================================================

    @Nested
    @DisplayName("resolveReference: pronoun resolution")
    class ResolveReferenceTests {

        @Test
        @DisplayName("Resolves pronoun '这个' with context")
        void testResolveReference_WithPronoun() {
            List<Map<String, String>> context = new ArrayList<>();
            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", "北京的故宫怎么样");
            context.add(userMsg);

            ContextServiceImpl service = createService();
            String resolved = service.resolveReference("这个多少钱", context);

            assertTrue(resolved.contains("北京"), "Should resolve pronoun using context");
        }

        @Test
        @DisplayName("Returns original text when no pronoun detected")
        void testResolveReference_NoPronoun() {
            List<Map<String, String>> context = new ArrayList<>();
            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", "北京的故宫怎么样");
            context.add(userMsg);

            ContextServiceImpl service = createService();
            String resolved = service.resolveReference("上海的迪士尼怎么走", context);

            assertEquals("上海的迪士尼怎么走", resolved);
        }

        @Test
        @DisplayName("Returns original text when context is null")
        void testResolveReference_NullContext() {
            ContextServiceImpl service = createService();
            assertEquals("Hello", service.resolveReference("Hello", null));
        }

        @Test
        @DisplayName("Returns original text when context is empty")
        void testResolveReference_EmptyContext() {
            ContextServiceImpl service = createService();
            assertEquals("Hello", service.resolveReference("Hello", List.of()));
        }
    }

    // =====================================================================
    // isContextExpired tests
    // =====================================================================

    @Nested
    @DisplayName("isContextExpired: TTL checks")
    class IsContextExpiredTests {

        @Test
        @DisplayName("Returns true when TTL is below threshold")
        void testIsContextExpired_TTLBelowThreshold() {
            String cacheKey = "context:session-exp";
            when(redisUtil.getExpire(cacheKey)).thenReturn(30L);

            ContextServiceImpl service = createService();
            assertTrue(service.isContextExpired("session-exp", 5));
        }

        @Test
        @DisplayName("Returns false when TTL is above threshold")
        void testIsContextExpired_TTLAboveThreshold() {
            String cacheKey = "context:session-ok";
            when(redisUtil.getExpire(cacheKey)).thenReturn(600L);

            ContextServiceImpl service = createService();
            assertFalse(service.isContextExpired("session-ok", 5));
        }

        @Test
        @DisplayName("Returns false when TTL is -1 (no expiry)")
        void testIsContextExpired_NoExpiry() {
            String cacheKey = "context:session-forever";
            when(redisUtil.getExpire(cacheKey)).thenReturn(-1L);

            ContextServiceImpl service = createService();
            assertFalse(service.isContextExpired("session-forever", 5));
        }
    }

    // =====================================================================
    // getRecentRounds tests
    // =====================================================================

    @Nested
    @DisplayName("getRecentRounds: round-based retrieval")
    class GetRecentRoundsTests {

        @Test
        @DisplayName("Returns recent N rounds correctly")
        void testGetRecentRounds_ReturnsCorrectRounds() {
            String sessionId = "session-rounds";
            String cacheKey = "context:" + sessionId;

            List<Object> allMessages = buildMessageMaps(5);
            when(redisUtil.lSize(cacheKey)).thenReturn(10L);
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(allMessages);

            ContextServiceImpl service = createService();
            List<Map<String, String>> result = service.getRecentRounds(sessionId, 3);

            assertNotNull(result);
            assertTrue(result.size() <= 6);
        }

        @Test
        @DisplayName("Returns empty list for non-existent session")
        void testGetRecentRounds_NoHistory() {
            String sessionId = "session-empty";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(0L);

            ContextServiceImpl service = createService();
            List<Map<String, String>> result = service.getRecentRounds(sessionId, 3);

            assertTrue(result.isEmpty());
        }
    }
}
