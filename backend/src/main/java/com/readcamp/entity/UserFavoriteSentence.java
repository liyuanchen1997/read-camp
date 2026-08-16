package com.readcamp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_favorite_sentence")
public class UserFavoriteSentence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long sentenceId;

    private String note;

    private LocalDateTime createdAt;
}
