package com.readcamp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 加入生词本请求 */
@Data
public class VocabAddRequest {

    @NotBlank(message = "单词不能为空")
    @Size(max = 100, message = "单词最长 100 字符")
    private String word;

    private Long sourceArticleId;

    @Size(max = 1000, message = "出处句过长")
    private String contextSentence;
}
