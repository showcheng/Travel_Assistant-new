package com.travel.ai.service;

import com.travel.ai.enums.IntentType;

import java.util.Map;

/**
 * 意图识别服务接口
 * 负责识别用户对话的意图和提取实体
 */
public interface IntentService {

    /**
     * 识别用户意图
     *
     * @param message 用户消息
     * @return 意图类型
     */
    IntentType recognizeIntent(String message);

    /**
     * 提取实体信息
     *
     * @param message 用户消息
     * @param intent 意图类型
     * @return 实体信息
     */
    Map<String, Object> extractEntities(String message, IntentType intent);

    /**
     * 分析对话上下文，优化意图识别
     *
     * @param message 当前消息
     * @param context 对话上下文
     * @return 优化后的意图类型
     */
    IntentType analyzeWithContext(String message, String context);

    /**
     * 生成意图路由响应
     *
     * @param intent 意图类型
     * @param entities 实体信息
     * @return 路由指令
     */
    String generateRoute(IntentType intent, Map<String, Object> entities);

    /**
     * 检查是否需要调用外部服务
     *
     * @param intent 意图类型
     * @return 是否需要调用外部服务
     */
    boolean requiresExternalService(IntentType intent);
}
