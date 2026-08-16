package com.readcamp.dto;

import com.readcamp.entity.Article;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 文章信息响应 */
@Data
public class ArticleDto {

    private Long id;
    private String title;
    private String summary;
    private String coverUrl;
    private List<String> tags;
    private Integer difficulty;
    private Integer status;
    private Integer wordCount;
    private Integer sentenceCount;
    private LocalDateTime createdAt;

    public static ArticleDto from(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setSummary(article.getSummary());
        dto.setCoverUrl(article.getCoverUrl());
        dto.setTags(article.getTags());
        dto.setDifficulty(article.getDifficulty());
        dto.setStatus(article.getStatus());
        dto.setWordCount(article.getWordCount());
        dto.setSentenceCount(article.getSentenceCount());
        dto.setCreatedAt(article.getCreatedAt());
        return dto;
    }
}
