package com.readcamp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "article", autoResultMap = true)
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    private String coverUrl;

    /** 英文全文原文（管理端回显/重切分用） */
    private String contentEn;

    /** 标签数组 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /** 1 入门 / 2 进阶 / 3 挑战 */
    private Integer difficulty;

    /** 0 下架(草稿) / 1 上架 */
    private Integer status;

    private Integer wordCount;

    private Integer sentenceCount;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
