package com.readcamp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 章节请求（文章创建/编辑时随 chapters 传入） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterRequest {

    @NotBlank(message = "章节标题不能为空")
    @Size(max = 200, message = "章节标题最长 200 字符")
    private String title;

    @NotBlank(message = "章节正文不能为空")
    private String content;
}
