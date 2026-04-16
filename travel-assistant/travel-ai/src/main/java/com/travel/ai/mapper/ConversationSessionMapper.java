package com.travel.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.ai.entity.ConversationSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话会话Mapper
 */
@Mapper
public interface ConversationSessionMapper extends BaseMapper<ConversationSession> {
}
