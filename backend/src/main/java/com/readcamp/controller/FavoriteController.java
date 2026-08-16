package com.readcamp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.common.Result;
import com.readcamp.common.UserContext;
import com.readcamp.dto.FavoriteItem;
import com.readcamp.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/favorites/sentences")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public Result<Page<FavoriteItem>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(favoriteService.list(UserContext.userId(), page, size));
    }

    /** 收藏例句（已收藏幂等成功） */
    @PostMapping
    public Result<Void> add(@RequestBody Map<String, Long> body) {
        Long sentenceId = body.get("sentenceId");
        if (sentenceId == null) {
            return Result.fail(400, "sentenceId 不能为空");
        }
        favoriteService.add(UserContext.userId(), sentenceId);
        return Result.ok();
    }

    /** 取消收藏 */
    @DeleteMapping("/{sentenceId}")
    public Result<Void> delete(@PathVariable Long sentenceId) {
        favoriteService.delete(UserContext.userId(), sentenceId);
        return Result.ok();
    }
}
