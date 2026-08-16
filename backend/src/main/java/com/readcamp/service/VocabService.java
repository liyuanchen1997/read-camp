package com.readcamp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.dto.VocabAddRequest;
import com.readcamp.dto.VocabItem;

public interface VocabService {

    /** 分页列表（keyword 匹配单词，可选） */
    Page<VocabItem> list(Long userId, String keyword, long page, long size);

    /** 加入生词本（小写规范化，已存在幂等返回） */
    void add(Long userId, VocabAddRequest request);

    /** 删除（不存在则幂等成功） */
    void delete(Long userId, String word);
}
