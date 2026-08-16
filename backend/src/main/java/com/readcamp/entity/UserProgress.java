package com.readcamp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "user_progress", autoResultMap = true)
public class UserProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long articleId;

    /** 已读句索引数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> readSentences;

    private Integer readCount;

    /** 句子总数快照 */
    private Integer totalCount;

    /** 0-100 */
    private Integer progress;

    private Boolean isCompleted;

    private LocalDateTime completedAt;

    private LocalDateTime lastReadAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
