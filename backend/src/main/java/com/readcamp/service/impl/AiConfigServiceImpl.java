package com.readcamp.service.impl;

import com.readcamp.dto.AiConfigDto;
import com.readcamp.entity.AiConfig;
import com.readcamp.mapper.AiConfigMapper;
import com.readcamp.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 模型配置服务：DB（ai_config 单行）为运行时唯一事实源，
 * application.yml 仅作初始默认值（首次访问落库）。
 */
@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {

    private static final Long CONFIG_ID = 1L;

    private final AiConfigMapper configMapper;

    @Value("${readcamp.ai.base-url:https://api.deepseek.com}")
    private String defaultBaseUrl;

    @Value("${readcamp.ai.api-key:}")
    private String defaultApiKey;

    @Value("${readcamp.ai.model:deepseek-v4-flash}")
    private String defaultModel;

    @Value("${readcamp.ai.batch-size:3}")
    private int defaultBatchSize;

    @Value("${readcamp.ai.timeout-seconds:120}")
    private int defaultTimeoutSeconds;

    private volatile AiConfig cache;

    @Override
    public AiConfig get() {
        AiConfig config = cache;
        if (config == null) {
            synchronized (this) {
                if (cache == null) {
                    cache = loadOrInit();
                }
                config = cache;
            }
        }
        return config;
    }

    private AiConfig loadOrInit() {
        AiConfig config = configMapper.selectById(CONFIG_ID);
        if (config == null) {
            // 首次：yml 默认值落库
            config = new AiConfig();
            config.setId(CONFIG_ID);
            config.setBaseUrl(defaultBaseUrl);
            config.setApiKey(defaultApiKey == null ? "" : defaultApiKey);
            config.setModel(defaultModel);
            config.setBatchSize(defaultBatchSize);
            config.setTemperature(new BigDecimal("0.3"));
            config.setTimeoutSeconds(defaultTimeoutSeconds);
            configMapper.insert(config);
        }
        return config;
    }

    @Override
    public AiConfig update(Long operatorId, AiConfigDto dto) {
        AiConfig config = get();
        config.setBaseUrl(dto.getBaseUrl().trim());
        config.setApiKey(dto.getApiKey() == null ? "" : dto.getApiKey().trim());
        config.setModel(dto.getModel().trim());
        config.setBatchSize(dto.getBatchSize());
        config.setTemperature(dto.getTemperature());
        config.setTimeoutSeconds(dto.getTimeoutSeconds());
        config.setUpdatedBy(operatorId);
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(config);
        cache = config;
        return config;
    }
}
