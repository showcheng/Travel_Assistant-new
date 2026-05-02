package com.travel.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档实体
 */
@Data
@TableName("knowledge_document")
public class KnowledgeDocument {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档ID
     */
    private String docId;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档分类
     */
    private String category;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 处理状态: PROCESSING, COMPLETED, FAILED
     */
    private String status;

    /**
     * 分块数量
     */
    private Integer chunkCount;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 处理完成时间
     */
    private LocalDateTime processedTime;

    /**
     * 创建者用户ID
     */
    private Long createdBy;
}
