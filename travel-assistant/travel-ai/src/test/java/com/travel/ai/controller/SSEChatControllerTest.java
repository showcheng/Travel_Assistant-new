package com.travel.ai.controller;

import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.service.AIService;
import com.travel.ai.service.ContextService;
import com.travel.ai.service.IntentService;
import com.travel.ai.service.SessionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SSE streaming chat controller unit tests.
 * TDD: these tests define the contract for SSEChatController.
 * Uses lenient stubbing because async CompletableFuture may not
 * consume all stubs before the synchronous test assertions complete.
 */
class SSEChatControllerTest extends BaseServiceTest {

    @Mock
    private AIService aiService;

    @Mock
    private IntentService intentService;

    @Mock
    private SessionService sessionService;

    @Mock
    private ContextService contextService;

    /**
     * Test 1: streamChat returns a non-null SseEmitter with infinite timeout.
     */
    @Test
    void testStreamChat_ReturnsSseEmitter() {
        // Arrange
        lenient().when(aiService.chatStream(anyString())).thenReturn("Hello from AI");
        SSEChatController controller = new SSEChatController(
                aiService, intentService, sessionService, contextService);

        // Act
        SseEmitter emitter = controller.streamChat("你好", "session123", 1L);

        // Assert
        assertNotNull(emitter, "SseEmitter should not be null");
        assertEquals(-1L, emitter.getTimeout(), "SseEmitter should have infinite timeout");
    }

    /**
     * Test 2: streamChat calls AIService.chatStream with the user message.
     */
    @Test
    void testStreamChat_CallsAIServiceChatStream() {
        // Arrange
        lenient().when(aiService.chatStream(anyString())).thenReturn("AI response");
        SSEChatController controller = new SSEChatController(
                aiService, intentService, sessionService, contextService);

        // Act
        controller.streamChat("推荐景点", "session456", 2L);

        // Assert - allow async call to complete
        verify(aiService, timeout(2000)).chatStream("推荐景点");
    }

    /**
     * Test 3: streamChat still returns SseEmitter when AI service throws exception.
     * The emitter itself should not throw; the error is sent through the SSE channel.
     */
    @Test
    void testStreamChat_HandlesAIServiceError() {
        // Arrange
        lenient().when(aiService.chatStream(anyString()))
                .thenThrow(new RuntimeException("AI service unavailable"));
        SSEChatController controller = new SSEChatController(
                aiService, intentService, sessionService, contextService);

        // Act - should NOT throw, error is handled internally
        SseEmitter emitter = controller.streamChat("test", "session789", 1L);

        // Assert
        assertNotNull(emitter, "SseEmitter should still be returned on error");
    }

    /**
     * Test 4: streamChat uses default userId when null is passed.
     */
    @Test
    void testStreamChat_DefaultUserId() {
        // Arrange
        lenient().when(aiService.chatStream(anyString())).thenReturn("OK");
        SSEChatController controller = new SSEChatController(
                aiService, intentService, sessionService, contextService);

        // Act
        SseEmitter emitter = controller.streamChat("hello", "s1", null);

        // Assert
        assertNotNull(emitter);
        verify(aiService, timeout(2000)).chatStream("hello");
    }

    /**
     * Test 5: streamChat handles empty sessionId gracefully.
     */
    @Test
    void testStreamChat_EmptySessionId() {
        // Arrange
        lenient().when(aiService.chatStream(anyString())).thenReturn("Hello");
        SSEChatController controller = new SSEChatController(
                aiService, intentService, sessionService, contextService);

        // Act
        SseEmitter emitter = controller.streamChat("你好", "", 1L);

        // Assert
        assertNotNull(emitter);
        verify(aiService, timeout(2000)).chatStream("你好");
    }

    /**
     * Test 6: streamChat handles empty AI response.
     */
    @Test
    void testStreamChat_EmptyAIResponse() {
        // Arrange
        lenient().when(aiService.chatStream(anyString())).thenReturn("");
        SSEChatController controller = new SSEChatController(
                aiService, intentService, sessionService, contextService);

        // Act
        SseEmitter emitter = controller.streamChat("test", "s1", 1L);

        // Assert
        assertNotNull(emitter);
        verify(aiService, timeout(2000)).chatStream("test");
    }

    /**
     * Test 7: constructor rejects null AIService.
     */
    @Test
    void testConstructor_RejectsNullAIService() {
        assertThrows(NullPointerException.class, () ->
                new SSEChatController(null, intentService, sessionService, contextService));
    }
}
