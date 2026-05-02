package com.travel.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * RAG检索响应DTO
 */
@Data
@Builder
public class RAGSearchResponse {

    /**
     * 查询问题
     */
    private String query;

    /**
     * 检索到的文档块列表
     */
    private List<DocumentChunk> chunks;

    /**
     * 检索到的文档块数量
     */
    private Integer totalChunks;

    /**
     * 增强的上下文（用于AI生成）
     */
    private String enhancedContext;

    /**
     * 文档块信息
     */
    @Data
    @Builder
    public static class DocumentChunk {

        /**
         * 分块ID
         */
        private String chunkId;

        /**
         * 文档ID
         */
        private String docId;

        /**
         * 文档标题
         */
        private String docTitle;

        /**
         * 分块内容
         */
        private String content;

        /**
         * 相似度分数
         */
        private Double score;

        /**
         * 分类
         */
        private String category;

        /**
         * 分块序号
         */
        private Integer chunkIndex;
    }
}
