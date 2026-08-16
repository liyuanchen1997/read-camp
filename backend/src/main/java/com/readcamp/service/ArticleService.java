package com.readcamp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.dto.ArticleDto;
import com.readcamp.dto.ArticleRequest;
import com.readcamp.dto.ReadingPayload;

import java.util.List;

public interface ArticleService {

    /** 创建文章：切分落库，返回文章信息 */
    ArticleDto create(ArticleRequest request, Long createdBy);

    /** 编辑文章：正文变更则重切分（删旧句子/标注/进度） */
    ArticleDto update(Long id, ArticleRequest request);

    /** 删除文章（级联句子/标注/进度/收藏） */
    void delete(Long id);

    /** 上架/下架 */
    ArticleDto changeStatus(Long id, Integer status);

    /** 管理端列表（status 可选，keyword 匹配标题） */
    Page<ArticleDto> adminList(Integer status, String keyword, long page, long size);

    /** 书架列表（仅上架，可按难度/标签过滤） */
    Page<ArticleDto> shelfList(String keyword, Integer difficulty, String tag, long page, long size);

    /** 文章元信息（不存在或已下架时对普通用户抛 404） */
    ArticleDto getById(Long id, boolean forAdmin);

    /** 阅读载荷：元信息 + 全部句子（含标注）+ 我的进度/生词/收藏集合 */
    ReadingPayload readingPayload(Long id, Long userId);
}
