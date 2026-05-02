package com.travel.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对话摘要压缩服务
 *
 * 论文 5.3.3: 当对话超过10轮（20条消息）时，将较早的对话压缩为摘要，
 * 保留最近5轮（10条消息）完整不变。
 *
 * 压缩策略：
 * 1. 优先使用 LLM 生成摘要（保留关键实体、需求、待解决问题）
 * 2. LLM 不可用时降级到首句提取（从每条消息提取第一个完整句子）
 */
@Slf4j
@Service
public class ConversationCompressor {

    private static final int COMPRESSION_THRESHOLD = 10;

    private static final String SUMMARY_PROMPT_TEMPLATE =
            "请将以下对话历史压缩成一段简短的摘要，保留关键信息：\n"
            + "1. 用户的主要需求\n"
            + "2. 已讨论的关键点\n"
            + "3. 待解决的问题\n\n"
            + "对话历史：\n%s\n\n摘要：";

    private static final String DISTILLATION_PROMPT_TEMPLATE =
            "请分析以下对话历史，完成两个任务：\n"
            + "1. 生成一段简短摘要（保留用户的主要需求、已讨论的关键点、待解决的问题）\n"
            + "2. 从对话中提取用户的旅行偏好信息\n\n"
            + "请严格返回以下JSON格式，不要返回其他内容：\n"
            + "{\n"
            + "  \"summary\": \"对话摘要文本\",\n"
            + "  \"preferences\": [\n"
            + "    {\"key\": \"偏好键\", \"value\": \"偏好值\", \"confidence\": 0.0-1.0}\n"
            + "  ]\n"
            + "}\n\n"
            + "偏好键可选：travel_style, budget_range, travel_companion, dietary, pace_preference, destination\n"
            + "confidence越高表示越确定。只提取明确提到的信息，不要猜测。\n"
            + "如果对话中没有明确的偏好信息，preferences返回空数组。\n\n"
            + "对话历史：\n%s";

    private final ChatLanguageModel chatLanguageModel;

    public ConversationCompressor(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    /**
     * 判断是否需要压缩，如果需要则执行压缩。
     *
     * 当消息数量超过阈值（10条）时，调用 LLM 或降级方法生成摘要。
     *
     * @param oldMessages 需要压缩的历史消息列表
     * @return 压缩后的摘要文本，如果不需要压缩则返回 null
     */
    public String compressIfNeeded(List<String> oldMessages) {
        if (oldMessages == null || oldMessages.size() <= COMPRESSION_THRESHOLD) {
            return null;
        }
        return compressWithFallback(oldMessages);
    }

    /**
     * 带降级机制的压缩方法。
     *
     * 优先使用 LLM 生成摘要，如果 LLM 失败或返回空则降级到首句提取。
     *
     * @param messages 待压缩的消息列表
     * @return 压缩后的摘要，输入为 null 或空时返回 null
     */
    public String compressWithFallback(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        try {
            String prompt = buildSummaryPrompt(messages);
            String summary = chatLanguageModel.generate(prompt);
            if (summary != null && !summary.isBlank()) {
                log.info("LLM摘要生成成功: {} chars", summary.length());
                return summary;
            }
        } catch (Exception e) {
            log.warn("LLM摘要生成失败，降级到首句提取: {}", e.getMessage());
        }

        return extractFirstSentences(messages);
    }

    /**
     * 构建发送给 LLM 的摘要提示词。
     *
     * @param messages 对话消息列表
     * @return 完整的提示词字符串
     */
    public String buildSummaryPrompt(List<String> messages) {
        StringBuilder sb = new StringBuilder();
        for (String msg : messages) {
            sb.append(msg).append("\n");
        }
        return String.format(SUMMARY_PROMPT_TEMPLATE, sb.toString());
    }

    /**
     * 降级方案：从每条消息中提取第一句话。
     *
     * 以句号、问号、感叹号作为句子边界，超长句子截断到20字符。
     *
     * @param messages 消息列表
     * @return 拼接后的摘要文本
     */
    private String extractFirstSentences(List<String> messages) {
        StringBuilder summary = new StringBuilder("对话摘要：");
        for (String msg : messages) {
            int end = msg.length();
            for (int i = 0; i < msg.length(); i++) {
                char c = msg.charAt(i);
                if (c == '。' || c == '？' || c == '！'
                        || c == '.' || c == '?' || c == '!') {
                    end = i + 1;
                    break;
                }
            }
            String sentence = msg.substring(0, Math.min(end, msg.length()));
            if (sentence.length() > 20) {
                sentence = sentence.substring(0, 20) + "...";
            }
            summary.append(sentence);
        }
        return summary.toString();
    }

    // ====================================================================
    // Structured distillation: summary + preferences extraction
    // ====================================================================

    /**
     * 结构化蒸馏结果，同时包含文本摘要和提取的偏好JSON。
     */
    @Data
    @Builder
    public static class DistillationResult {
        /** 文本摘要 */
        private String summary;
        /** 结构化偏好JSON（仅含非空的 preferences 数组） */
        private String extractedJson;
        /** 是否使用了LLM（false表示降级到首句提取） */
        private boolean usedLlm;
    }

    /**
     * 对对话进行结构化蒸馏，同时生成摘要和提取偏好。
     *
     * 发送增强的提示词给LLM，要求返回包含summary和preferences的JSON。
     * 如果LLM失败或返回不可解析的内容，降级到compressWithFallback获取摘要。
     *
     * @param messages 待蒸馏的消息列表
     * @return 包含摘要和偏好JSON的蒸馏结果；消息不足阈值时返回null
     */
    public DistillationResult distill(List<String> messages) {
        if (messages == null || messages.size() <= COMPRESSION_THRESHOLD) {
            return null;
        }

        try {
            String prompt = buildDistillationPrompt(messages);
            String response = chatLanguageModel.generate(prompt);

            if (response != null && !response.isBlank()) {
                DistillationResult result = parseDistillationResponse(response);
                if (result != null) {
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("LLM distillation failed, falling back to compressWithFallback: {}", e.getMessage());
        }

        // Fallback: use existing compressWithFallback for summary, no preferences
        String fallbackSummary = compressWithFallback(messages);
        return DistillationResult.builder()
                .summary(fallbackSummary)
                .extractedJson(null)
                .usedLlm(false)
                .build();
    }

    /**
     * 构建发送给LLM的结构化蒸馏提示词。
     *
     * @param messages 对话消息列表
     * @return 完整的提示词字符串
     */
    public String buildDistillationPrompt(List<String> messages) {
        StringBuilder sb = new StringBuilder();
        for (String msg : messages) {
            sb.append(msg).append("\n");
        }
        return String.format(DISTILLATION_PROMPT_TEMPLATE, sb.toString());
    }

    /**
     * 解析LLM的蒸馏响应，提取summary和preferences。
     *
     * 支持直接JSON和markdown代码块包裹的JSON。
     *
     * @param response LLM原始响应
     * @return 解析后的DistillationResult，解析失败返回null
     */
    private DistillationResult parseDistillationResponse(String response) {
        String jsonStr = response.trim();

        // Strip markdown code blocks if present
        if (jsonStr.contains("```json")) {
            int start = jsonStr.indexOf("```json") + 7;
            int end = jsonStr.indexOf("```", start);
            if (end > start) {
                jsonStr = jsonStr.substring(start, end).trim();
            }
        } else if (jsonStr.contains("```")) {
            int start = jsonStr.indexOf("```") + 3;
            int end = jsonStr.indexOf("```", start);
            if (end > start) {
                jsonStr = jsonStr.substring(start, end).trim();
            }
        }

        try {
            JSONObject json = JSON.parseObject(jsonStr);
            if (json == null) {
                return null;
            }

            String summary = json.getString("summary");
            if (summary == null || summary.isBlank()) {
                return null;
            }

            // Extract preferences as JSON string if non-empty
            String extractedJson = null;
            com.alibaba.fastjson2.JSONArray prefs = json.getJSONArray("preferences");
            if (prefs != null && !prefs.isEmpty()) {
                extractedJson = prefs.toJSONString();
            }

            return DistillationResult.builder()
                    .summary(summary)
                    .extractedJson(extractedJson)
                    .usedLlm(true)
                    .build();
        } catch (Exception e) {
            log.debug("Failed to parse distillation response as JSON: {}", e.getMessage());
            return null;
        }
    }
}
