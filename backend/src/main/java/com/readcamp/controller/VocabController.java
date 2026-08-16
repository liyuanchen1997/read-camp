package com.readcamp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.common.Result;
import com.readcamp.common.UserContext;
import com.readcamp.dto.VocabAddRequest;
import com.readcamp.dto.VocabItem;
import com.readcamp.service.VocabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vocab")
@RequiredArgsConstructor
public class VocabController {

    private final VocabService vocabService;

    @GetMapping
    public Result<Page<VocabItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(vocabService.list(UserContext.userId(), keyword, page, size));
    }

    /** 加入生词本（已存在幂等成功） */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody VocabAddRequest request) {
        vocabService.add(UserContext.userId(), request);
        return Result.ok();
    }

    /** 删除（URL 编码单词） */
    @DeleteMapping("/{word}")
    public Result<Void> delete(@PathVariable String word) {
        vocabService.delete(UserContext.userId(), word);
        return Result.ok();
    }
}
