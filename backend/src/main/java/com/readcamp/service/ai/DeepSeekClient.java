package com.readcamp.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek OpenAI 兼容客户端（/chat/completions）
 * 密钥来自配置（${DEEPSEEK_API_KEY}），禁止硬编码
 */
@Slf4j
@Component
public class DeepSeekClient {

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int timeoutSeconds;

    public DeepSeekClient(
            @Value("${readcamp.ai.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${readcamp.ai.api-key:}") String apiKey,
            @Value("${readcamp.ai.model:deepseek-v4-flash}") String model,
            @Value("${readcamp.ai.timeout-seconds:120}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("readcamp.ai.api-key 未配置（环境变量 DEEPSEEK_API_KEY）");
        }
        // 连接/读取超时（reasoning 模型响应可能较慢）
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        this.client = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 对话补全，要求返回 JSON 对象。
     *
     * @return 模型输出的原始 JSON 文本（可能带 ```json 围栏，由调用方解析防护）
     */
    public String chatJson(String systemPrompt, String userPrompt, int maxTokens) throws AiCallException {
        Map<String, Object> body = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "max_tokens", maxTokens,
                "temperature", 0.3);

        try {
            String response = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new AiCallException("DeepSeek 调用失败: " + e.getMessage(), e);
        }
    }

    /** AI 调用异常（批级重试判定用） */
    public static class AiCallException extends RuntimeException {
        public AiCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
