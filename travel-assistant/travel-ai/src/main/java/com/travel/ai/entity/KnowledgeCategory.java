package com.travel.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库分类实体
 */
@Data
@TableName("knowledge_category")
public class KnowledgeCategory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类代码
     */
    private String categoryCode;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
