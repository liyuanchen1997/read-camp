package com.readcamp.service;

import com.readcamp.dto.ProgressResponse;
import com.readcamp.dto.RecentReadingItem;

import java.util.List;

public interface ProgressService {

    /** 批量上报已读句索引（服务端并集去重），返回最新进度 */
    ProgressResponse report(Long userId, Long articleId, List<Integer> readSentenceIndexes);

    /** 近期阅读（按 last_read_at 倒序，默认 10 条） */
    List<RecentReadingItem> recentReading(Long userId);

    /** 用户学习统计（/users/me 聚合用） */
    long[] aggregateStats(Long userId);
}
