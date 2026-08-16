package com.readcamp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_vocab")
public class UserVocab {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 单词（小写规范化） */
    private String word;

    private Long sourceArticleId;

    /** 出处原句（复习展示用） */
    private String contextSentence;

    private LocalDateTime createdAt;
}
