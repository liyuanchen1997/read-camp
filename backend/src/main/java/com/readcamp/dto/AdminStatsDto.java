package com.readcamp.dto;

import lombok.Data;

/** 管理端仪表盘统计 */
@Data
public class AdminStatsDto {

    private long users;
    private long articles;
    private long published;
    private long sentences;
    /** 已生成标注句子数 */
    private long genDone;
    /** 生成失败句子数 */
    private long genFailed;
}
