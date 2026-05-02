package com.travel.ai.service.impl;

import com.travel.ai.entity.ConversationMessage;
import com.travel.ai.entity.ConversationSession;
import com.travel.ai.mapper.ConversationMessageMapper;
import com.travel.ai.mapper.ConversationSessionMapper;
import com.travel.ai.service.ContextService;
import com.travel.ai.service.ConversationCompressor;
import com.travel.ai.service.UserProfileMemoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travel.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 上下文管理服务实现类
 * 使用Redis存储对话历史，支持上下文管理和指代消解
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextServiceImpl implements ContextService {

    private final ConversationMessageMapper messageMapper;
    private final RedisUtil redisUtil;
    private final ConversationSessionMapper sessionMapper;
    private final ConversationCompressor conversationCompressor;
    private final UserProfileMemoryService userProfileMemoryService;

    private static final String CONTEXT_CACHE_PREFIX = "context:";
    private static final String SUMMARY_CACHE_PREFIX = "context:summary:";
    private static final int DEFAULT_CONTEXT_LIMIT = 10; // 保存最近10条消息
    private static final int DEFAULT_ROUNDS = 3; // 默认3轮对话
    private static final long CONTEXT_CACHE_EXPIRE_HOURS = 24;
    private static final int COMPRESSION_MESSAGE_THRESHOLD = 20; // 超过20条消息(10轮)触发压缩
    private static final int RECENT_MESSAGES_TO_KEEP = 10; // 压缩时保留最近10条消息(5轮)

    // 指代词模式
    private static final Pattern[] REFERENCE_PATTERNS = {
        Pattern.compile("它|它|这个|这个.*?怎么样"),
        Pattern.compile("那个|那个.*?怎么样"),
        Pattern.compile("这里|这里.*?怎么样"),
        Pattern.compile("那里|那里.*?怎么样"),
        Pattern.compile(".*?呢")  // 简单模式
    };

    @Override
    public void addMessage(String sessionId, String role, String content) {
        try {
            // 创建消息对象
            Map<String, String> message = new HashMap<>();
            message.put("role", role);
            message.put("content", content);
            message.put("timestamp", LocalDateTime.now().toString());

            // 添加到Redis列表
            String cacheKey = CONTEXT_CACHE_PREFIX + sessionId;
            redisUtil.lRightPush(cacheKey, message);

            // 限制列表长度
            Long size = redisUtil.lSize(cacheKey);
            if (size != null && size > DEFAULT_CONTEXT_LIMIT) {
                // 移除最旧的消息
                redisUtil.lLeftPop(cacheKey);
            }

            // 设置过期时间
            redisUtil.expire(cacheKey, CONTEXT_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

            log.debug("添加消息到上下文: sessionId={}, role={}", sessionId, role);

            // Check if compression is needed (over 20 messages = 10 rounds)
            compressHistoryIfNeeded(sessionId, cacheKey);

        } catch (Exception e) {
            log.error("添加消息失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * Check conversation history size and compress if it exceeds threshold.
     * When history exceeds 20 messages (10 rounds), older messages are
     * summarized by the ConversationCompressor and the summary is stored
     * in the ConversationSession entity.
     */
    private void compressHistoryIfNeeded(String sessionId, String cacheKey) {
        try {
            Long totalSize = redisUtil.lSize(cacheKey);
            if (totalSize == null || totalSize <= COMPRESSION_MESSAGE_THRESHOLD) {
                return;
            }

            log.info("对话历史超过{}条消息，触发压缩: sessionId={}, messageCount={}",
                    COMPRESSION_MESSAGE_THRESHOLD, sessionId, totalSize);

            // Get older messages (all except recent ones to keep)
            int olderCount = totalSize.intValue() - RECENT_MESSAGES_TO_KEEP;
            List<Object> olderRaw = redisUtil.lRange(cacheKey, 0, Math.max(0, olderCount - 1));

            // Convert to text list for compressor
            List<String> olderTexts = new ArrayList<>();
            for (Object msg : olderRaw) {
                if (msg instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> messageMap = (Map<String, String>) msg;
                    String roleName = "user".equals(messageMap.get("role")) ? "用户" : "助手";
                    olderTexts.add(roleName + ": " + messageMap.get("content"));
                }
            }

            // Compress older messages
            String summary = conversationCompressor.compressIfNeeded(olderTexts);

            if (summary != null) {
                // Store summary in session
                storeSessionSummary(sessionId, summary);
            }
        } catch (Exception e) {
            log.warn("压缩历史记录失败，不影响主流程: sessionId={}", sessionId, e);
        }
    }

    /**
     * Store the generated summary in the ConversationSession entity.
     */
    private void storeSessionSummary(String sessionId, String summary) {
        try {
            QueryWrapper<ConversationSession> query = new QueryWrapper<>();
            query.eq("session_id", sessionId);
            ConversationSession session = sessionMapper.selectOne(query);

            if (session != null) {
                session.setSummary(summary);
                session.setUpdatedAt(LocalDateTime.now());
                sessionMapper.updateById(session);
                log.info("会话摘要已更新: sessionId={}, summaryLength={}", sessionId, summary.length());
            } else {
                log.warn("未找到会话记录，跳过摘要存储: sessionId={}", sessionId);
            }
        } catch (Exception e) {
            log.error("存储会话摘要失败: sessionId={}", sessionId, e);
        }
    }

    @Override
    public List<Map<String, String>> getHistory(String sessionId, int limit) {
        try {
            String cacheKey = CONTEXT_CACHE_PREFIX + sessionId;

            // 从Redis获取最近的消息
            Long size = redisUtil.lSize(cacheKey);
            if (size == null || size == 0) {
                return new ArrayList<>();
            }

            // 获取最近的消息（从最新到最旧）
            int actualLimit = Math.min(limit, size.intValue());
            List<Object> messages = redisUtil.lRange(cacheKey, -actualLimit, -1);

            List<Map<String, String>> result = new ArrayList<>();
            for (Object msg : messages) {
                if (msg instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> messageMap = (Map<String, String>) msg;
                    result.add(messageMap);
                }
            }

            // 反转顺序（从旧到新）
            Collections.reverse(result);
            return result;

        } catch (Exception e) {
            log.error("获取对话历史失败: sessionId={}", sessionId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getFormattedContext(String sessionId, int limit) {
        List<Map<String, String>> history = getHistory(sessionId, limit);

        if (history.isEmpty()) {
            return "";
        }

        // 格式化为AI模型可理解的上下文
        StringBuilder context = new StringBuilder();
        context.append("以下是我们的对话历史，请参考这些信息回答问题：\n\n");

        // Include session summary if available
        String sessionSummary = getSessionSummary(sessionId);
        if (sessionSummary != null && !sessionSummary.isEmpty()) {
            context.append("【历史摘要】\n").append(sessionSummary).append("\n\n");
        }

        for (Map<String, String> message : history) {
            String roleName = "user".equals(message.get("role")) ? "用户" : "助手";
            context.append(roleName).append(": ").append(message.get("content")).append("\n");
        }

        return context.toString();
    }

    /**
     * Retrieve the summary for a given session from the database.
     */
    private String getSessionSummary(String sessionId) {
        try {
            QueryWrapper<ConversationSession> query = new QueryWrapper<>();
            query.eq("session_id", sessionId);
            ConversationSession session = sessionMapper.selectOne(query);
            if (session != null) {
                return session.getSummary();
            }
        } catch (Exception e) {
            log.warn("获取会话摘要失败: sessionId={}", sessionId, e);
        }
        return null;
    }

    @Override
    public void clearContext(String sessionId) {
        try {
            String cacheKey = CONTEXT_CACHE_PREFIX + sessionId;
            redisUtil.delete(cacheKey);
            log.info("清除对话上下文: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("清除上下文失败: sessionId={}", sessionId, e);
        }
    }

    @Override
    public List<Map<String, String>> getAllMessages(String sessionId) {
        return getHistory(sessionId, DEFAULT_CONTEXT_LIMIT);
    }

    @Override
    public void saveMessage(String sessionId, Long userId, String role, String content, String intentType) {
        try {
            // 保存到数据库
            ConversationMessage message = new ConversationMessage();
            message.setSessionId(sessionId);
            message.setUserId(userId);
            message.setRole(role);
            message.setContent(content);
            message.setIntentType(intentType);
            message.setCreatedAt(LocalDateTime.now());

            messageMapper.insert(message);

            log.debug("保存消息到数据库: sessionId={}, role={}", sessionId, role);

        } catch (Exception e) {
            log.error("保存消息失败: sessionId={}", sessionId, e);
        }
    }

    @Override
    public String resolveReference(String text, List<Map<String, String>> context) {
        if (context == null || context.isEmpty()) {
            return text;
        }

        // 简单的指代消解逻辑
        String resolved = text;

        // 检查是否包含指代词
        for (Pattern pattern : REFERENCE_PATTERNS) {
            Matcher matcher = pattern.matcher(resolved);
            if (matcher.find()) {
                // 获取最后一条用户消息作为上下文
                for (int i = context.size() - 1; i >= 0; i--) {
                    Map<String, String> message = context.get(i);
                    if ("user".equals(message.get("role"))) {
                        String lastUserMessage = message.get("content");
                        // 简单替换指代词为上下文中的实体
                        resolved = resolved.replaceAll("它|这个", "关于" + extractEntity(lastUserMessage));
                        break;
                    }
                }
                break;
            }
        }

        return resolved;
    }

    @Override
    public boolean isContextExpired(String sessionId, int expireMinutes) {
        try {
            String cacheKey = CONTEXT_CACHE_PREFIX + sessionId;
            Long ttl = redisUtil.getExpire(cacheKey);

            if (ttl == null || ttl == -1) {
                return false; // 不存在或永久有效
            }

            // 检查是否过期
            long expireSeconds = TimeUnit.MINUTES.toSeconds(expireMinutes);
            return ttl < expireSeconds;

        } catch (Exception e) {
            log.error("检查上下文过期失败: sessionId={}", sessionId, e);
            return true;
        }
    }

    @Override
    public List<Map<String, String>> getRecentRounds(String sessionId, int rounds) {
        List<Map<String, String>> allMessages = getHistory(sessionId, rounds * 2);

        if (allMessages.isEmpty()) {
            return new ArrayList<>();
        }

        // 按轮次分组（用户+助手为一轮）
        List<Map<String, String>> recentRounds = new ArrayList<>();
        int messageCount = 0;
        int maxMessages = rounds * 2;

        // 从最新的消息开始
        for (int i = allMessages.size() - 1; i >= 0 && messageCount < maxMessages; i--) {
            recentRounds.add(0, allMessages.get(i));
            messageCount++;
        }

        // 反转回正确顺序
        Collections.reverse(recentRounds);
        return recentRounds;
    }

    @Override
    public String getCompressedContext(String sessionId, int recentRounds) {
        try {
            String cacheKey = CONTEXT_CACHE_PREFIX + sessionId;
            Long size = redisUtil.lSize(cacheKey);

            if (size == null || size == 0) {
                return "";
            }

            int totalMessages = size.intValue();
            int recentMessageCount = recentRounds * 2;

            // 如果消息总数不足以触发压缩，直接返回格式化上下文
            if (totalMessages <= COMPRESSION_MESSAGE_THRESHOLD) {
                return getFormattedContext(sessionId, recentMessageCount);
            }

            // 获取最近N轮消息（保持完整）
            List<Map<String, String>> recentMessages = getHistory(sessionId, recentMessageCount);

            // 获取较早的消息用于压缩
            int olderCount = totalMessages - recentMessageCount;
            List<Object> olderRaw = redisUtil.lRange(cacheKey, 0, Math.max(0, olderCount - 1));

            // 检查是否已有缓存的摘要
            String summaryKey = SUMMARY_CACHE_PREFIX + sessionId;
            Object cachedSummary = redisUtil.get(summaryKey);
            String summary;

            if (cachedSummary != null && !cachedSummary.toString().isEmpty()) {
                summary = cachedSummary.toString();
                log.debug("使用缓存的对话摘要: sessionId={}", sessionId);
            } else {
                // 将较早的消息转为文本列表进行压缩
                List<String> olderTexts = new ArrayList<>();
                for (Object msg : olderRaw) {
                    if (msg instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> messageMap = (Map<String, String>) msg;
                        String roleName = "user".equals(messageMap.get("role")) ? "用户" : "助手";
                        olderTexts.add(roleName + ": " + messageMap.get("content"));
                    }
                }

                summary = conversationCompressor.compressIfNeeded(olderTexts);

                if (summary != null) {
                    // 缓存摘要，设置与上下文相同的过期时间
                    redisUtil.set(summaryKey, summary);
                    redisUtil.expire(summaryKey, CONTEXT_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                    log.info("对话摘要已缓存: sessionId={}, summaryLength={}", sessionId, summary.length());
                }
            }

            // 构建完整上下文：摘要 + 最近对话
            StringBuilder context = new StringBuilder();
            if (summary != null && !summary.isEmpty()) {
                context.append("【对话摘要】\n").append(summary).append("\n\n");
            }
            context.append("【最近对话】\n");
            for (Map<String, String> message : recentMessages) {
                String roleName = "user".equals(message.get("role")) ? "用户" : "助手";
                context.append(roleName).append(": ").append(message.get("content")).append("\n");
            }

            return context.toString();

        } catch (Exception e) {
            log.error("获取压缩上下文失败: sessionId={}", sessionId, e);
            // 降级到普通格式化上下文
            return getFormattedContext(sessionId, recentRounds * 2);
        }
    }

    @Override
    public String buildFullContext(String sessionId, String userId, int recentRounds) {
        // 1. Get compressed conversation context
        String conversationContext = getCompressedContext(sessionId, recentRounds);

        // 2. If userId is null or blank, return only conversation context
        if (userId == null || userId.isBlank()) {
            return conversationContext;
        }

        // 3. Get user profile context with graceful degradation
        String profileContext;
        try {
            profileContext = userProfileMemoryService.getProfileContext(userId);
        } catch (Exception e) {
            log.warn("获取用户画像失败，仅使用对话上下文: userId={}", userId, e);
            return conversationContext;
        }

        // 4. If no meaningful profile, return only conversation context
        if (profileContext == null || profileContext.isBlank()
                || "暂无用户画像信息。".equals(profileContext)) {
            return conversationContext;
        }

        // 5. Combine: profile first, then conversation
        StringBuilder fullContext = new StringBuilder();
        fullContext.append("【用户画像】\n").append(profileContext).append("\n\n");

        if (conversationContext != null && !conversationContext.isBlank()) {
            fullContext.append(conversationContext);
        }

        return fullContext.toString();
    }

    /**
     * 从消息中提取实体（简单实现）
     */
    private String extractEntity(String message) {
        // 简单的实体提取：取前10个字符
        if (message.length() > 10) {
            return message.substring(0, 10) + "...";
        }
        return message;
    }
}
