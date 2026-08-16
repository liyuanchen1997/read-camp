package com.readcamp.dto;

import lombok.Data;

import java.util.List;

/** 阅读页我的进度视图（reading 载荷内嵌） */
@Data
public class ProgressView {

    /** 已读句索引集合 */
    private List<Integer> readSentences;
    /** 0-100 */
    private Integer progress;
    private Boolean isCompleted;
}
