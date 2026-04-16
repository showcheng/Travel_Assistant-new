package com.travel.order.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket服务
 * 用于向前端推送订单状态变更通知
 */
@Slf4j
@Component
@ServerEndpoint(value = "/ws/order/{userId}")
public class OrderWebSocketServer {

    /**
     * 在线连接数
     */
    private static AtomicInteger onlineCount = new AtomicInteger(0);

    /**
     * 存放所有在线用户的key为userId，value为WebSocketServer对象
     */
    private static ConcurrentHashMap<Long, OrderWebSocketServer> webSocketMap = new ConcurrentHashMap<>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        this.session = session;
        this.userId = userId;
        webSocketMap.put(userId, this);
        onlineCount.incrementAndGet();

        log.info("WebSocket连接建立: userId={}, 当前在线人数: {}", userId, onlineCount.get());

        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("WebSocket发送消息失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        webSocketMap.remove(userId);
        onlineCount.decrementAndGet();
        log.info("WebSocket连接关闭: userId={}, 当前在线人数: {}", userId, onlineCount.get());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到WebSocket消息: userId={}, message={}", userId, message);

        // 心跳检测
        if ("ping".equals(message)) {
            try {
                sendMessage("pong");
            } catch (IOException e) {
                log.error("WebSocket心跳响应失败: userId={}, error={}", userId, e.getMessage());
            }
        }
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket发生错误: userId={}, error={}", userId, error.getMessage(), error);
    }

    /**
     * 发送消息
     *
     * @param message 消息内容
     */
    private void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    public static void sendMessage(Long userId, String message) {
        try {
            OrderWebSocketServer server = webSocketMap.get(userId);
            if (server != null) {
                server.sendMessage(message);
                log.info("WebSocket消息发送成功: userId={}, message={}", userId, message);
            } else {
                log.warn("用户不在线: userId={}", userId);
            }
        } catch (IOException e) {
            log.error("WebSocket消息发送失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 向指定用户发送订单通知
     *
     * @param userId    用户ID
     * @param orderId   订单ID
     * @param orderNo   订单号
     * @param status    订单状态
     * @param statusText 状态描述
     */
    public static void sendOrderNotification(Long userId, Long orderId, String orderNo, Integer status, String statusText) {
        String message = String.format(
            "{\"type\":\"order\",\"orderId\":%d,\"orderNo\":\"%s\",\"status\":%d,\"statusText\":\"%s\",\"timestamp\":%d}",
            orderId, orderNo, status, statusText, System.currentTimeMillis()
        );
        sendMessage(userId, message);
    }

    /**
     * 获取当前在线人数
     */
    public static int getOnlineCount() {
        return onlineCount.get();
    }
}