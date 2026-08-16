package com.readcamp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.dto.VocabAddRequest;
import com.readcamp.dto.VocabItem;
import com.readcamp.entity.Sentence;
import com.readcamp.entity.SentenceAnnotation;
import com.readcamp.entity.UserVocab;
import com.readcamp.mapper.SentenceAnnotationMapper;
import com.readcamp.mapper.SentenceMapper;
import com.readcamp.mapper.UserVocabMapper;
import com.readcamp.service.VocabService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabServiceImpl implements VocabService {

    private final UserVocabMapper vocabMapper;
    private final SentenceMapper sentenceMapper;
    private final SentenceAnnotationMapper annotationMapper;

    @Override
    public Page<VocabItem> list(Long userId, String keyword, long page, long size) {
        LambdaQueryWrapper<UserVocab> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserVocab::getUserId, userId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(UserVocab::getWord, keyword.trim().toLowerCase(Locale.ROOT));
        }
        wrapper.orderByDesc(UserVocab::getCreatedAt);
        Page<UserVocab> result = vocabMapper.selectPage(new Page<>(page, size), wrapper);
        Page<VocabItem> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(fillExplanations(result.getRecords()));
        return dtoPage;
    }

    /**
     * 解释填充：按来源文章聚合查句子 → 标注 → words JSON 匹配单词，带出词性/意思/作用。
     * 无来源（手工添加/文章已删/标注未生成）时解释字段为 null。
     */
    private List<VocabItem> fillExplanations(List<UserVocab> vocabs) {
        List<VocabItem> items = vocabs.stream().map(VocabItem::from).collect(Collectors.toList());
        Map<Long, List<VocabItem>> byArticle = items.stream()
                .filter(v -> v.getSourceArticleId() != null && StringUtils.hasText(v.getContextSentence()))
                .collect(Collectors.groupingBy(VocabItem::getSourceArticleId));
        for (Map.Entry<Long, List<VocabItem>> e : byArticle.entrySet()) {
            List<Sentence> sentences = sentenceMapper.selectList(
                    new LambdaQueryWrapper<Sentence>()
                            .eq(Sentence::getArticleId, e.getKey()));
            Map<String, Long> sentenceIdByText = sentences.stream()
                    .collect(Collectors.toMap(Sentence::getContentEn, Sentence::getId, (a, b) -> a));
            Map<Long, SentenceAnnotation> annMap = Map.of();
            if (!sentences.isEmpty()) {
                annMap = annotationMapper.selectList(
                                new LambdaQueryWrapper<SentenceAnnotation>()
                                        .in(SentenceAnnotation::getSentenceId,
                                                sentences.stream().map(Sentence::getId).collect(Collectors.toList())))
                        .stream()
                        .collect(Collectors.toMap(SentenceAnnotation::getSentenceId, a -> a));
            }
            Map<Long, SentenceAnnotation> finalAnnMap = annMap;
            for (VocabItem v : e.getValue()) {
                Long sid = sentenceIdByText.get(v.getContextSentence());
                if (sid == null) {
                    continue;
                }
                SentenceAnnotation ann = finalAnnMap.get(sid);
                if (ann == null || ann.getWords() == null) {
                    continue;
                }
                for (Map<String, String> w : ann.getWords()) {
                    String word = w.get("word");
                    if (word != null && word.toLowerCase(Locale.ROOT).equals(v.getWord())) {
                        v.setPos(w.get("pos"));
                        v.setMeaning(w.get("meaning"));
                        v.setRole(w.get("role"));
                        v.setPhonetic(w.get("phonetic"));
                        break;
                    }
                }
            }
        }
        return items;
    }

    @Override
    public void add(Long userId, VocabAddRequest request) {
        String word = request.getWord().trim().toLowerCase(Locale.ROOT);
        Long exists = vocabMapper.selectCount(
                new LambdaQueryWrapper<UserVocab>()
                        .eq(UserVocab::getUserId, userId)
                        .eq(UserVocab::getWord, word));
        if (exists > 0) {
            return; // 幂等：已存在直接成功
        }
        UserVocab vocab = new UserVocab();
        vocab.setUserId(userId);
        vocab.setWord(word);
        vocab.setSourceArticleId(request.getSourceArticleId());
        vocab.setContextSentence(request.getContextSentence());
        vocabMapper.insert(vocab);
    }

    @Override
    public void delete(Long userId, String word) {
        vocabMapper.delete(
                new LambdaQueryWrapper<UserVocab>()
                        .eq(UserVocab::getUserId, userId)
                        .eq(UserVocab::getWord, word.trim().toLowerCase(Locale.ROOT)));
    }
}
