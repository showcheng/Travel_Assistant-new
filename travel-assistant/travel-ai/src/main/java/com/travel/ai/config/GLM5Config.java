package com.travel.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * GLM-5 AI模型配置
 * 使用OpenAI兼容接口连接智谱GLM-5
 */
@Configuration
public class GLM5Config {

    @Value("${glm5.api.key}")
    private String apiKey;

    @Value("${glm5.api.url:https://open.bigmodel.cn/api/paas/v4/}")
    private String apiUrl;

    @Value("${glm5.api.model:glm-4-flash}")
    private String modelName;

    @Value("${glm5.api.temperature:0.7}")
    private Double temperature;

    @Value("${glm5.api.max-tokens:2000}")
    private Integer maxTokens;

    @Value("${glm5.api.timeout:30000}")
    private Integer timeout;

    /**
     * 配置ChatLanguageModel用于对话
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .baseUrl(apiUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofMillis(timeout))
                .build();
    }

    /**
     * 配置EmbeddingModel用于向量化
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(apiUrl)
                .apiKey(apiKey)
                .modelName("embedding-2")  // GLM-5的向量化模型
                .timeout(Duration.ofMillis(timeout))
                .build();
    }
}
