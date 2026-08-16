package com.readcamp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sentence")
public class Sentence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    /** 句序，0 起 */
    private Integer seq;

    /** 段落号，0 起（按原文空行分段） */
    private Integer para;

    private String contentEn;

    private LocalDateTime createdAt;
}
