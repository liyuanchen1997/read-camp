package com.readcamp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.readcamp.common.ApiException;
import com.readcamp.dto.ProgressResponse;
import com.readcamp.dto.RecentReadingItem;
import com.readcamp.entity.Article;
import com.readcamp.entity.Sentence;
import com.readcamp.entity.UserProgress;
import com.readcamp.mapper.ArticleMapper;
import com.readcamp.mapper.SentenceMapper;
import com.readcamp.mapper.UserProgressMapper;
import com.readcamp.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private final UserProgressMapper progressMapper;
    private final ArticleMapper articleMapper;
    private final SentenceMapper sentenceMapper;

    @Override
    @Transactional
    public ProgressResponse report(Long userId, Long articleId, List<Integer> readSentenceIndexes) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw ApiException.notFound("文章不存在");
        }
        int totalCount = article.getSentenceCount();

        UserProgress progress = progressMapper.selectOne(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId)
                        .eq(UserProgress::getArticleId, articleId));

        if (progress == null) {
            progress = new UserProgress();
            progress.setUserId(userId);
            progress.setArticleId(articleId);
            progress.setReadSentences(new ArrayList<>());
            progress.setTotalCount(totalCount);
            progress.setIsCompleted(false);
            progress.setCreatedAt(LocalDateTime.now());
        }

        // 并集去重 + 越界过滤
        Set<Integer> merged = new HashSet<>(progress.getReadSentences() == null
                ? List.of() : progress.getReadSentences());
        for (Integer idx : readSentenceIndexes) {
            if (idx != null && idx >= 0 && idx < totalCount) {
                merged.add(idx);
            }
        }
        List<Integer> sorted = merged.stream().sorted().collect(Collectors.toList());

        progress.setReadSentences(sorted);
        progress.setReadCount(sorted.size());
        progress.setProgress(totalCount == 0 ? 0 : Math.min(100, sorted.size() * 100 / totalCount));
        boolean completed = totalCount > 0 && sorted.size() >= totalCount;
        if (completed && !Boolean.TRUE.equals(progress.getIsCompleted())) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }
        progress.setLastReadAt(LocalDateTime.now());

        if (progress.getId() == null) {
            progressMapper.insert(progress);
        } else {
            progressMapper.updateById(progress);
        }

        ProgressResponse response = new ProgressResponse();
        response.setProgress(progress.getProgress());
        response.setIsCompleted(completed);
        return response;
    }

    @Override
    public List<RecentReadingItem> recentReading(Long userId) {
        List<UserProgress> progresses = progressMapper.selectList(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId)
                        .orderByDesc(UserProgress::getLastReadAt)
                        .last("LIMIT 10"));

        if (progresses.isEmpty()) {
            return List.of();
        }
        Map<Long, Article> articleMap = articleMapper.selectBatchIds(
                        progresses.stream().map(UserProgress::getArticleId).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));

        List<RecentReadingItem> items = new ArrayList<>(progresses.size());
        for (UserProgress p : progresses) {
            Article article = articleMap.get(p.getArticleId());
            if (article == null) {
                continue;
            }
            RecentReadingItem item = new RecentReadingItem();
            item.setArticleId(article.getId());
            item.setTitle(article.getTitle());
            item.setCoverUrl(article.getCoverUrl());
            item.setDifficulty(article.getDifficulty());
            item.setProgress(p.getProgress());
            item.setIsCompleted(p.getIsCompleted());
            item.setLastReadAt(p.getLastReadAt());
            item.setCompletedAt(p.getCompletedAt());
            items.add(item);
        }
        return items;
    }

    @Override
    public long[] aggregateStats(Long userId) {
        List<UserProgress> all = progressMapper.selectList(
                new LambdaQueryWrapper<UserProgress>().eq(UserProgress::getUserId, userId));
        long completed = all.stream().filter(p -> Boolean.TRUE.equals(p.getIsCompleted())).count();
        long reading = all.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsCompleted()) && p.getReadCount() != null && p.getReadCount() > 0)
                .count();
        long totalProgress = all.isEmpty() ? 0
                : all.stream().mapToLong(p -> p.getProgress() == null ? 0 : p.getProgress()).sum() / all.size();
        return new long[]{completed, reading, totalProgress};
    }
}
