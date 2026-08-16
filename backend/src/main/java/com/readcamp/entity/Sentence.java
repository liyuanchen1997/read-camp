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

    private String contentEn;

    private LocalDateTime createdAt;
}
