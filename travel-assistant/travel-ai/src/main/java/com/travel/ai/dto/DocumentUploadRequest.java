package com.travel.ai.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 文档上传请求DTO
 */
@Data
public class DocumentUploadRequest {

    /**
     * 文档标题
     */
    @NotBlank(message = "文档标题不能为空")
    private String title;

    /**
     * 文档分类
     */
    @NotBlank(message = "文档分类不能为空")
    private String category;

    /**
     * 文档内容（纯文本）
     */
    @NotBlank(message = "文档内容不能为空")
    private String content;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 创建者用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
