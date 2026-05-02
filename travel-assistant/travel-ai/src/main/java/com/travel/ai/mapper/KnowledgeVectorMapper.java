package com.travel.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.ai.entity.KnowledgeVector;
import org.apache.ibatis.annotations.Mapper;

/**
 * 向量存储记录Mapper
 */
@Mapper
public interface KnowledgeVectorMapper extends BaseMapper<KnowledgeVector> {
}
