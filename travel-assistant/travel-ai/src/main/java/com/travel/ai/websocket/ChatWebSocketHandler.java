package com.travel.ai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.ai.service.AIService;
import com.travel.ai.service.ContextService;
import com.travel.ai.service.IntentService;
import com.travel.ai.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket聊天处理器
 * 处理实时聊天通信和流式输出
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final AIService aiService;
    private final SessionService sessionService;
    private final ContextService contextService;
    private final IntentService intentService;
    private final ObjectMapper objectMapper;

    // 存储活跃的WebSocket会话
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    // 心跳检测间隔（30秒）
    private static final long HEARTBEAT_INTERVAL = 30000;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session);

        log.info("WebSocket连接建立: sessionId={}, sessionId={}", session.getId(), sessionId);

        // 保存会话
        activeSessions.put(sessionId, session);

        // 发送连接成功消息
        sendMessage(session, createMessage("connected", "连接成功", sessionId));

        // 启动心跳检测
        startHeartbeat(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        String payload = message.getPayload();

        log.debug("收到WebSocket消息: sessionId={}, message={}", sessionId, payload);

        try {
            // 解析消息
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String userMessage = (String) messageData.get("message");
            String messageType = (String) messageData.getOrDefault("type", "message");

            // 处理不同类型的消息
            switch (messageType) {
                case "message":
                    handleChatMessage(session, sessionId, userMessage);
                    break;
                case "heartbeat":
                    // 心跳响应
                    sendMessage(session, createMessage("heartbeat", "pong", sessionId));
                    break;
                default:
                    sendMessage(session, createMessage("error", "未知消息类型", sessionId));
            }

        } catch (Exception e) {
            log.error("处理WebSocket消息失败: sessionId={}", sessionId, e);
            sendMessage(session, createMessage("error", "消息处理失败: " + e.getMessage(), sessionId));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = extractSessionId(session);

        log.info("WebSocket连接关闭: sessionId={}, status={}", sessionId, status);

        // 移除会话
        activeSessions.remove(sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = extractSessionId(session);

        log.error("WebSocket传输错误: sessionId={}", sessionId, exception);

        // 移除会话
        activeSessions.remove(sessionId);
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(WebSocketSession session, String sessionId, String userMessage) {
        try {
            // 1. 识别意图
            var intent = intentService.recognizeIntent(userMessage);

            // 2. 添加用户消息到上下文
            contextService.addMessage(sessionId, "user", userMessage);

            // 3. 构建AI请求（包含上下文）
            String context = contextService.getFormattedContext(sessionId, 5);
            String fullMessage = context + "\n用户: " + userMessage + "\n助手:";

            // 4. 调用AI服务
            String aiResponse = aiService.chatStream(fullMessage);

            // 5. 流式发送AI回复
            streamResponse(session, sessionId, aiResponse, intent.getCode());

            // 6. 保存助手回复到上下文
            contextService.addMessage(sessionId, "assistant", aiResponse);

            // 7. 保存到数据库
            contextService.saveMessage(sessionId, 1L, "assistant", aiResponse, intent.getCode());

        } catch (Exception e) {
            log.error("处理聊天消息失败: sessionId={}", sessionId, e);
            sendMessage(session, createMessage("error", "处理失败: " + e.getMessage(), sessionId));
        }
    }

    /**
     * 流式发送AI回复
     */
    private void streamResponse(WebSocketSession session, String sessionId, String response, String intent) {
        try {
            // 模拟流式输出（将响应分块发送）
            int chunkSize = 20;
            int totalLength = response.length();

            for (int i = 0; i < totalLength; i += chunkSize) {
                int end = Math.min(i + chunkSize, totalLength);
                String chunk = response.substring(i, end);

                // 发送文本块
                Map<String, Object> chunkData = new HashMap<>();
                chunkData.put("type", "token");
                chunkData.put("content", chunk);
                chunkData.put("sessionId", sessionId);
                chunkData.put("intent", intent);
                chunkData.put("finished", false);

                sendMessage(session, objectMapper.writeValueAsString(chunkData));

                // 模拟打字延迟
                Thread.sleep(50);
            }

            // 发送完成标记
            Map<String, Object> finishData = new HashMap<>();
            finishData.put("type", "done");
            finishData.put("sessionId", sessionId);
            finishData.put("intent", intent);
            finishData.put("finished", true);
            finishData.put("tokens", response.length());

            sendMessage(session, objectMapper.writeValueAsString(finishData));

        } catch (Exception e) {
            log.error("流式发送失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 发送消息到WebSocket
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    /**
     * 创建标准消息格式
     */
    private String createMessage(String type, String content, String sessionId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", type);
            message.put("content", content);
            message.put("sessionId", sessionId);
            message.put("timestamp", System.currentTimeMillis());
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("创建消息失败", e);
            return "{\"type\":\"error\",\"content\":\"消息格式化失败\"}";
        }
    }

    /**
     * 从会话中提取sessionId
     */
    private String extractSessionId(WebSocketSession session) {
        String uri = session.getUri().toString();
        return uri.substring(uri.lastIndexOf('/') + 1);
    }

    /**
     * 启动心跳检测
     */
    private void startHeartbeat(WebSocketSession session) {
        new Thread(() -> {
            while (session.isOpen()) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL);
                    if (session.isOpen()) {
                        sendMessage(session, createMessage("heartbeat", "ping", extractSessionId(session)));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("心跳发送失败", e);
                    break;
                }
            }
        }).start();
    }

    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * 向特定会话发送消息
     */
    public void sendToSession(String sessionId, String message) {
        WebSocketSession session = activeSessions.get(sessionId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        }
    }

    /**
     * 向所有会话广播消息
     */
    public void broadcast(String message) {
        activeSessions.forEach((id, session) -> {
            sendMessage(session, message);
        });
    }
}
