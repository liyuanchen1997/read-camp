package com.readcamp.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 例句收藏列表项（联表带出文章信息） */
@Data
public class FavoriteItem {

    private Long sentenceId;
    /** 例句原文 */
    private String en;
    private Integer seq;
    private Long articleId;
    private String articleTitle;
    private LocalDateTime createdAt;
}
