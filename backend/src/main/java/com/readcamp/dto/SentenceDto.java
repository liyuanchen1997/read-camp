package com.readcamp.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 句子 + 精读标注响应 */
@Data
public class SentenceDto {

    private Long id;
    /** 章节 id（NULL=无章节旧数据，前端归入单章） */
    private Long chapterId;
    private Integer seq;
    /** 章内段落号（前端段落流式排版） */
    private Integer para;
    private String en;
    /** 中文翻译（未生成时为 null） */
    private String zh;
    /** 句子解释（未生成时为 null） */
    private String explanation;
    /** 句子成分（未生成时为 null） */
    private List<Map<String, String>> components;
    /** 单词标注（未生成时为 null） */
    private List<Map<String, String>> words;
    /** 0 未生成 / 1 生成中 / 2 已生成 / 3 生成失败 */
    private Integer genStatus;
}
