package com.readcamp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 阅读进度批量上报请求（已读句索引集合） */
@Data
public class ProgressReportRequest {

    @NotNull(message = "readSentenceIndexes 不能为空")
    private List<Integer> readSentenceIndexes;
}
