package com.readcamp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.common.Result;
import com.readcamp.common.UserContext;
import com.readcamp.dto.ArticleDto;
import com.readcamp.dto.ArticleRequest;
import com.readcamp.dto.GenStatusResponse;
import com.readcamp.dto.GenerateRequest;
import com.readcamp.service.ArticleService;
import com.readcamp.service.ai.AiGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;
    private final AiGenerationService aiGenerationService;

    /** 创建文章（服务端切分落库） */
    @PostMapping
    public Result<ArticleDto> create(@Valid @RequestBody ArticleRequest request) {
        return Result.ok(articleService.create(request, UserContext.userId()));
    }

    /** 编辑文章（正文变更 → 重切分 + 清标注/进度） */
    @PutMapping("/{id}")
    public Result<ArticleDto> update(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return Result.ok(articleService.update(id, request));
    }

    /** 删除文章（级联句子/标注/进度/收藏） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return Result.ok();
    }

    /** 上架/下架 */
    @PostMapping("/{id}/status")
    public Result<ArticleDto> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return Result.ok(articleService.changeStatus(id, body.get("status")));
    }

    /** 管理列表（status 可选，keyword 匹配标题） */
    @GetMapping
    public Result<Page<ArticleDto>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(articleService.adminList(status, keyword, page, size));
    }

    // ---------- AI 生成 ----------

    /** 启动异步生成（进行中返回 409；页面关闭任务不中断，DB 状态可恢复） */
    @PostMapping("/{id}/generate")
    public Result<Map<String, Object>> generate(@PathVariable Long id,
                                                @Valid @RequestBody GenerateRequest request) {
        int total = aiGenerationService.start(id, request.getTarget(), request.getBatchSize());
        return Result.ok(Map.of("total", total));
    }

    /** 生成进度（四态计数 + 逐句状态，管理端 2s 轮询） */
    @GetMapping("/{id}/gen-status")
    public Result<GenStatusResponse> genStatus(@PathVariable Long id) {
        return Result.ok(aiGenerationService.genStatus(id));
    }

    /** 单句生成/失败重试（3 → 重新生成） */
    @PostMapping("/{id}/sentences/{sentenceId}/generate")
    public Result<Void> generateOne(@PathVariable Long id, @PathVariable Long sentenceId) {
        aiGenerationService.generateOne(id, sentenceId);
        return Result.ok();
    }

    /** 取消进行中任务（批间生效，最多浪费一个批次） */
    @PostMapping("/{id}/generate/cancel")
    public Result<Boolean> cancel(@PathVariable Long id) {
        return Result.ok(aiGenerationService.cancel(id));
    }
}
