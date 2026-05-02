package com.travel.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.ai.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档Mapper
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {
}
