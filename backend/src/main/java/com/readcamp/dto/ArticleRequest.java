package com.readcamp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 创建/编辑文章请求 */
@Data
public class ArticleRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 字符")
    private String title;

    @Size(max = 500, message = "简介最长 500 字符")
    private String summary;

    @NotBlank(message = "正文不能为空")
    private String content;

    /** 章节列表（可选；缺省 = 单章：title=文章标题、content=content 字段，兼容旧调用） */
    @Size(max = 50, message = "章节最多 50 个")
    @Valid
    private List<ChapterRequest> chapters;

    @Size(max = 5, message = "标签最多 5 个")
    private List<@Size(max = 20, message = "单个标签最长 20 字符") String> tags;

    @NotNull(message = "难度不能为空")
    private Integer difficulty;
}
