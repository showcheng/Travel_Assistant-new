package com.travel.ai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 知识库来源信息DTO
 * 用于在ChatResponse中标注AI回答所引用的知识库来源
 */
@Data
@Builder
public class SourceInfo {

    /**
     * 文档标题
     */
    private String docTitle;

    /**
     * 相似度分数 (0.0 ~ 1.0)
     */
    private Double score;

    /**
     * 文档分类
     */
    private String category;

    /**
     * 相关度标签: "高度相关", "一般相关", "低相关"
     */
    private String relevance;
}
