package com.readcamp.dto;

import lombok.Data;

/** 进度上报响应 */
@Data
public class ProgressResponse {

    /** 0-100 */
    private Integer progress;
    private Boolean isCompleted;
}
