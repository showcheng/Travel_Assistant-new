package com.travel.ai.service.impl;

import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.mapper.ConversationMessageMapper;
import com.travel.ai.mapper.ConversationSessionMapper;
import com.travel.ai.service.ConversationCompressor;
import com.travel.ai.service.UserProfileMemoryService;
import com.travel.common.utils.RedisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ContextService + UserProfileMemory integration tests (TDD).
 *
 * Tests the integration of UserProfileMemoryService into ContextService,
 * verifying that buildFullContext correctly combines compressed conversation
 * context with user profile context.
 *
 * Red phase: These tests define the expected behavior before implementation.
 */
class ContextServiceMemoryIntegrationTest extends BaseServiceTest {

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

    // =====================================================================
    // Helper methods
    // =====================================================================

    /**
     * Creates a ContextServiceImpl with all mocked dependencies including
     * UserProfileMemoryService.
     */
    private ContextServiceImpl createService() {
        return new ContextServiceImpl(
                messageMapper, redisUtil, sessionMapper, compressor, userProfileMemoryService
        );
    }

    /**
     * Builds a list of Map messages simulating conversation rounds.
     */
    private List<Object> buildMessageMaps(int rounds) {
        List<Object> messages = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", "User question " + (i + 1));
            userMsg.put("timestamp", LocalDateTime.now().minusMinutes(rounds - i).toString());
            messages.add(userMsg);

            Map<String, String> assistantMsg = new LinkedHashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", "Assistant answer " + (i + 1));
            assistantMsg.put("timestamp", LocalDateTime.now().minusMinutes(rounds - i).toString());
            messages.add(assistantMsg);
        }
        return messages;
    }

    /**
     * Sets up Redis mocks for a session with the given number of messages.
     */
    private void setupRedisMocks(String sessionId, int totalMessages) {
        String cacheKey = "context:" + sessionId;
        when(redisUtil.lSize(cacheKey)).thenReturn((long) totalMessages);
        when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                .thenReturn(buildMessageMaps(totalMessages / 2));
    }

    // =====================================================================
    // buildFullContext tests
    // =====================================================================

    @Nested
    @DisplayName("buildFullContext: combines conversation context with user profile")
    class BuildFullContextTests {

        @Test
        @DisplayName("Returns both compressed context and profile context combined")
        void testBuildFullContext_ReturnsBothContexts() {
            String sessionId = "session-full";
            String userId = "user-123";
            String cacheKey = "context:" + sessionId;

            // Short conversation - no compression needed
            when(redisUtil.lSize(cacheKey)).thenReturn(8L);
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(buildMessageMaps(4));

            // User profile returns meaningful context
            String profileContext = "[用户画像] 偏好自然风光，预算3000-5000元，喜欢自助游";
            when(userProfileMemoryService.getProfileContext(userId)).thenReturn(profileContext);

            ContextServiceImpl service = createService();
            String result = service.buildFullContext(sessionId, userId, 3);

            assertNotNull(result, "buildFullContext should never return null");
            assertTrue(result.contains("User question"), "Should include conversation context");
            assertTrue(result.contains(profileContext),
                    "Should include user profile context");
        }

        @Test
        @DisplayName("Properly formats sections with clear headers")
        void testBuildFullContext_HasSectionHeaders() {
            String sessionId = "session-headers";
            String userId = "user-456";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(6L);
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(buildMessageMaps(3));

            when(userProfileMemoryService.getProfileContext(userId))
                    .thenReturn("[用户画像] 偏好文化古迹");

            ContextServiceImpl service = createService();
            String result = service.buildFullContext(sessionId, userId, 3);

            // Should have clear section separation
            assertTrue(result.contains("用户画像") || result.contains("画像"),
                    "Should contain profile section header");
        }

        @Test
        @DisplayName("With no user profile returns only conversation context")
        void testBuildFullContext_NoUserProfile_ReturnsOnlyConversation() {
            String sessionId = "session-no-profile";
            String userId = "user-noprofile";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(6L);
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(buildMessageMaps(3));

            // No profile available - service returns default "no profile" text
            when(userProfileMemoryService.getProfileContext(userId))
                    .thenReturn("暂无用户画像信息。");

            ContextServiceImpl service = createService();
            String result = service.buildFullContext(sessionId, userId, 3);

            assertNotNull(result, "Should return non-null even without profile");
            assertTrue(result.contains("User question"),
                    "Should still include conversation context");
        }

        @Test
        @DisplayName("With null userId still returns conversation context")
        void testBuildFullContext_NullUserId_ReturnsConversationContext() {
            String sessionId = "session-null-user";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(4L);
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(buildMessageMaps(2));

            ContextServiceImpl service = createService();
            String result = service.buildFullContext(sessionId, null, 3);

            assertNotNull(result);
            assertTrue(result.contains("User question"));
            // Should not call profile service for null userId
            verify(userProfileMemoryService, never()).getProfileContext(anyString());
        }

        @Test
        @DisplayName("With empty userId still returns conversation context")
        void testBuildFullContext_EmptyUserId_ReturnsConversationContext() {
            String sessionId = "session-empty-user";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(4L);
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(buildMessageMaps(2));

            ContextServiceImpl service = createService();
            String result = service.buildFullContext(sessionId, "", 3);

            assertNotNull(result);
            assertTrue(result.contains("User question"));
            verify(userProfileMemoryService, never()).getProfileContext(anyString());
        }

        @Test
        @DisplayName("Empty session and no profile returns empty string")
        void testBuildFullContext_EmptySession_NoProfile() {
            String sessionId = "session-empty-all";
            String userId = "user-empty";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(0L);

            ContextServiceImpl service = createService();
            String result = service.buildFullContext(sessionId, userId, 3);

            assertNotNull(result);
            // Empty session should still attempt to get profile or return empty
            verify(userProfileMemoryService).getProfileContext(userId);
        }

        @Test
        @DisplayName("Profile service exception does not crash buildFullContext")
        void testBuildFullContext_ProfileServiceException_GracefulDegradation() {
            String sessionId = "session-profile-error";
            String userId = "user-error";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(6L);
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(buildMessageMaps(3));

            when(userProfileMemoryService.getProfileContext(userId))
                    .thenThrow(new RuntimeException("Profile service unavailable"));

            ContextServiceImpl service = createService();
            // Should NOT throw - graceful degradation
            assertDoesNotThrow(() -> service.buildFullContext(sessionId, userId, 3));
        }

        @Test
        @DisplayName("Long conversation triggers compression and profile is appended")
        void testBuildFullContext_LongConversation_CompressionWithProfile() {
            String sessionId = "session-long-with-profile";
            String userId = "user-long";
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

            when(userProfileMemoryService.getProfileContext(userId))
                    .thenReturn("[用户画像] 偏好海岛游，预算8000-10000元");

            ContextServiceImpl service = createService();
            String result = service.buildFullContext(sessionId, userId, 3);

            assertNotNull(result);
            assertTrue(result.contains("对话摘要"), "Should include conversation summary");
            assertTrue(result.contains("最近对话"), "Should include recent messages");
            assertTrue(result.contains("海岛游"), "Should include user profile preferences");
        }

        @Test
        @DisplayName("Profile context appears before conversation context")
        void testBuildFullContext_ProfileComesBeforeConversation() {
            String sessionId = "session-order";
            String userId = "user-order";
            String cacheKey = "context:" + sessionId;

            when(redisUtil.lSize(cacheKey)).thenReturn(6L);
            when(redisUtil.lRange(eq(cacheKey), anyLong(), anyLong()))
                    .thenReturn(buildMessageMaps(3));

            when(userProfileMemoryService.getProfileContext(userId))
                    .thenReturn("[用户画像] 测试画像内容");

            ContextServiceImpl service = createService();
            String result = service.buildFullContext(sessionId, userId, 3);

            assertNotNull(result);
            int profileIndex = result.indexOf("测试画像内容");
            int conversationIndex = result.indexOf("User question");
            assertTrue(profileIndex >= 0, "Profile content should be present");
            assertTrue(conversationIndex >= 0, "Conversation content should be present");
            assertTrue(profileIndex < conversationIndex,
                    "Profile context should appear before conversation context");
        }
    }
}
