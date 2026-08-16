package com.readcamp.dto;

import com.readcamp.entity.AiConfig;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** AI 模型配置（读写共用） */
@Data
public class AiConfigDto {

    @NotBlank(message = "接口地址不能为空")
    private String baseUrl;

    /** API Key（本地模型可为空） */
    private String apiKey;

    @NotBlank(message = "模型名不能为空")
    private String model;

    @NotNull(message = "批量大小不能为空")
    @Min(value = 1, message = "批量最小 1")
    @Max(value = 20, message = "批量最大 20")
    private Integer batchSize;

    @NotNull(message = "温度不能为空")
    private BigDecimal temperature;

    @NotNull(message = "超时不能为空")
    @Min(value = 10, message = "超时最小 10 秒")
    @Max(value = 600, message = "超时最大 600 秒")
    private Integer timeoutSeconds;

    public static AiConfigDto from(AiConfig config) {
        AiConfigDto dto = new AiConfigDto();
        dto.setBaseUrl(config.getBaseUrl());
        dto.setApiKey(config.getApiKey());
        dto.setModel(config.getModel());
        dto.setBatchSize(config.getBatchSize());
        dto.setTemperature(config.getTemperature());
        dto.setTimeoutSeconds(config.getTimeoutSeconds());
        return dto;
    }
}
