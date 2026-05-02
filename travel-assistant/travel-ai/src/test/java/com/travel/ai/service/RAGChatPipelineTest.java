package com.travel.ai.service;

import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.dto.ChatResponse;
import com.travel.ai.dto.RAGSearchRequest;
import com.travel.ai.dto.RAGSearchResponse;
import com.travel.ai.dto.SourceInfo;
import com.travel.ai.dto.RAGSearchResponse.DocumentChunk;
import com.travel.ai.enums.IntentType;
import com.travel.ai.service.RAGChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RAG Chat Pipeline unit tests (STRICT TDD - RED phase).
 *
 * Tests the complete RAG pipeline orchestrated by RAGChatService:
 *   message -> intent -> threshold/topK -> knowledgeBaseService.search()
 *   -> enhancedContextService.buildPromptWithContext() -> aiService.chatStream()
 *   -> response with sources
 */
class RAGChatPipelineTest extends BaseServiceTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @Mock
    private IntentService intentService;

    @Mock
    private DynamicThresholdService dynamicThresholdService;

    @Mock
    private EnhancedContextService enhancedContextService;

    @Mock
    private AIService aiService;

    @InjectMocks
    private RAGChatService ragChatService;

    private static final String SESSION_ID = "test-session-001";

    // ========== Test 1: GREETING intent skips RAG entirely ==========

    @Test
    @DisplayName("GREETING intent should skip RAG search entirely")
    void testGreeting_SkipsRAG() {
        // Given
        when(intentService.recognizeIntent("你好")).thenReturn(IntentType.GREETING);
        when(dynamicThresholdService.needsRAG(IntentType.GREETING)).thenReturn(false);
        when(aiService.chatStream("你好")).thenReturn("您好！有什么可以帮您？");

        // When
        ChatResponse response = ragChatService.chatWithRAG("你好", SESSION_ID);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals(SESSION_ID, response.getSessionId());
        assertEquals("您好！有什么可以帮您？", response.getResponse());
        assertEquals("greeting", response.getIntentType());
        assertTrue(response.getFinished());

        // RAG search should NEVER be called for GREETING
        verify(knowledgeBaseService, never()).search(any(RAGSearchRequest.class));
        verify(enhancedContextService, never()).buildPromptWithContext(anyList(), anyString());
    }

    // ========== Test 2: PRICE_INQUIRY uses threshold 0.5 and topK 3 ==========

    @Test
    @DisplayName("PRICE_INQUIRY should use threshold 0.5 and topK 3 for RAG search")
    void testPriceInquiry_UsesHighThreshold() {
        // Given
        String message = "故宫门票多少钱";
        when(intentService.recognizeIntent(message)).thenReturn(IntentType.PRICE_INQUIRY);
        when(dynamicThresholdService.needsRAG(IntentType.PRICE_INQUIRY)).thenReturn(true);
        when(dynamicThresholdService.getThreshold(IntentType.PRICE_INQUIRY)).thenReturn(0.5);
        when(dynamicThresholdService.getTopK(IntentType.PRICE_INQUIRY)).thenReturn(3);

        RAGSearchResponse searchResponse = RAGSearchResponse.builder()
                .query(message)
                .chunks(Collections.emptyList())
                .totalChunks(0)
                .build();
        when(knowledgeBaseService.search(any(RAGSearchRequest.class))).thenReturn(searchResponse);
        when(enhancedContextService.buildPromptWithContext(anyList(), eq(message)))
                .thenReturn("增强Prompt");
        when(aiService.chatStream("增强Prompt")).thenReturn("故宫旺季门票60元");

        // When
        ChatResponse response = ragChatService.chatWithRAG(message, SESSION_ID);

        // Then
        assertNotNull(response);
        assertEquals("故宫旺季门票60元", response.getResponse());

        // Verify search was called with correct threshold and topK
        ArgumentCaptor<RAGSearchRequest> captor = ArgumentCaptor.forClass(RAGSearchRequest.class);
        verify(knowledgeBaseService).search(captor.capture());
        RAGSearchRequest capturedReq = captor.getValue();
        assertEquals(0.5, capturedReq.getThreshold(), 0.001, "Threshold should be 0.5");
        assertEquals(3, capturedReq.getTopK(), "TopK should be 3");
        assertEquals(message, capturedReq.getQuery(), "Query should be the user message");
    }

    // ========== Test 3: PRODUCT_RECOMMENDATION uses threshold 0.3 and topK 5 ==========

    @Test
    @DisplayName("PRODUCT_RECOMMENDATION should use threshold 0.3 and topK 5 for broader recall")
    void testRecommendation_UsesLowThreshold() {
        // Given
        String message = "推荐景点";
        when(intentService.recognizeIntent(message)).thenReturn(IntentType.PRODUCT_RECOMMENDATION);
        when(dynamicThresholdService.needsRAG(IntentType.PRODUCT_RECOMMENDATION)).thenReturn(true);
        when(dynamicThresholdService.getThreshold(IntentType.PRODUCT_RECOMMENDATION)).thenReturn(0.3);
        when(dynamicThresholdService.getTopK(IntentType.PRODUCT_RECOMMENDATION)).thenReturn(5);

        RAGSearchResponse searchResponse = RAGSearchResponse.builder()
                .query(message)
                .chunks(Collections.emptyList())
                .totalChunks(0)
                .build();
        when(knowledgeBaseService.search(any(RAGSearchRequest.class))).thenReturn(searchResponse);
        when(enhancedContextService.buildPromptWithContext(anyList(), eq(message)))
                .thenReturn("推荐Prompt");
        when(aiService.chatStream("推荐Prompt")).thenReturn("为您推荐故宫和长城");

        // When
        ChatResponse response = ragChatService.chatWithRAG(message, SESSION_ID);

        // Then
        assertNotNull(response);

        // Verify search parameters
        ArgumentCaptor<RAGSearchRequest> captor = ArgumentCaptor.forClass(RAGSearchRequest.class);
        verify(knowledgeBaseService).search(captor.capture());
        RAGSearchRequest capturedReq = captor.getValue();
        assertEquals(0.3, capturedReq.getThreshold(), 0.001, "Threshold should be 0.3");
        assertEquals(5, capturedReq.getTopK(), "TopK should be 5");
    }

    // ========== Test 4: RAG results produce sources in ChatResponse ==========

    @Test
    @DisplayName("RAG search results should produce SourceInfo list in ChatResponse")
    void testRAGResponse_ContainsSources() {
        // Given
        String message = "故宫怎么玩";
        when(intentService.recognizeIntent(message)).thenReturn(IntentType.ATTRACTION_QUERY);
        when(dynamicThresholdService.needsRAG(IntentType.ATTRACTION_QUERY)).thenReturn(true);
        when(dynamicThresholdService.getThreshold(IntentType.ATTRACTION_QUERY)).thenReturn(0.5);
        when(dynamicThresholdService.getTopK(IntentType.ATTRACTION_QUERY)).thenReturn(3);

        DocumentChunk chunk1 = DocumentChunk.builder()
                .chunkId("c1").docId("d1").docTitle("故宫攻略")
                .content("建议路线：午门→太和殿→乾清宫").score(0.9).category("景点").build();
        DocumentChunk chunk2 = DocumentChunk.builder()
                .chunkId("c2").docId("d2").docTitle("故宫门票")
                .content("门票60元").score(0.7).category("景点").build();

        List<DocumentChunk> chunks = Arrays.asList(chunk1, chunk2);
        RAGSearchResponse searchResponse = RAGSearchResponse.builder()
                .query(message).chunks(chunks).totalChunks(2).build();

        when(knowledgeBaseService.search(any(RAGSearchRequest.class))).thenReturn(searchResponse);
        when(enhancedContextService.buildPromptWithContext(eq(chunks), eq(message)))
                .thenReturn("增强Prompt含来源");
        when(aiService.chatStream("增强Prompt含来源")).thenReturn("故宫推荐路线：午门→太和殿");

        List<SourceInfo> expectedSources = Arrays.asList(
                SourceInfo.builder().docTitle("故宫攻略").score(0.9).category("景点").relevance("高度相关").build(),
                SourceInfo.builder().docTitle("故宫门票").score(0.7).category("景点").relevance("高度相关").build()
        );
        when(enhancedContextService.buildSourceInfoList(chunks)).thenReturn(expectedSources);

        // When
        ChatResponse response = ragChatService.chatWithRAG(message, SESSION_ID);

        // Then
        assertNotNull(response);
        assertNotNull(response.getSources(), "Sources list should not be null");
        assertEquals(2, response.getSources().size(), "Should have 2 source items");
        assertEquals("故宫攻略", response.getSources().get(0).getDocTitle());
        assertEquals("故宫门票", response.getSources().get(1).getDocTitle());
    }

    // ========== Test 5: Empty RAG results still produce response ==========

    @Test
    @DisplayName("Empty RAG results should still produce a response via LLM")
    void testEmptyRAGResults_StillResponds() {
        // Given
        String message = "什么好玩";
        when(intentService.recognizeIntent(message)).thenReturn(IntentType.GENERAL);
        when(dynamicThresholdService.needsRAG(IntentType.GENERAL)).thenReturn(true);
        when(dynamicThresholdService.getThreshold(IntentType.GENERAL)).thenReturn(0.2);
        when(dynamicThresholdService.getTopK(IntentType.GENERAL)).thenReturn(5);

        RAGSearchResponse emptyResponse = RAGSearchResponse.builder()
                .query(message).chunks(Collections.emptyList()).totalChunks(0).build();
        when(knowledgeBaseService.search(any(RAGSearchRequest.class))).thenReturn(emptyResponse);
        when(enhancedContextService.buildPromptWithContext(eq(Collections.emptyList()), eq(message)))
                .thenReturn("空结果Prompt");
        when(enhancedContextService.buildSourceInfoList(Collections.emptyList()))
                .thenReturn(Collections.emptyList());
        when(aiService.chatStream("空结果Prompt")).thenReturn("推荐您去故宫、长城");

        // When
        ChatResponse response = ragChatService.chatWithRAG(message, SESSION_ID);

        // Then
        assertNotNull(response);
        assertNotNull(response.getResponse(), "Response should not be null even with empty RAG results");
        assertFalse(response.getResponse().isEmpty(), "Response should not be empty");
        verify(enhancedContextService).buildPromptWithContext(eq(Collections.emptyList()), eq(message));
        verify(aiService).chatStream("空结果Prompt");
    }

    // ========== Test 6: RAG search failure falls back to direct chat ==========

    @Test
    @DisplayName("RAG search failure should fall back to direct chat without sources")
    void testRAGFailure_FallbackToDirectChat() {
        // Given
        String message = "故宫门票多少钱";
        when(intentService.recognizeIntent(message)).thenReturn(IntentType.PRICE_INQUIRY);
        when(dynamicThresholdService.needsRAG(IntentType.PRICE_INQUIRY)).thenReturn(true);
        when(dynamicThresholdService.getThreshold(IntentType.PRICE_INQUIRY)).thenReturn(0.5);
        when(dynamicThresholdService.getTopK(IntentType.PRICE_INQUIRY)).thenReturn(3);

        // Simulate RAG search failure
        when(knowledgeBaseService.search(any(RAGSearchRequest.class)))
                .thenThrow(new RuntimeException("Milvus connection failed"));

        // Fallback: direct chat with original message
        when(aiService.chatStream(message)).thenReturn("故宫门票旺季60元");

        // When
        ChatResponse response = ragChatService.chatWithRAG(message, SESSION_ID);

        // Then
        assertNotNull(response);
        assertEquals("故宫门票旺季60元", response.getResponse(),
                "Should fall back to direct AI response");
        assertTrue(response.getSources() == null || response.getSources().isEmpty(),
                "Sources should be null or empty on RAG failure fallback");
        assertEquals(SESSION_ID, response.getSessionId(), "Session ID should be preserved");
        assertTrue(response.getFinished(), "Response should be marked as finished");
    }

    // ========== Test 7: GENERAL intent uses RAG with low threshold ==========

    @Test
    @DisplayName("GENERAL intent should use RAG with low threshold 0.2 for broad recall")
    void testGeneralIntent_UsesRAG() {
        // Given
        String message = "介绍一下北京";
        when(intentService.recognizeIntent(message)).thenReturn(IntentType.GENERAL);
        when(dynamicThresholdService.needsRAG(IntentType.GENERAL)).thenReturn(true);
        when(dynamicThresholdService.getThreshold(IntentType.GENERAL)).thenReturn(0.2);
        when(dynamicThresholdService.getTopK(IntentType.GENERAL)).thenReturn(5);

        RAGSearchResponse searchResponse = RAGSearchResponse.builder()
                .query(message).chunks(Collections.emptyList()).totalChunks(0).build();
        when(knowledgeBaseService.search(any(RAGSearchRequest.class))).thenReturn(searchResponse);
        when(enhancedContextService.buildPromptWithContext(anyList(), eq(message)))
                .thenReturn("通用Prompt");
        when(aiService.chatStream("通用Prompt")).thenReturn("北京是中国的首都");

        // When
        ChatResponse response = ragChatService.chatWithRAG(message, SESSION_ID);

        // Then
        assertNotNull(response);
        assertEquals("general", response.getIntentType());

        ArgumentCaptor<RAGSearchRequest> captor = ArgumentCaptor.forClass(RAGSearchRequest.class);
        verify(knowledgeBaseService).search(captor.capture());
        assertEquals(0.2, captor.getValue().getThreshold(), 0.001,
                "GENERAL intent should use low threshold 0.2");
    }
}
