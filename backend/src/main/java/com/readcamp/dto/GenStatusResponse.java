package com.readcamp.dto;

import lombok.Data;

import java.util.List;

/** 生成进度（GET gen-status）：四态计数 + 逐句状态 */
@Data
public class GenStatusResponse {

    @Data
    public static class SentenceStatus {
        private Long sentenceId;
        private Integer seq;
        /** 0 未生成 / 1 生成中 / 2 已生成 / 3 生成失败 */
        private Integer genStatus;
        private String genError;
    }

    private int total;
    private int pending;
    private int generating;
    private int done;
    private int failed;
    /** 是否有进行中任务 */
    private boolean running;
    private List<SentenceStatus> perSentence;
}
