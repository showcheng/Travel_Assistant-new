package com.travel.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * GLM-5服务测试类
 * 测试GLM-4 API连接和基本功能
 */
@SpringBootTest
public class GLM5ServiceTest {

    private static final String API_KEY = "e217140203d34544acb721230c4f3d57.M4VERrYX2rKgB66r";
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/";

    /**
     * 测试GLM-4 API连接
     */
    @Test
    public void testGLM4Connection() {
        System.out.println("==========================================");
        System.out.println("  GLM-4 API连接测试");
        System.out.println("==========================================");

        try {
            // 创建ChatLanguageModel
            ChatLanguageModel model = OpenAiChatModel.builder()
                    .baseUrl(API_URL)
                    .apiKey(API_KEY)
                    .modelName("glm-4")
                    .temperature(0.7)
                    .maxTokens(100)
                    .build();

            // 测试简单对话
            String testMessage = "Hello, please introduce yourself in one sentence.";
            System.out.println("发送消息: " + testMessage);

            String response = model.generate(testMessage);
            System.out.println("AI回复: " + response);

            // 验证响应
            if (response != null && !response.isEmpty()) {
                System.out.println("✅ GLM-4 API连接测试成功！");
                System.out.println("响应长度: " + response.length() + " 字符");
            } else {
                System.out.println("❌ GLM-4 API连接测试失败：响应为空");
            }

        } catch (Exception e) {
            System.out.println("❌ GLM-4 API连接测试失败");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("==========================================");
    }

    /**
     * 测试中文对话
     */
    @Test
    public void testChineseDialogue() {
        System.out.println("==========================================");
        System.out.println("  GLM-4 中文对话测试");
        System.out.println("==========================================");

        try {
            ChatLanguageModel model = OpenAiChatModel.builder()
                    .baseUrl(API_URL)
                    .apiKey(API_KEY)
                    .modelName("glm-4")
                    .temperature(0.7)
                    .maxTokens(100)
                    .build();

            // 测试中文对话
            String testMessage = "你好，请用一句话介绍北京的故宫";
            System.out.println("发送消息: " + testMessage);

            String response = model.generate(testMessage);
            System.out.println("AI回复: " + response);

            if (response != null && !response.isEmpty()) {
                System.out.println("✅ 中文对话测试成功！");
            } else {
                System.out.println("❌ 中文对话测试失败");
            }

        } catch (Exception e) {
            System.out.println("❌ 中文对话测试失败");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("==========================================");
    }

    /**
     * 测试多轮对话
     */
    @Test
    public void testMultiTurnDialogue() {
        System.out.println("==========================================");
        System.out.println("  GLM-4 多轮对话测试");
        System.out.println("==========================================");

        try {
            ChatLanguageModel model = OpenAiChatModel.builder()
                    .baseUrl(API_URL)
                    .apiKey(API_KEY)
                    .modelName("glm-4")
                    .temperature(0.7)
                    .maxTokens(100)
                    .build();

            // 第一轮对话
            String message1 = "我叫小明，我喜欢旅游";
            System.out.println("第一轮 - 用户: " + message1);
            String response1 = model.generate(message1);
            System.out.println("第一轮 - AI: " + response1);

            // 第二轮对话
            String message2 = "我叫什么名字？";
            System.out.println("第二轮 - 用户: " + message2);
            String response2 = model.generate(message2);
            System.out.println("第二轮 - AI: " + response2);

            // 第三轮对话
            String message3 = "我有什么爱好？";
            System.out.println("第三轮 - 用户: " + message3);
            String response3 = model.generate(message3);
            System.out.println("第三轮 - AI: " + response3);

            System.out.println("✅ 多轮对话测试完成");

        } catch (Exception e) {
            System.out.println("❌ 多轮对话测试失败");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("==========================================");
    }
}
