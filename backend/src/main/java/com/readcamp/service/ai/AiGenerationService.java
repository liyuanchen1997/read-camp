package com.readcamp.service.ai;

import com.readcamp.dto.GenStatusResponse;

public interface AiGenerationService {

    /**
     * 启动异步生成任务（同文章进行中则抛 409）。
     *
     * @param target missing=仅未生成的句子 / all=全部重新生成
     * @return 本次任务将处理的句子数（0 表示无需生成）
     */
    int start(Long articleId, String target, Integer batchSize);

    /** 单句生成/失败重试（同步，仅该句） */
    void generateOne(Long articleId, Long sentenceId);

    /** 取消进行中任务（批间生效） */
    boolean cancel(Long articleId);

    /** 生成进度四态计数 + 逐句状态 */
    GenStatusResponse genStatus(Long articleId);
}
