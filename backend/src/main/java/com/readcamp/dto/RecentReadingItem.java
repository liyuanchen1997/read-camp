package com.readcamp.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 近期阅读列表项 */
@Data
public class RecentReadingItem {

    private Long articleId;
    private String title;
    private String coverUrl;
    private Integer difficulty;
    /** 0-100 */
    private Integer progress;
    private Boolean isCompleted;
    private LocalDateTime lastReadAt;
    private LocalDateTime completedAt;
}
