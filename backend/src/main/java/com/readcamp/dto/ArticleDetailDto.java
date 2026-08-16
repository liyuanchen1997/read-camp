package com.readcamp.dto;

import com.readcamp.entity.Article;
import lombok.Data;

/** 管理端文章详情（含正文原文与章节，编辑回显用） */
@Data
public class ArticleDetailDto {

    private Long id;
    private String title;
    private String summary;
    private String contentEn;
    private java.util.List<String> tags;
    private Integer difficulty;
    private Integer status;
    /** 章节列表（含各章原文；旧文章为空 list） */
    private java.util.List<ChapterDto> chapters;

    public static ArticleDetailDto from(Article article) {
        ArticleDetailDto dto = new ArticleDetailDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setSummary(article.getSummary());
        dto.setContentEn(article.getContentEn());
        dto.setTags(article.getTags());
        dto.setDifficulty(article.getDifficulty());
        dto.setStatus(article.getStatus());
        return dto;
    }
}
