package com.travel.ai.service;

import com.travel.ai.dto.ChatResponse;
import com.travel.ai.dto.RAGSearchRequest;
import com.travel.ai.dto.RAGSearchResponse;
import com.travel.ai.dto.SourceInfo;
import com.travel.ai.dto.RAGSearchResponse.DocumentChunk;
import com.travel.ai.enums.IntentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import java.util.Map;

/**
 * RAG Chat Pipeline Service
 *
 * Orchestrates the full Retrieval-Augmented Generation pipeline:
 *   message -> intent recognition -> dynamic threshold/topK
 *   -> knowledge base search -> context-enhanced prompt -> LLM generation
 *   -> response with source citations
 *
 * This is the integration layer that connects the previously-disconnected
 * knowledge base search to the chat flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGChatService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final IntentService intentService;
    private final DynamicThresholdService dynamicThresholdService;
    private final EnhancedContextService enhancedContextService;
    private final AIService aiService;
    private final UserProfileMemoryService userProfileMemoryService;

    /**
     * Process a user message through the RAG pipeline.
     *
     * Flow:
     * 1. Recognize intent
     * 2. Skip RAG for intents that don't need it (e.g., GREETING)
     * 3. Determine dynamic threshold and topK based on intent
     * 4. Search knowledge base with those parameters
     * 5. Build enhanced prompt with retrieved chunks + user profile
     * 6. Generate AI response using the enhanced prompt
     * 7. Attach source citations to the response
     *
     * On RAG search failure, falls back to direct chat without sources.
     *
     * @param message   the user's message
     * @param sessionId the conversation session ID
     * @return ChatResponse with AI reply and optional source citations
     */
    public ChatResponse chatWithRAG(String message, String sessionId) {
        return chatWithRAG(message, sessionId, null);
    }

    /**
     * Process a user message through the RAG pipeline with user profile personalization.
     *
     * @param message   the user's message
     * @param sessionId the conversation session ID
     * @param userId    the user ID (nullable; when null, no profile is injected)
     * @return ChatResponse with AI reply and optional source citations
     */
    public ChatResponse chatWithRAG(String message, String sessionId, String userId) {
        // 1. Recognize intent
        IntentType intent = intentService.recognizeIntent(message);
        log.info("RAG Pipeline: intent={} for message='{}'", intent, message);

        // 2. Get user profile context if userId is available
        String profileContext = null;
        if (userId != null && !userId.isBlank()) {
            try {
                profileContext = userProfileMemoryService.getProfileContext(userId);
                if ("暂无用户画像信息。".equals(profileContext)) {
                    profileContext = null;
                }
            } catch (Exception e) {
                log.warn("获取用户画像失败，跳过个性化注入: userId={}", userId, e);
            }
        }

        // 3. Check if RAG is needed for this intent
        if (!dynamicThresholdService.needsRAG(intent)) {
            log.debug("Intent {} does not require RAG, using direct chat", intent);
            String prompt = injectProfileIntoMessage(message, profileContext);
            String directReply = aiService.chatStream(prompt);
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .response(directReply)
                    .intentType(intent.getCode())
                    .tokens(directReply.length())
                    .timestamp(LocalDateTime.now())
                    .finished(true)
                    .build();
        }

        // 4. Get dynamic threshold and topK for this intent
        double threshold = dynamicThresholdService.getThreshold(intent);
        int topK = dynamicThresholdService.getTopK(intent);
        log.debug("RAG parameters: intent={}, threshold={}, topK={}", intent, threshold, topK);

        try {
            // 5. Search knowledge base
            RAGSearchRequest searchRequest = new RAGSearchRequest();
            searchRequest.setQuery(message);
            searchRequest.setThreshold(threshold);
            searchRequest.setTopK(topK);

            RAGSearchResponse searchResponse = knowledgeBaseService.search(searchRequest);
            List<DocumentChunk> chunks = searchResponse.getChunks() != null
                    ? searchResponse.getChunks()
                    : Collections.emptyList();
            log.info("RAG search returned {} chunks", chunks.size());

            // 6. Build enhanced prompt with retrieved context + user profile
            String enhancedPrompt = enhancedContextService.buildPromptWithContext(chunks, message);
            if (profileContext != null && !profileContext.isBlank()) {
                enhancedPrompt = "[用户画像] " + profileContext + "\n\n" + enhancedPrompt;
            }

            // 7. Generate AI response using enhanced prompt
            String aiResponse = aiService.chatStream(enhancedPrompt);

            // 8. Build source citations
            List<SourceInfo> sources = enhancedContextService.buildSourceInfoList(chunks);

            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .response(aiResponse)
                    .intentType(intent.getCode())
                    .tokens(aiResponse.length())
                    .timestamp(LocalDateTime.now())
                    .finished(true)
                    .sources(sources)
                    .build();

        } catch (Exception e) {
            // Fallback to direct chat on RAG failure
            log.warn("RAG search failed, falling back to direct chat: {}", e.getMessage());
            String prompt = injectProfileIntoMessage(message, profileContext);
            String fallbackReply = aiService.chatStream(prompt);
            return ChatResponse.builder()
                    .sessionId(sessionId)
                    .response(fallbackReply)
                    .intentType(intent.getCode())
                    .tokens(fallbackReply.length())
                    .timestamp(LocalDateTime.now())
                    .finished(true)
                    .build();
        }
    }

    /**
     * Inject user profile context into the message prompt.
     * Prepends a brief profile section so the LLM can personalize its response.
     */
    private String injectProfileIntoMessage(String message, String profileContext) {
        if (profileContext == null || profileContext.isBlank()) {
            return message;
        }
        return "[用户画像] " + profileContext + "\n\n用户问题：" + message;
    }
}
