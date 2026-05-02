package com.travel.ai.service;

import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.service.ConversationCompressor.DistillationResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * ConversationCompressor.distill() structured distillation tests (TDD).
 *
 * Tests the new distill() method that returns both a summary
 * and structured JSON preferences, plus the DistillationResult container.
 */
class ConversationCompressorDistillationTest extends BaseServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    // ====================================================================
    // DistillationResult builder / data holder tests
    // ====================================================================

    @Nested
    @DisplayName("DistillationResult data class")
    class DistillationResultTests {

        @Test
        @DisplayName("builder sets all fields correctly")
        void builderSetsAllFields() {
            DistillationResult result = DistillationResult.builder()
                    .summary("summary text")
                    .extractedJson("{\"preferences\":[]}")
                    .usedLlm(true)
                    .build();

            assertEquals("summary text", result.getSummary());
            assertEquals("{\"preferences\":[]}", result.getExtractedJson());
            assertTrue(result.isUsedLlm());
        }

        @Test
        @DisplayName("builder defaults usedLlm to false")
        void builderDefaultsUsedLlm() {
            DistillationResult result = DistillationResult.builder()
                    .summary("s")
                    .extractedJson(null)
                    .build();

            assertFalse(result.isUsedLlm());
        }

        @Test
        @DisplayName("allows null extractedJson")
        void allowsNullExtractedJson() {
            DistillationResult result = DistillationResult.builder()
                    .summary("summary only")
                    .extractedJson(null)
                    .usedLlm(false)
                    .build();

            assertEquals("summary only", result.getSummary());
            assertNull(result.getExtractedJson());
        }
    }

    // ====================================================================
    // distill() - LLM returns valid JSON with both fields
    // ====================================================================

    @Nested
    @DisplayName("distill() with valid LLM JSON response")
    class DistillValidLlmResponse {

        @Test
        @DisplayName("returns both summary and preferences when LLM returns complete JSON")
        void returnsBothSummaryAndPreferences() {
            String llmResponse = "{\"summary\":\"用户询问了故宫门票价格\",\"preferences\":[{\"key\":\"budget_range\",\"value\":\"60-80\",\"confidence\":0.8}]}";
            when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = IntStream.range(0, 12)
                    .mapToObj(i -> "Message " + i)
                    .collect(Collectors.toList());

            DistillationResult result = compressor.distill(messages);

            assertNotNull(result);
            assertEquals("用户询问了故宫门票价格", result.getSummary());
            assertNotNull(result.getExtractedJson());
            assertTrue(result.getExtractedJson().contains("budget_range"));
            assertTrue(result.isUsedLlm());
        }

        @Test
        @DisplayName("extractedJson preserves the preferences array from LLM")
        void extractedJsonPreservesPreferences() {
            String llmResponse = "{\"summary\":\"用户想去三亚度假\",\"preferences\":[{\"key\":\"destination\",\"value\":\"三亚\",\"confidence\":0.9},{\"key\":\"travel_style\",\"value\":\"休闲\",\"confidence\":0.7}]}";
            when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = IntStream.range(0, 15)
                    .mapToObj(i -> "Msg " + i)
                    .collect(Collectors.toList());

            DistillationResult result = compressor.distill(messages);

            assertNotNull(result);
            assertTrue(result.getExtractedJson().contains("destination"));
            assertTrue(result.getExtractedJson().contains("travel_style"));
            assertTrue(result.getExtractedJson().contains("三亚"));
        }
    }

    // ====================================================================
    // distill() - LLM returns only summary (no preferences key)
    // ====================================================================

    @Nested
    @DisplayName("distill() with LLM response missing preferences")
    class DistillMissingPreferences {

        @Test
        @DisplayName("returns summary with null extractedJson when LLM returns only summary")
        void returnsSummaryWithNullExtractedJson() {
            String llmResponse = "{\"summary\":\"用户讨论了旅游计划\"}";
            when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = IntStream.range(0, 12)
                    .mapToObj(i -> "Msg " + i)
                    .collect(Collectors.toList());

            DistillationResult result = compressor.distill(messages);

            assertNotNull(result);
            assertEquals("用户讨论了旅游计划", result.getSummary());
            assertNull(result.getExtractedJson());
            assertTrue(result.isUsedLlm());
        }

        @Test
        @DisplayName("returns summary with null extractedJson when preferences array is empty")
        void returnsSummaryWithNullExtractedJsonWhenEmptyPreferences() {
            String llmResponse = "{\"summary\":\"用户询问天气情况\",\"preferences\":[]}";
            when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = IntStream.range(0, 11)
                    .mapToObj(i -> "Msg " + i)
                    .collect(Collectors.toList());

            DistillationResult result = compressor.distill(messages);

            assertNotNull(result);
            assertEquals("用户询问天气情况", result.getSummary());
            assertNull(result.getExtractedJson(), "Empty preferences array should yield null extractedJson");
        }
    }

    // ====================================================================
    // distill() - LLM returns non-JSON (fallback)
    // ====================================================================

    @Nested
    @DisplayName("distill() fallback when LLM returns unparseable response")
    class DistillFallbackNonJson {

        @Test
        @DisplayName("falls back to compressWithFallback summary when LLM returns plain text")
        void fallsBackToCompressWhenLlmReturnsPlainText() {
            // First call: distill prompt returns plain text (unparseable)
            when(chatLanguageModel.generate(anyString()))
                    .thenReturn("This is just a plain text summary, not JSON at all.");

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = IntStream.range(0, 12)
                    .mapToObj(i -> "Message " + i)
                    .collect(Collectors.toList());

            DistillationResult result = compressor.distill(messages);

            assertNotNull(result);
            assertNotNull(result.getSummary(), "Fallback should still produce a summary");
            assertNull(result.getExtractedJson());
            assertFalse(result.isUsedLlm(), "Fallback result should report usedLlm = false");
        }
    }

    // ====================================================================
    // distill() - LLM throws exception
    // ====================================================================

    @Nested
    @DisplayName("distill() fallback when LLM fails")
    class DistillFallbackLlmFailure {

        @Test
        @DisplayName("falls back gracefully when LLM throws exception")
        void fallsBackWhenLlmThrows() {
            when(chatLanguageModel.generate(anyString()))
                    .thenThrow(new RuntimeException("API connection failed"));

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = IntStream.range(0, 12)
                    .mapToObj(i -> "Message " + i)
                    .collect(Collectors.toList());

            DistillationResult result = compressor.distill(messages);

            assertNotNull(result, "Should return non-null result even on LLM failure");
            assertNotNull(result.getSummary(), "Fallback summary should exist");
            assertNull(result.getExtractedJson());
            assertFalse(result.isUsedLlm());
        }
    }

    // ====================================================================
    // distill() - edge cases: null, empty, under-threshold
    // ====================================================================

    @Nested
    @DisplayName("distill() edge cases")
    class DistillEdgeCases {

        @Test
        @DisplayName("returns null for null messages")
        void returnsNullForNullMessages() {
            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);

            DistillationResult result = compressor.distill(null);

            assertNull(result);
        }

        @Test
        @DisplayName("returns null for empty messages list")
        void returnsNullForEmptyMessages() {
            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);

            DistillationResult result = compressor.distill(List.of());

            assertNull(result);
        }

        @Test
        @DisplayName("returns null when message count is at threshold (10)")
        void returnsNullAtThreshold() {
            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = IntStream.range(0, 10)
                    .mapToObj(i -> "Msg " + i)
                    .collect(Collectors.toList());

            DistillationResult result = compressor.distill(messages);

            assertNull(result, "At threshold (10 messages) distill should return null");
        }

        @Test
        @DisplayName("returns null when message count is below threshold")
        void returnsNullBelowThreshold() {
            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = IntStream.range(0, 5)
                    .mapToObj(i -> "Msg " + i)
                    .collect(Collectors.toList());

            DistillationResult result = compressor.distill(messages);

            assertNull(result, "Below threshold distill should return null");
        }

        @Test
        @DisplayName("returns result when message count exceeds threshold by 1")
        void returnsResultJustAboveThreshold() {
            String llmResponse = "{\"summary\":\"摘要\",\"preferences\":[{\"key\":\"k\",\"value\":\"v\",\"confidence\":0.5}]}";
            when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = IntStream.range(0, 11)
                    .mapToObj(i -> "Msg " + i)
                    .collect(Collectors.toList());

            DistillationResult result = compressor.distill(messages);

            assertNotNull(result, "11 messages should trigger distillation");
        }
    }

    // ====================================================================
    // distill() - prompt content verification
    // ====================================================================

    @Nested
    @DisplayName("distill() prompt verification")
    class DistillPromptVerification {

        @Test
        @DisplayName("prompt contains both summary and extraction instructions")
        void promptContainsBothSummaryAndExtractionInstructions() {
            when(chatLanguageModel.generate(anyString())).thenReturn(
                    "{\"summary\":\"s\",\"preferences\":[]}");

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = IntStream.range(0, 12)
                    .mapToObj(i -> "Msg " + i)
                    .collect(Collectors.toList());

            compressor.distill(messages);

            verify(chatLanguageModel).generate(argThat((String prompt) ->
                    prompt != null
                    && prompt.contains("summary")
                    && prompt.contains("preferences")
            ));
        }

        @Test
        @DisplayName("prompt includes the original conversation content")
        void promptIncludesConversationContent() {
            when(chatLanguageModel.generate(anyString())).thenReturn(
                    "{\"summary\":\"s\",\"preferences\":[]}");

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = List.of(
                    "故宫门票多少钱？",
                    "旺季60元，淡季40元。",
                    "学生有优惠吗？"
            );
            // Pad to exceed threshold
            for (int i = 0; i < 10; i++) {
                messages = new java.util.ArrayList<>(messages);
                messages.add("Extra message " + i);
            }

            compressor.distill(messages);

            verify(chatLanguageModel).generate(argThat((String prompt) ->
                    prompt.contains("故宫门票多少钱")
                            && prompt.contains("旺季60元")
            ));
        }
    }

    // ====================================================================
    // distill() - LLM returns JSON wrapped in markdown code blocks
    // ====================================================================

    @Nested
    @DisplayName("distill() handles markdown-wrapped JSON")
    class DistillMarkdownWrappedJson {

        @Test
        @DisplayName("parses JSON from markdown code block")
        void parsesJsonFromMarkdownCodeBlock() {
            String llmResponse = "```json\n{\"summary\":\"用户计划去北京\",\"preferences\":[{\"key\":\"destination\",\"value\":\"北京\",\"confidence\":0.9}]}\n```";
            when(chatLanguageModel.generate(anyString())).thenReturn(llmResponse);

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = IntStream.range(0, 12)
                    .mapToObj(i -> "Msg " + i)
                    .collect(Collectors.toList());

            DistillationResult result = compressor.distill(messages);

            assertNotNull(result);
            assertEquals("用户计划去北京", result.getSummary());
            assertNotNull(result.getExtractedJson());
            assertTrue(result.getExtractedJson().contains("北京"));
            assertTrue(result.isUsedLlm());
        }
    }

    // ====================================================================
    // Existing functionality preservation
    // ====================================================================

    @Nested
    @DisplayName("Existing compressWithFallback still works")
    class ExistingFunctionalityPreserved {

        @Test
        @DisplayName("compressWithFallback still returns String summary")
        void compressWithFallbackStillReturnsString() {
            when(chatLanguageModel.generate(anyString())).thenReturn("LLM summary");

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = List.of("msg1", "msg2");

            String summary = compressor.compressWithFallback(messages);

            assertEquals("LLM summary", summary);
        }

        @Test
        @DisplayName("compressWithFallback still falls back on LLM failure")
        void compressWithFallbackStillFallsBack() {
            when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("fail"));

            ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
            List<String> messages = List.of("故宫门票多少钱？价格如何。");

            String summary = compressor.compressWithFallback(messages);

            assertNotNull(summary);
            assertTrue(summary.contains("对话摘要"));
        }
    }
}
