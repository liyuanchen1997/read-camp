package com.readcamp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.dto.FavoriteItem;

import java.util.List;

public interface FavoriteService {

    /** 分页列表（联表带出文章标题） */
    Page<FavoriteItem> list(Long userId, long page, long size);

    /** 收藏例句（已收藏幂等成功） */
    void add(Long userId, Long sentenceId);

    /** 取消收藏（不存在幂等成功） */
    void delete(Long userId, Long sentenceId);

    /** 用户收藏的句子 id 集合（reading 载荷用） */
    List<Long> favSentenceIds(Long userId, List<Long> articleSentenceIds);
}
