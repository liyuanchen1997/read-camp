package com.readcamp.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 例句收藏列表项（联表带出文章信息与 AI 标注） */
@Data
public class FavoriteItem {

    private Long sentenceId;
    /** 例句原文 */
    private String en;
    /** 中文翻译（AI 标注，可能为 null） */
    private String zh;
    /** 中文讲解（AI 标注，可能为 null） */
    private String explanation;
    /** 单词标注 [{word,pos,meaning,role,phonetic}]（AI 标注，可能为 null） */
    private java.util.List<java.util.Map<String, String>> words;
    private Integer seq;
    private Long articleId;
    private String articleTitle;
    private LocalDateTime createdAt;
}
