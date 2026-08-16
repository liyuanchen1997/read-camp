package com.readcamp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 启动生成请求 */
@Data
public class GenerateRequest {

    /** missing=只生成未生成的句子（默认）；all=全部重新生成 */
    @NotBlank(message = "target 不能为空")
    private String target;

    @Min(value = 1, message = "batchSize 最小 1")
    @Max(value = 20, message = "batchSize 最大 20")
    private Integer batchSize;
}
