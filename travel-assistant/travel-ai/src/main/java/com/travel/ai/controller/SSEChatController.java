package com.travel.ai.controller;

import com.travel.ai.service.AIService;
import com.travel.ai.service.ContextService;
import com.travel.ai.service.IntentService;
import com.travel.ai.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * SSE Streaming Chat Controller
 *
 * Provides a Server-Sent Events endpoint for real-time streaming AI responses.
 * The existing POST /api/ai/chat/send remains as a synchronous fallback.
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
public class SSEChatController {

    private static final int CHUNK_SIZE = 20;
    private static final int CHUNK_DELAY_MS = 30;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30L;

    private final AIService aiService;
    private final IntentService intentService;
    private final SessionService sessionService;
    private final ContextService contextService;

    public SSEChatController(AIService aiService,
                             IntentService intentService,
                             SessionService sessionService,
                             ContextService contextService) {
        this.aiService = requireNonNull(aiService, "aiService");
        this.intentService = intentService;
        this.sessionService = sessionService;
        this.contextService = contextService;
    }

    /**
     * SSE streaming endpoint for AI chat.
     * Sends the AI response in chunks as SSE events.
     *
     * @param message   user message
     * @param sessionId optional session ID
     * @param userId    optional user ID (defaults to 1)
     * @return SseEmitter that streams response chunks
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Long userId) {

        final Long effectiveUserId = (userId != null) ? userId : 1L;
        final SseEmitter emitter = new SseEmitter(-1L);

        // Heartbeat to keep connection alive
        ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();
        heartbeat.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception e) {
                heartbeat.shutdown();
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        emitter.onCompletion(() -> {
            heartbeat.shutdown();
            log.info("SSE connection completed: sessionId={}", sessionId);
        });
        emitter.onTimeout(() -> {
            heartbeat.shutdown();
            log.info("SSE connection timed out: sessionId={}", sessionId);
        });

        // Async streaming of AI response
        CompletableFuture.runAsync(() -> {
            try {
                String aiResponse = aiService.chatStream(message);

                if (aiResponse != null && !aiResponse.isEmpty()) {
                    // Send response in chunks to simulate streaming
                    for (int i = 0; i < aiResponse.length(); i += CHUNK_SIZE) {
                        int end = Math.min(i + CHUNK_SIZE, aiResponse.length());
                        String chunk = aiResponse.substring(i, end);
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(chunk));
                        Thread.sleep(CHUNK_DELAY_MS);
                    }
                }

                // Send completion event
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("[DONE]"));
                emitter.complete();

            } catch (Exception e) {
                log.error("SSE stream error: sessionId={}", sessionId, e);
                sendErrorAndComplete(emitter, e.getMessage());
            }
        });

        return emitter;
    }

    /**
     * Send an error event through the SSE channel and complete with error.
     */
    private void sendErrorAndComplete(SseEmitter emitter, String errorMessage) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(errorMessage != null ? errorMessage : "Unknown error"));
        } catch (IOException ignored) {
            // Client already disconnected
        }
        try {
            emitter.completeWithError(new RuntimeException(errorMessage));
        } catch (Exception ignored) {
            // Emitter may already be completed
        }
    }

    /**
     * Null-check helper that throws NullPointerException with a descriptive message.
     */
    private static <T> T requireNonNull(T obj, String paramName) {
        if (obj == null) {
            throw new NullPointerException(paramName + " must not be null");
        }
        return obj;
    }
}
