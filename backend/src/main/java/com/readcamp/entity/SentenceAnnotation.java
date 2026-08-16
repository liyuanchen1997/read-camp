package com.readcamp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "sentence_annotation", autoResultMap = true)
public class SentenceAnnotation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sentenceId;

    /** 逐句中文翻译 */
    private String contentZh;

    /** 句子解释（英文讲解） */
    private String explanation;

    /** 句子成分 [{type,text,detail}] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> components;

    /** 单词标注 [{word,pos,meaning,role}] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> words;

    /** 0 未生成 / 1 生成中 / 2 已生成 / 3 生成失败 */
    private Integer genStatus;

    private String genError;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
