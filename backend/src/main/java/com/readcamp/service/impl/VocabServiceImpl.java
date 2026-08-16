package com.readcamp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.dto.VocabAddRequest;
import com.readcamp.dto.VocabItem;
import com.readcamp.entity.UserVocab;
import com.readcamp.mapper.UserVocabMapper;
import com.readcamp.service.VocabService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabServiceImpl implements VocabService {

    private final UserVocabMapper vocabMapper;

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
        dtoPage.setRecords(result.getRecords().stream().map(VocabItem::from).collect(Collectors.toList()));
        return dtoPage;
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
