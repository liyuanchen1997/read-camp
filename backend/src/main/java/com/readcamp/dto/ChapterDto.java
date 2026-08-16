package com.readcamp.dto;

import lombok.Data;

/** 章节响应（读写共用）：阅读载荷 content=null；管理端详情回显 content 填充 */
@Data
public class ChapterDto {

    /** 章节 id（阅读载荷中无章节旧文章合成单章时为 null） */
    private Long id;
    /** 章序，0 起 */
    private Integer seq;
    /** 章节标题 */
    private String title;
    /** 本章原文（仅管理端详情回显） */
    private String content;
}
