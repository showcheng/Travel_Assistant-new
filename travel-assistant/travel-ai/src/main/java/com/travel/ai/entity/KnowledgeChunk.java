package com.travel.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档分块实体
 */
@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分块ID
     */
    private String chunkId;

    /**
     * 文档ID
     */
    private String docId;

    /**
     * 分块序号
     */
    private Integer chunkIndex;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 元数据(JSON格式)
     */
    private String metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
