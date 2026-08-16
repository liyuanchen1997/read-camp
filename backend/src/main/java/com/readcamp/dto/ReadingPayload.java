package com.readcamp.dto;

import lombok.Data;

import java.util.List;

/**
 * 阅读载荷（GET /articles/{id}/reading 一次拉全）
 * progress/vocabWords/favSentenceIds 由步骤 5 填充
 */
@Data
public class ReadingPayload {

    private ArticleDto article;
    private List<SentenceDto> sentences;
    /** 我的进度（步骤 5 填充） */
    private Object progress;
    /** 我的生词集合（步骤 5 填充） */
    private List<String> vocabWords;
    /** 我收藏的句子 id 集合（步骤 5 填充） */
    private List<Long> favSentenceIds;
}
