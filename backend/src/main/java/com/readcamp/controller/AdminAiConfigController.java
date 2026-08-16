package com.readcamp.controller;

import com.readcamp.common.ApiException;
import com.readcamp.common.Result;
import com.readcamp.common.UserContext;
import com.readcamp.dto.AiConfigDto;
import com.readcamp.service.AiConfigService;
import com.readcamp.service.ai.DeepSeekClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** AI 模型配置（管理后台，doc/00-design.md §3 模型可配置） */
@RestController
@RequestMapping("/api/admin/ai-config")
@RequiredArgsConstructor
public class AdminAiConfigController {

    private final AiConfigService configService;
    private final DeepSeekClient deepSeekClient;

    @GetMapping
    public Result<AiConfigDto> get() {
        return Result.ok(AiConfigDto.from(configService.get()));
    }

    /** 更新配置（下次生成即生效） */
    @PutMapping
    public Result<AiConfigDto> update(@Valid @RequestBody AiConfigDto dto) {
        return Result.ok(AiConfigDto.from(configService.update(UserContext.userId(), dto)));
    }

    /** 用当前（或待保存的）配置发测试请求验证连通 */
    @PostMapping("/test")
    public Result<Map<String, Object>> test(@Valid @RequestBody AiConfigDto dto) {
        configService.update(UserContext.userId(), dto); // 先保存再测试
        try {
            String content = deepSeekClient.chatJson(
                    "你是一个连通性测试助手。",
                    "请回复一个 JSON 对象：{\"ok\": true}",
                    200);
            return Result.ok(Map.of("ok", true, "reply", content == null ? "" : content.substring(0, Math.min(content.length(), 200))));
        } catch (DeepSeekClient.AiCallException e) {
            throw new ApiException(400, 40020, "连接测试失败: " + e.getMessage());
        }
    }
}
