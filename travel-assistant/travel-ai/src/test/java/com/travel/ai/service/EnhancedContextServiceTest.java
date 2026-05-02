package com.travel.ai.service;

import com.travel.ai.config.BaseServiceTest;
import com.travel.ai.dto.RAGSearchResponse.DocumentChunk;
import com.travel.ai.dto.SourceInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EnhancedContextService 单元测试
 *
 * 严格 TDD: RED 阶段 - 先写测试，验证失败后再实现
 */
class EnhancedContextServiceTest extends BaseServiceTest {

    // 直接实例化，不依赖 Spring 容器
    private final EnhancedContextService enhancedContextService = new EnhancedContextService();

    // ========== buildPromptWithContext 测试 ==========

    @Test
    @DisplayName("单个来源：构建上下文包含来源编号、文档标题和相似度")
    void testBuildContext_SingleSource() {
        // Given
        DocumentChunk chunk = DocumentChunk.builder()
                .chunkId("chunk-1")
                .docId("doc-1")
                .docTitle("故宫旅游攻略")
                .content("故宫门票价格为60元")
                .score(0.85)
                .category("景点")
                .chunkIndex(0)
                .build();
        List<DocumentChunk> chunks = Collections.singletonList(chunk);

        // When
        String result = enhancedContextService.buildPromptWithContext(chunks, "故宫门票多少钱");

        // Then
        assertTrue(result.contains("[来源1]"), "应包含来源编号 [来源1]");
        assertTrue(result.contains("故宫旅游攻略"), "应包含文档标题");
        assertTrue(result.contains("相似度"), "应包含相似度信息");
        assertTrue(result.contains("85.0%"), "应显示正确的相似度百分比");
        assertTrue(result.contains("故宫门票价格为60元"), "应包含分块内容");
    }

    @Test
    @DisplayName("多个来源：按相似度降序排列")
    void testBuildContext_MultipleSources() {
        // Given - 故意不按分数顺序传入
        DocumentChunk chunk1 = DocumentChunk.builder()
                .docTitle("长城门票信息")
                .content("长城门票40元")
                .score(0.5)
                .build();
        DocumentChunk chunk2 = DocumentChunk.builder()
                .docTitle("故宫门票信息")
                .content("故宫门票60元")
                .score(0.9)
                .build();
        DocumentChunk chunk3 = DocumentChunk.builder()
                .docTitle("天坛门票信息")
                .content("天坛门票15元")
                .score(0.7)
                .build();
        List<DocumentChunk> chunks = Arrays.asList(chunk1, chunk2, chunk3);

        // When
        String result = enhancedContextService.buildPromptWithContext(chunks, "北京门票价格");

        // Then - 验证排序：[来源1] 应该是最高分 0.9
        assertTrue(result.contains("[来源1]"), "应包含 [来源1]");
        assertTrue(result.contains("[来源2]"), "应包含 [来源2]");
        assertTrue(result.contains("[来源3]"), "应包含 [来源3]");

        // 验证排序：来源1 应该是 0.9 分的故宫
        int idxGugong = result.indexOf("故宫门票信息");
        int idxTiantan = result.indexOf("天坛门票信息");
        int idxChangcheng = result.indexOf("长城门票信息");
        assertTrue(idxGugong < idxTiantan, "故宫(0.9)应排在天坛(0.7)之前");
        assertTrue(idxTiantan < idxChangcheng, "天坛(0.7)应排在长城(0.5)之前");
    }

    @Test
    @DisplayName("空结果列表：返回友好提示信息")
    void testBuildContext_EmptyResults() {
        // Given
        List<DocumentChunk> chunks = Collections.emptyList();

        // When
        String result = enhancedContextService.buildPromptWithContext(chunks, "什么好玩");

        // Then
        assertTrue(result.contains("未找到相关") || result.contains("未找到"),
                "空结果时应返回友好提示，告知未找到相关内容");
    }

    @Test
    @DisplayName("空结果null列表：返回友好提示信息")
    void testBuildContext_NullResults() {
        // Given
        List<DocumentChunk> chunks = null;

        // When
        String result = enhancedContextService.buildPromptWithContext(chunks, "什么好玩");

        // Then
        assertTrue(result.contains("未找到相关") || result.contains("未找到"),
                "null结果时应返回友好提示");
    }

    // ========== buildSourceInfoList 测试 ==========

    @Test
    @DisplayName("转换分块为SourceInfo列表，包含正确的相关度标签")
    void testBuildSourceInfoList() {
        // Given
        DocumentChunk chunkHigh = DocumentChunk.builder()
                .docTitle("高度相关文档")
                .score(0.85)
                .category("景点")
                .build();
        DocumentChunk chunkMedium = DocumentChunk.builder()
                .docTitle("一般相关文档")
                .score(0.55)
                .category("美食")
                .build();
        DocumentChunk chunkLow = DocumentChunk.builder()
                .docTitle("低相关文档")
                .score(0.2)
                .category("住宿")
                .build();
        List<DocumentChunk> chunks = Arrays.asList(chunkHigh, chunkMedium, chunkLow);

        // When
        List<SourceInfo> sources = enhancedContextService.buildSourceInfoList(chunks);

        // Then
        assertEquals(3, sources.size(), "应返回3个SourceInfo");

        // 验证按分数降序
        assertEquals("高度相关", sources.get(0).getRelevance(), "score >= 0.7 应为 '高度相关'");
        assertEquals(0.85, sources.get(0).getScore(), 0.001, "分数应正确传递");
        assertEquals("景点", sources.get(0).getCategory(), "分类应正确传递");

        assertEquals("一般相关", sources.get(1).getRelevance(), "0.4 <= score < 0.7 应为 '一般相关'");

        assertEquals("低相关", sources.get(2).getRelevance(), "score < 0.4 应为 '低相关'");
    }

    @Test
    @DisplayName("相关度边界值：0.7为高度相关，0.4为一般相关")
    void testBuildSourceInfoList_RelevanceBoundary() {
        // Given - 精确边界值
        DocumentChunk chunk07 = DocumentChunk.builder()
                .docTitle("边界值0.7")
                .score(0.7)
                .category("测试")
                .build();
        DocumentChunk chunk04 = DocumentChunk.builder()
                .docTitle("边界值0.4")
                .score(0.4)
                .category("测试")
                .build();
        DocumentChunk chunk039 = DocumentChunk.builder()
                .docTitle("边界值0.39")
                .score(0.39)
                .category("测试")
                .build();
        List<DocumentChunk> chunks = Arrays.asList(chunk07, chunk04, chunk039);

        // When
        List<SourceInfo> sources = enhancedContextService.buildSourceInfoList(chunks);

        // Then
        assertEquals("高度相关", sources.get(0).getRelevance(), "score=0.7 应为 '高度相关'");
        assertEquals("一般相关", sources.get(1).getRelevance(), "score=0.4 应为 '一般相关'");
        assertEquals("低相关", sources.get(2).getRelevance(), "score=0.39 应为 '低相关'");
    }

    @Test
    @DisplayName("空分块列表：返回空SourceInfo列表")
    void testBuildSourceInfoList_EmptyInput() {
        // Given
        List<DocumentChunk> chunks = Collections.emptyList();

        // When
        List<SourceInfo> sources = enhancedContextService.buildSourceInfoList(chunks);

        // Then
        assertNotNull(sources, "不应返回null");
        assertTrue(sources.isEmpty(), "空输入应返回空列表");
    }

    @Test
    @DisplayName("null分块列表：返回空SourceInfo列表")
    void testBuildSourceInfoList_NullInput() {
        // When
        List<SourceInfo> sources = enhancedContextService.buildSourceInfoList(null);

        // Then
        assertNotNull(sources, "不应返回null");
        assertTrue(sources.isEmpty(), "null输入应返回空列表");
    }

    // ========== Prompt 模板测试 ==========

    @Test
    @DisplayName("Prompt模板包含系统指令：要求基于检索结果回答并标注来源")
    void testPromptTemplate_ContainsInstructions() {
        // Given
        DocumentChunk chunk = DocumentChunk.builder()
                .docTitle("测试文档")
                .content("测试内容")
                .score(0.8)
                .build();

        // When
        String result = enhancedContextService.buildPromptWithContext(
                Collections.singletonList(chunk), "测试问题");

        // Then
        assertTrue(result.contains("请根据以下检索结果回答") || result.contains("检索结果"),
                "应包含基于检索结果回答的指令");
        assertTrue(result.contains("标注信息来源") || result.contains("来源"),
                "应包含标注来源的要求");
    }

    @Test
    @DisplayName("Prompt模板包含用户问题")
    void testPromptTemplate_ContainsUserQuestion() {
        // Given
        String userQuery = "故宫门票多少钱";

        // When
        String result = enhancedContextService.buildPromptWithContext(
                Collections.emptyList(), userQuery);

        // Then
        assertTrue(result.contains(userQuery),
                "Prompt应包含用户原始问题: " + userQuery);
    }
}
