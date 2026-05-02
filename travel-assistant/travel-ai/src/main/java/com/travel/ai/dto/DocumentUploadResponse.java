package com.travel.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档上传响应DTO
 */
@Data
@Builder
public class DocumentUploadResponse {

    /**
     * 文档ID
     */
    private String docId;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 处理状态
     */
    private String status;

    /**
     * 分块数量
     */
    private Integer chunkCount;

    /**
     * 消息
     */
    private String message;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
}
