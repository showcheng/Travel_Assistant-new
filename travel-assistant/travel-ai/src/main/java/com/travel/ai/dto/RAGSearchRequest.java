package com.travel.ai.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * RAG检索请求DTO
 */
@Data
public class RAGSearchRequest {

    /**
     * 查询问题
     */
    @NotBlank(message = "查询问题不能为空")
    private String query;

    /**
     * 返回结果数量（默认3，最多10）
     */
    @Min(value = 1, message = "至少返回1个结果")
    @Max(value = 10, message = "最多返回10个结果")
    private Integer topK = 3;

    /**
     * 分类过滤（可选）
     */
    private String category;

    /**
     * 相似度阈值（默认0.6）
     */
    private Double threshold = 0.6;
}
