package com.travel.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.ai.entity.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话消息Mapper
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
}
