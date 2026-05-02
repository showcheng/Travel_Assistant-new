package com.travel.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.ai.entity.ConversationSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 对话会话Mapper
 */
@Mapper
public interface ConversationSessionMapper extends BaseMapper<ConversationSession> {

    /**
     * 增加消息计数
     *
     * @param sessionId 会话ID
     * @return 影响行数
     */
    int incrementMessageCount(@Param("sessionId") String sessionId);
}
