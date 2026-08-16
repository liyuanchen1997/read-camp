package com.readcamp.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readcamp.entity.AiConfig;
import com.readcamp.service.AiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容客户端（/chat/completions）
 * 运行时从 ai_config 表读取 base_url / api_key / model / temperature / timeout，
 * 支持切换任意 OpenAI 兼容服务（DeepSeek、通义、本地 Ollama 等），
 * 配置变更后按 (baseUrl+apiKey) 重建连接，下次生成即生效。
 */
@Slf4j
@Component
public class DeepSeekClient {

    private final ObjectMapper objectMapper;
    private final AiConfigService configService;

    /** 按 (baseUrl + apiKey) 缓存的客户端 */
    private volatile RestClient client;
    private volatile String clientKey;

    public DeepSeekClient(AiConfigService configService, ObjectMapper objectMapper) {
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    private RestClient client() {
        AiConfig config = configService.get();
        String key = config.getBaseUrl() + "|" + config.getApiKey();
        RestClient current = client;
        if (current == null || !key.equals(clientKey)) {
            synchronized (this) {
                if (client == null || !key.equals(clientKey)) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(config.getTimeoutSeconds() * 1000);
                    factory.setReadTimeout(config.getTimeoutSeconds() * 1000);
                    client = RestClient.builder()
                            .requestFactory(factory)
                            .baseUrl(config.getBaseUrl())
                            .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .build();
                    clientKey = key;
                    log.info("[ai-config] 已切换模型服务: {} / {}", config.getBaseUrl(), config.getModel());
                }
                current = client;
            }
        }
        return current;
    }

    /**
     * 对话补全，要求返回 JSON 对象。
     *
     * @return 模型输出的原始 JSON 文本（可能带 ```json 围栏，由调用方解析防护）
     */
    public String chatJson(String systemPrompt, String userPrompt, int maxTokens) throws AiCallException {
        AiConfig config = configService.get();
        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "max_tokens", maxTokens,
                "temperature", config.getTemperature());

        try {
            String response = client().post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new AiCallException("模型调用失败: " + e.getMessage(), e);
        }
    }

    /** AI 调用异常（批级重试判定用） */
    public static class AiCallException extends RuntimeException {
        public AiCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
