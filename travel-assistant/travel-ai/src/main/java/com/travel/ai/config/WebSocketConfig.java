package com.travel.ai.config;

import com.travel.ai.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 启用WebSocket支持并注册端点
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebSocket端点
        registry.addHandler(chatWebSocketHandler, "/api/ai/chat/stream/{sessionId}")
                .setAllowedOrigins("*"); // 允许所有来源（生产环境应限制）
    }
}
