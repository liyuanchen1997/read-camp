package com.readcamp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.common.ApiException;
import com.readcamp.dto.FavoriteItem;
import com.readcamp.entity.Article;
import com.readcamp.entity.Sentence;
import com.readcamp.entity.SentenceAnnotation;
import com.readcamp.entity.UserFavoriteSentence;
import com.readcamp.mapper.ArticleMapper;
import com.readcamp.mapper.SentenceAnnotationMapper;
import com.readcamp.mapper.SentenceMapper;
import com.readcamp.mapper.UserFavoriteSentenceMapper;
import com.readcamp.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final UserFavoriteSentenceMapper favoriteMapper;
    private final SentenceMapper sentenceMapper;
    private final ArticleMapper articleMapper;
    private final SentenceAnnotationMapper annotationMapper;

    @Override
    public Page<FavoriteItem> list(Long userId, long page, long size) {
        Page<UserFavoriteSentence> result = favoriteMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UserFavoriteSentence>()
                        .eq(UserFavoriteSentence::getUserId, userId)
                        .orderByDesc(UserFavoriteSentence::getCreatedAt));

        List<FavoriteItem> items = new ArrayList<>(result.getRecords().size());
        if (!result.getRecords().isEmpty()) {
            List<Long> sentenceIds = result.getRecords().stream()
                    .map(UserFavoriteSentence::getSentenceId).collect(Collectors.toList());
            Map<Long, Sentence> sentenceMap = sentenceMapper.selectBatchIds(sentenceIds).stream()
                    .collect(Collectors.toMap(Sentence::getId, Function.identity()));
            Map<Long, Article> articleMap = Map.of();
            List<Long> articleIds = sentenceMap.values().stream()
                    .map(Sentence::getArticleId).distinct().collect(Collectors.toList());
            if (!articleIds.isEmpty()) {
                articleMap = articleMapper.selectBatchIds(articleIds).stream()
                        .collect(Collectors.toMap(Article::getId, Function.identity()));
            }
            // AI 标注（翻译/讲解），句子未生成标注时为 null
            Map<Long, SentenceAnnotation> annMap = annotationMapper.selectList(
                            new LambdaQueryWrapper<SentenceAnnotation>()
                                    .in(SentenceAnnotation::getSentenceId, sentenceIds))
                    .stream()
                    .collect(Collectors.toMap(SentenceAnnotation::getSentenceId, Function.identity()));
            Map<Long, Article> finalArticleMap = articleMap;
            for (UserFavoriteSentence fav : result.getRecords()) {
                Sentence sentence = sentenceMap.get(fav.getSentenceId());
                if (sentence == null) {
                    continue; // 句子已被删，跳过
                }
                FavoriteItem item = new FavoriteItem();
                item.setSentenceId(sentence.getId());
                item.setEn(sentence.getContentEn());
                SentenceAnnotation ann = annMap.get(sentence.getId());
                if (ann != null) {
                    item.setZh(ann.getContentZh());
                    item.setExplanation(ann.getExplanation());
                }
                item.setSeq(sentence.getSeq());
                item.setArticleId(sentence.getArticleId());
                Article article = finalArticleMap.get(sentence.getArticleId());
                item.setArticleTitle(article == null ? "" : article.getTitle());
                item.setCreatedAt(fav.getCreatedAt());
                items.add(item);
            }
        }

        Page<FavoriteItem> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(items);
        return dtoPage;
    }

    @Override
    public void add(Long userId, Long sentenceId) {
        if (sentenceMapper.selectById(sentenceId) == null) {
            throw ApiException.notFound("句子不存在");
        }
        Long exists = favoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavoriteSentence>()
                        .eq(UserFavoriteSentence::getUserId, userId)
                        .eq(UserFavoriteSentence::getSentenceId, sentenceId));
        if (exists > 0) {
            return; // 幂等
        }
        UserFavoriteSentence fav = new UserFavoriteSentence();
        fav.setUserId(userId);
        fav.setSentenceId(sentenceId);
        favoriteMapper.insert(fav);
    }

    @Override
    public void delete(Long userId, Long sentenceId) {
        favoriteMapper.delete(
                new LambdaQueryWrapper<UserFavoriteSentence>()
                        .eq(UserFavoriteSentence::getUserId, userId)
                        .eq(UserFavoriteSentence::getSentenceId, sentenceId));
    }

    @Override
    public List<Long> favSentenceIds(Long userId, List<Long> articleSentenceIds) {
        if (articleSentenceIds.isEmpty()) {
            return List.of();
        }
        return favoriteMapper.selectList(
                        new LambdaQueryWrapper<UserFavoriteSentence>()
                                .eq(UserFavoriteSentence::getUserId, userId)
                                .in(UserFavoriteSentence::getSentenceId, articleSentenceIds))
                .stream()
                .map(UserFavoriteSentence::getSentenceId)
                .collect(Collectors.toList());
    }
}
