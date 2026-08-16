package com.readcamp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_config")
public class AiConfig {

    /** 固定 1（单行配置） */
    @TableId(type = IdType.INPUT)
    private Long id;

    private String baseUrl;

    private String apiKey;

    private String model;

    private Integer batchSize;

    private BigDecimal temperature;

    private Integer timeoutSeconds;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}
