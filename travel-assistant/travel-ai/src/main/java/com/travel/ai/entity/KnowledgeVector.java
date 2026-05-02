package com.travel.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 向量存储记录实体
 */
@Data
@TableName("knowledge_vector")
public class KnowledgeVector {

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
     * Milvus中的向量ID
     */
    private String vectorId;

    /**
     * 向量维度
     */
    private Integer dimension;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
