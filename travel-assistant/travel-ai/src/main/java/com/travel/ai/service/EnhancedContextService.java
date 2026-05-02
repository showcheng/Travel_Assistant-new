package com.travel.ai.service;

import com.travel.ai.dto.RAGSearchResponse.DocumentChunk;
import com.travel.ai.dto.SourceInfo;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 增强上下文服务
 *
 * 负责将 RAG 检索到的文档分块转换为增强 Prompt 和前端可用的来源信息。
 * 不修改现有的 KnowledgeBaseServiceSimpleImpl，后续由 AIServiceImpl 调用。
 */
@Service
public class EnhancedContextService {

    private static final String SYSTEM_PROMPT =
            "你是专业的旅游顾问。请根据以下检索结果回答用户问题。\n" +
            "要求：\n" +
            "1. 基于检索结果回答，标注信息来源\n" +
            "2. 如果检索结果中没有相关信息，请明确告知\n" +
            "3. 回答要简洁准确\n\n";

    /**
     * 构建带有检索结果的增强 Prompt
     *
     * @param chunks   检索到的文档分块列表（可为 null 或空）
     * @param userQuery 用户原始问题
     * @return 拼接好的完整 Prompt
     */
    public String buildPromptWithContext(List<DocumentChunk> chunks, String userQuery) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);

        if (chunks == null || chunks.isEmpty()) {
            prompt.append("未找到相关知识库内容。请根据你的知识回答。\n\n");
        } else {
            prompt.append("检索结果：\n\n");
            List<DocumentChunk> sorted = chunks.stream()
                    .sorted(Comparator.comparing(DocumentChunk::getScore).reversed())
                    .collect(Collectors.toList());

            for (int i = 0; i < sorted.size(); i++) {
                DocumentChunk chunk = sorted.get(i);
                prompt.append(String.format("[来源%d] %s (相似度: %.1f%%)\n%s\n\n",
                        i + 1,
                        chunk.getDocTitle(),
                        chunk.getScore() * 100,
                        chunk.getContent()));
            }
        }

        prompt.append("用户问题：").append(userQuery);
        return prompt.toString();
    }

    /**
     * 将文档分块转换为前端可用的来源信息列表
     *
     * @param chunks 检索到的文档分块列表（可为 null 或空）
     * @return 排序后的来源信息列表
     */
    public List<SourceInfo> buildSourceInfoList(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        return chunks.stream()
                .sorted(Comparator.comparing(DocumentChunk::getScore).reversed())
                .map(chunk -> SourceInfo.builder()
                        .docTitle(chunk.getDocTitle())
                        .score(chunk.getScore())
                        .category(chunk.getCategory())
                        .relevance(getRelevanceLabel(chunk.getScore()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 根据相似度分数返回中文相关度标签
     */
    private String getRelevanceLabel(double score) {
        if (score >= 0.7) {
            return "高度相关";
        }
        if (score >= 0.4) {
            return "一般相关";
        }
        return "低相关";
    }
}
