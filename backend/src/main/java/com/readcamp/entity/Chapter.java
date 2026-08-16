package com.readcamp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 文章章节（article.content_en 为各章拼接全文，本章原文独立存储供编辑回显） */
@Data
@TableName("chapter")
public class Chapter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    /** 章序，0 起 */
    private Integer seq;

    /** 章节标题 */
    private String title;

    /** 本章英文原文（已 trim） */
    private String contentEn;

    private LocalDateTime createdAt;
}
