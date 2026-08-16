package com.readcamp.dto;

import lombok.Data;

import java.util.List;

/**
 * 阅读载荷（GET /articles/{id}/reading 一次拉全）
 */
@Data
public class ReadingPayload {

    private ArticleDto article;
    /** 章节列表（恒非空：无章节旧文章合成单章 {id:null, seq:0, title:文章标题}） */
    private List<ChapterDto> chapters;
    private List<SentenceDto> sentences;
    /** 我的进度 */
    private ProgressView progress;
    /** 我的生词集合（全局，气泡已加状态用） */
    private List<String> vocabWords;
    /** 我收藏的句子 id 集合（本文档内） */
    private List<Long> favSentenceIds;
}
