package com.travel.ai.service.impl;

import com.travel.ai.entity.ConversationSession;
import com.travel.ai.entity.ConversationMessage;
import com.travel.ai.mapper.ConversationMessageMapper;
import com.travel.ai.mapper.ConversationSessionMapper;
import com.travel.ai.service.SessionService;
import com.travel.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 会话管理服务实现类
 * 使用Redis缓存 + MySQL持久化的双层架构
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final ConversationSessionMapper sessionMapper;
    private final ConversationMessageMapper messageMapper;
    private final RedisUtil redisUtil;

    private static final String SESSION_CACHE_PREFIX = "session:";
    private static final String SESSION_CACHE_KEY_PREFIX = "session:data:";
    private static final long SESSION_CACHE_EXPIRE_HOURS = 24;

    @Override
    public String createSession(Long userId) {
        // 生成唯一会话ID
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        // 创建会话对象
        ConversationSession session = new ConversationSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setTitle("新对话");
        session.setStatus("ACTIVE");
        session.setMessageCount(0);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setExpiredAt(LocalDateTime.now().plusHours(SESSION_CACHE_EXPIRE_HOURS));

        try {
            // 保存到数据库
            sessionMapper.insert(session);

            // 缓存到Redis
            cacheSession(session);

            log.info("创建新会话: sessionId={}, userId={}", sessionId, userId);
            return sessionId;

        } catch (Exception e) {
            log.error("创建会话失败: userId={}", userId, e);
            throw new RuntimeException("创建会话失败", e);
        }
    }

    @Override
    public Object getSession(String sessionId) {
        // 先从缓存获取
        String cacheKey = SESSION_CACHE_KEY_PREFIX + sessionId;
        Object cachedSession = redisUtil.get(cacheKey);
        if (cachedSession != null) {
            return cachedSession;
        }

        // 从数据库获取
        ConversationSession session = sessionMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConversationSession>()
                .eq(ConversationSession::getSessionId, sessionId)
        );

        if (session != null) {
            // 缓存到Redis
            cacheSession(session);
            return session;
        }

        return null;
    }

    @Override
    public List<Object> getUserSessions(Long userId) {
        // 从数据库查询用户的所有会话
        List<ConversationSession> sessions = sessionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConversationSession>()
                .eq(ConversationSession::getUserId, userId)
                .orderByDesc(ConversationSession::getUpdatedAt)
        );

        return new ArrayList<>(sessions);
    }

    @Override
    public void updateSession(String sessionId, String title) {
        // 更新数据库
        ConversationSession session = new ConversationSession();
        session.setSessionId(sessionId);
        session.setTitle(title);
        session.setUpdatedAt(LocalDateTime.now());

        sessionMapper.update(session,
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConversationSession>()
                .eq(ConversationSession::getSessionId, sessionId)
        );

        // 清除缓存
        clearSessionCache(sessionId);

        log.info("更新会话标题: sessionId={}, title={}", sessionId, title);
    }

    @Override
    public void deleteSession(String sessionId) {
        try {
            // 删除数据库记录
            sessionMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConversationSession>()
                    .eq(ConversationSession::getSessionId, sessionId)
            );

            // 删除相关消息
            messageMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConversationMessage>()
                    .eq(ConversationMessage::getSessionId, sessionId)
            );

            // 清除缓存
            clearSessionCache(sessionId);

            log.info("删除会话: sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("删除会话失败: sessionId={}", sessionId, e);
            throw new RuntimeException("删除会话失败", e);
        }
    }

    @Override
    public int cleanExpiredSessions(int expireMinutes) {
        // 计算过期时间
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(expireMinutes);

        try {
            // 查询过期会话
            List<ConversationSession> expiredSessions = sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConversationSession>()
                    .lt(ConversationSession::getExpiredAt, expireTime)
            );

            if (expiredSessions.isEmpty()) {
                return 0;
            }

            // 批量删除过期会话
            int count = 0;
            for (ConversationSession session : expiredSessions) {
                try {
                    deleteSession(session.getSessionId());
                    count++;
                } catch (Exception e) {
                    log.error("删除过期会话失败: sessionId={}", session.getSessionId(), e);
                }
            }

            log.info("清理过期会话: count={}, expireMinutes={}", count, expireMinutes);
            return count;

        } catch (Exception e) {
            log.error("清理过期会话失败", e);
            return 0;
        }
    }

    @Override
    public boolean existsSession(String sessionId) {
        // 检查缓存
        String cacheKey = SESSION_CACHE_KEY_PREFIX + sessionId;
        if (redisUtil.hasKey(cacheKey)) {
            return true;
        }

        // 检查数据库
        Long count = sessionMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConversationSession>()
                .eq(ConversationSession::getSessionId, sessionId)
                .eq(ConversationSession::getStatus, "ACTIVE")
        );

        return count != null && count > 0;
    }

    @Override
    public void incrementMessageCount(String sessionId) {
        // 更新数据库
        sessionMapper.incrementMessageCount(sessionId);

        // 清除缓存，下次访问时重新加载
        clearSessionCache(sessionId);
    }

    @Override
    public Integer getMessageCount(String sessionId) {
        ConversationSession session = (ConversationSession) getSession(sessionId);
        return session != null ? session.getMessageCount() : 0;
    }

    /**
     * 缓存会话到Redis
     */
    private void cacheSession(ConversationSession session) {
        String cacheKey = SESSION_CACHE_KEY_PREFIX + session.getSessionId();
        redisUtil.set(cacheKey, session, SESSION_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 清除会话缓存
     */
    private void clearSessionCache(String sessionId) {
        String cacheKey = SESSION_CACHE_KEY_PREFIX + sessionId;
        redisUtil.delete(cacheKey);
    }
}
