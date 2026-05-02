package com.travel.ai.service;

import com.travel.ai.config.BaseServiceTest;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ConversationCompressor 单元测试 (TDD RED阶段)
 *
 * 测试对话摘要压缩功能：
 * - 当对话超过10轮（20条消息）时，将较早的对话压缩为摘要
 * - LLM不可用时，降级到首句提取
 * - 摘要应保留关键实体信息
 *
 * 论文 5.3.3: 对话摘要压缩机制
 */
class ConversationCompressionTest extends BaseServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    // ========== compressIfNeeded 测试 ==========

    @Test
    @DisplayName("少于10条消息时不应触发压缩")
    void testCompressIfNeeded_UnderThreshold_NoCompression() {
        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
        List<String> messages = IntStream.range(0, 8)
                .mapToObj(i -> "msg" + i)
                .collect(Collectors.toList());

        String result = compressor.compressIfNeeded(messages);

        assertNull(result, "少于10条消息时不应触发压缩，应返回null");
    }

    @Test
    @DisplayName("恰好10条消息时不应触发压缩（边界值）")
    void testCompressIfNeeded_ExactlyThreshold_NoCompression() {
        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
        List<String> messages = IntStream.range(0, 10)
                .mapToObj(i -> "msg" + i)
                .collect(Collectors.toList());

        String result = compressor.compressIfNeeded(messages);

        assertNull(result, "恰好10条消息时不应触发压缩，应返回null");
    }

    @Test
    @DisplayName("超过10条消息时应触发压缩")
    void testCompressIfNeeded_OverThreshold_TriggersCompression() {
        when(chatLanguageModel.generate(anyString())).thenReturn("用户询问了故宫门票价格和优惠政策");

        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
        List<String> messages = IntStream.range(0, 16)
                .mapToObj(i -> "Message " + i)
                .collect(Collectors.toList());

        String summary = compressor.compressIfNeeded(messages);

        assertNotNull(summary, "超过10条消息时应返回压缩摘要");
        assertEquals("用户询问了故宫门票价格和优惠政策", summary);
    }

    @Test
    @DisplayName("空消息列表返回null")
    void testCompressIfNeeded_EmptyMessages() {
        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);

        assertNull(compressor.compressIfNeeded(List.of()),
                "空消息列表应返回null");
    }

    @Test
    @DisplayName("null消息列表返回null")
    void testCompressIfNeeded_NullMessages() {
        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);

        assertNull(compressor.compressIfNeeded(null),
                "null消息列表应返回null");
    }

    // ========== compressWithFallback 测试 ==========

    @Test
    @DisplayName("LLM调用失败时降级到首句提取")
    void testCompressWithFallback_LLMFailure_FallbackToFirstSentence() {
        when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("API error"));

        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
        List<String> messages = List.of(
                "故宫门票多少钱？我想了解一下。",
                "故宫的门票是旺季60元，淡季40元。",
                "还有其他优惠吗？",
                "学生可以享受半价优惠。"
        );

        String summary = compressor.compressWithFallback(messages);

        assertNotNull(summary, "LLM失败时应有降级摘要");
        assertTrue(summary.length() > 0, "降级摘要不应为空");
        assertTrue(summary.contains("对话摘要"), "降级摘要应包含'对话摘要'前缀");
    }

    @Test
    @DisplayName("LLM返回空字符串时降级到首句提取")
    void testCompressWithFallback_LLMReturnsEmpty_Fallback() {
        when(chatLanguageModel.generate(anyString())).thenReturn("");

        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
        List<String> messages = List.of(
                "故宫门票多少钱？",
                "旺季60元，淡季40元。"
        );

        String summary = compressor.compressWithFallback(messages);

        assertNotNull(summary, "LLM返回空时应有降级摘要");
        assertTrue(summary.length() > 0, "降级摘要不应为空");
    }

    @Test
    @DisplayName("LLM返回空白字符串时降级到首句提取")
    void testCompressWithFallback_LLMReturnsBlank_Fallback() {
        when(chatLanguageModel.generate(anyString())).thenReturn("   ");

        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
        List<String> messages = List.of(
                "故宫门票多少钱？",
                "旺季60元，淡季40元。"
        );

        String summary = compressor.compressWithFallback(messages);

        assertNotNull(summary, "LLM返回空白时应有降级摘要");
        assertTrue(summary.length() > 0, "降级摘要不应为空");
    }

    @Test
    @DisplayName("compressWithFallback的null输入返回null")
    void testCompressWithFallback_NullInput() {
        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);

        assertNull(compressor.compressWithFallback(null),
                "null输入应返回null");
    }

    @Test
    @DisplayName("compressWithFallback的空列表输入返回null")
    void testCompressWithFallback_EmptyInput() {
        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);

        assertNull(compressor.compressWithFallback(List.of()),
                "空列表应返回null");
    }

    // ========== buildSummaryPrompt 测试 ==========

    @Test
    @DisplayName("摘要提示词包含压缩指令和关键信息要求")
    void testBuildSummaryPrompt_ContainsInstructions() {
        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);

        String prompt = compressor.buildSummaryPrompt(List.of(
                "用户：故宫门票多少钱？",
                "助手：旺季60元，淡季40元。",
                "用户：学生有优惠吗？",
                "助手：学生半价优惠。"
        ));

        assertTrue(prompt.contains("压缩"), "提示词应包含'压缩'关键词");
        assertTrue(prompt.contains("关键信息"), "提示词应包含'关键信息'关键词");
    }

    @Test
    @DisplayName("摘要提示词包含原始对话内容")
    void testBuildSummaryPrompt_ContainsOriginalMessages() {
        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);

        String prompt = compressor.buildSummaryPrompt(List.of(
                "用户：故宫门票多少钱？",
                "助手：旺季60元，淡季40元。"
        ));

        assertTrue(prompt.contains("故宫门票多少钱"),
                "提示词应包含用户原始消息");
        assertTrue(prompt.contains("旺季60元"),
                "提示词应包含助手原始回复");
    }

    // ========== 首句提取降级逻辑测试 ==========

    @Test
    @DisplayName("首句提取：按标点符号截取每条消息的首句")
    void testFirstSentenceExtraction_ByPunctuation() {
        when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("fail"));

        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
        List<String> messages = List.of(
                "故宫门票多少钱？我想了解一下。",
                "旺季60元，淡季40元。价格合理。"
        );

        String summary = compressor.compressWithFallback(messages);

        assertNotNull(summary);
        // The first message should be extracted up to "？"
        assertTrue(summary.contains("故宫门票多少钱？"),
                "应提取到第一个问号为止的首句");
    }

    @Test
    @DisplayName("首句提取：超长句子截断到20字符")
    void testFirstSentenceExtraction_LongSentenceTruncation() {
        when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("fail"));

        ConversationCompressor compressor = new ConversationCompressor(chatLanguageModel);
        List<String> messages = List.of(
                "这是一条非常非常长的消息没有任何标点符号来分割它所以应该被截断处理"
        );

        String summary = compressor.compressWithFallback(messages);

        assertNotNull(summary);
        // Should be truncated to 20 chars + "..."
        assertTrue(summary.contains("..."),
                "超长无标点的消息应被截断并添加省略号");
    }
}
