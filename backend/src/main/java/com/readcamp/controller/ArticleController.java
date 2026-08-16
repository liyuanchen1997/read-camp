package com.readcamp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.common.Result;
import com.readcamp.common.UserContext;
import com.readcamp.dto.ArticleDto;
import com.readcamp.dto.ReadingPayload;
import com.readcamp.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /** 书架分页（仅上架） */
    @GetMapping
    public Result<Page<ArticleDto>> shelf(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long size) {
        return Result.ok(articleService.shelfList(keyword, difficulty, tag, page, size));
    }

    /** 文章元信息 */
    @GetMapping("/{id}")
    public Result<ArticleDto> detail(@PathVariable Long id) {
        return Result.ok(articleService.getById(id, false));
    }

    /** 阅读载荷：一次拉全（元信息 + 句子 + 标注 + 我的学习数据） */
    @GetMapping("/{id}/reading")
    public Result<ReadingPayload> reading(@PathVariable Long id) {
        return Result.ok(articleService.readingPayload(id, UserContext.userId()));
    }
}
