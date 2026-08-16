package com.readcamp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.readcamp.common.Result;
import com.readcamp.dto.AdminStatsDto;
import com.readcamp.entity.Article;
import com.readcamp.entity.Sentence;
import com.readcamp.entity.SentenceAnnotation;
import com.readcamp.entity.User;
import com.readcamp.mapper.ArticleMapper;
import com.readcamp.mapper.SentenceAnnotationMapper;
import com.readcamp.mapper.SentenceMapper;
import com.readcamp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端仪表盘（doc/00-design.md §2） */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final SentenceMapper sentenceMapper;
    private final SentenceAnnotationMapper annotationMapper;

    @GetMapping
    public Result<AdminStatsDto> stats() {
        AdminStatsDto dto = new AdminStatsDto();
        dto.setUsers(userMapper.selectCount(null));
        dto.setArticles(articleMapper.selectCount(null));
        dto.setPublished(articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1)));
        dto.setSentences(sentenceMapper.selectCount(null));
        dto.setGenDone(annotationMapper.selectCount(
                new LambdaQueryWrapper<SentenceAnnotation>()
                        .eq(SentenceAnnotation::getGenStatus, 2)));
        dto.setGenFailed(annotationMapper.selectCount(
                new LambdaQueryWrapper<SentenceAnnotation>()
                        .eq(SentenceAnnotation::getGenStatus, 3)));
        return Result.ok(dto);
    }
}
