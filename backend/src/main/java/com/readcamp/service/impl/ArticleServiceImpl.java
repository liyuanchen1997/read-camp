package com.readcamp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.common.ApiException;
import com.readcamp.dto.ArticleDto;
import com.readcamp.dto.ArticleRequest;
import com.readcamp.dto.ReadingPayload;
import com.readcamp.dto.SentenceDto;
import com.readcamp.entity.Article;
import com.readcamp.entity.Sentence;
import com.readcamp.entity.SentenceAnnotation;
import com.readcamp.mapper.ArticleMapper;
import com.readcamp.mapper.SentenceAnnotationMapper;
import com.readcamp.mapper.SentenceMapper;
import com.readcamp.mapper.UserFavoriteSentenceMapper;
import com.readcamp.mapper.UserProgressMapper;
import com.readcamp.service.ArticleService;
import com.readcamp.service.ai.SentenceSplitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final SentenceMapper sentenceMapper;
    private final SentenceAnnotationMapper annotationMapper;
    private final UserProgressMapper userProgressMapper;
    private final UserFavoriteSentenceMapper favoriteMapper;

    @Override
    @Transactional
    public ArticleDto create(ArticleRequest request, Long createdBy) {
        Article article = new Article();
        applyRequest(article, request);
        article.setStatus(0);
        article.setCreatedBy(createdBy);
        articleMapper.insert(article);
        splitAndStore(article, request.getContent());
        return ArticleDto.from(article);
    }

    @Override
    @Transactional
    public ArticleDto update(Long id, ArticleRequest request) {
        Article article = requireArticle(id);
        String oldContent = article.getContentEn();
        applyRequest(article, request);
        articleMapper.updateById(article);

        if (!oldContent.equals(request.getContent())) {
            // 正文变更 → 重切分：删旧句子+标注，清进度（前端已确认）
            reSplit(article, request.getContent());
        }
        return ArticleDto.from(article);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireArticle(id);
        List<Sentence> sentences = sentenceMapper.selectList(
                new LambdaQueryWrapper<Sentence>().eq(Sentence::getArticleId, id));
        if (!sentences.isEmpty()) {
            annotationMapper.deleteBySentenceIds(sentences.stream()
                    .map(Sentence::getId).collect(Collectors.toList()));
        }
        sentenceMapper.deleteByArticleId(id);
        userProgressMapper.deleteByArticleId(id);
        favoriteMapper.deleteByArticleId(id);
        articleMapper.deleteById(id);
    }

    @Override
    public ArticleDto changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ApiException(400, 40010, "status 仅支持 0（下架）/ 1（上架）");
        }
        Article article = requireArticle(id);
        article.setStatus(status);
        articleMapper.updateById(article);
        return ArticleDto.from(article);
    }

    @Override
    public Page<ArticleDto> adminList(Integer status, String keyword, long page, long size) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Article::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Article::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(Article::getCreatedAt);
        Page<Article> result = articleMapper.selectPage(new Page<>(page, size), wrapper);
        return toDtoPage(result);
    }

    @Override
    public Page<ArticleDto> shelfList(String keyword, Integer difficulty, String tag, long page, long size) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Article::getTitle, keyword.trim());
        }
        if (difficulty != null) {
            wrapper.eq(Article::getDifficulty, difficulty);
        }
        if (StringUtils.hasText(tag)) {
            wrapper.apply("JSON_CONTAINS(tags, {0})", "\"" + tag.trim() + "\"");
        }
        wrapper.orderByDesc(Article::getCreatedAt);
        Page<Article> result = articleMapper.selectPage(new Page<>(page, size), wrapper);
        return toDtoPage(result);
    }

    @Override
    public ArticleDto getById(Long id, boolean forAdmin) {
        Article article = requireArticle(id);
        if (!forAdmin && article.getStatus() != 1) {
            throw ApiException.notFound("文章不存在或已下架");
        }
        return ArticleDto.from(article);
    }

    @Override
    public ReadingPayload readingPayload(Long id, Long userId) {
        ArticleDto article = getById(id, false);

        List<Sentence> sentences = sentenceMapper.selectList(
                new LambdaQueryWrapper<Sentence>()
                        .eq(Sentence::getArticleId, id)
                        .orderByAsc(Sentence::getSeq));

        Map<Long, SentenceAnnotation> annotationMap = Map.of();
        if (!sentences.isEmpty()) {
            annotationMap = annotationMapper.selectList(
                            new LambdaQueryWrapper<SentenceAnnotation>()
                                    .in(SentenceAnnotation::getSentenceId,
                                            sentences.stream().map(Sentence::getId).collect(Collectors.toList())))
                    .stream()
                    .collect(Collectors.toMap(SentenceAnnotation::getSentenceId, Function.identity()));
        }

        List<SentenceDto> sentenceDtos = new ArrayList<>(sentences.size());
        for (Sentence s : sentences) {
            SentenceDto dto = new SentenceDto();
            dto.setId(s.getId());
            dto.setSeq(s.getSeq());
            dto.setEn(s.getContentEn());
            SentenceAnnotation ann = annotationMap.get(s.getId());
            if (ann != null) {
                dto.setZh(ann.getContentZh());
                dto.setExplanation(ann.getExplanation());
                dto.setComponents(ann.getComponents());
                dto.setWords(ann.getWords());
                dto.setGenStatus(ann.getGenStatus());
            } else {
                dto.setGenStatus(0);
            }
            sentenceDtos.add(dto);
        }

        ReadingPayload payload = new ReadingPayload();
        payload.setArticle(article);
        payload.setSentences(sentenceDtos);
        // 进度/生词/收藏集合由步骤 5 填充
        payload.setProgress(null);
        payload.setVocabWords(List.of());
        payload.setFavSentenceIds(List.of());
        return payload;
    }

    // ---------- 内部方法 ----------

    private Article requireArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw ApiException.notFound("文章不存在");
        }
        return article;
    }

    private void applyRequest(Article article, ArticleRequest request) {
        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary() == null ? "" : request.getSummary().trim());
        article.setContentEn(request.getContent());
        article.setTags(request.getTags() == null ? List.of() : request.getTags());
        article.setDifficulty(request.getDifficulty());
    }

    /** 切分全文并落库，回填 word_count / sentence_count */
    private void splitAndStore(Article article, String content) {
        List<String> sentences = SentenceSplitter.split(content);
        article.setWordCount(countWords(content));
        article.setSentenceCount(sentences.size());
        articleMapper.updateById(article);

        int seq = 0;
        for (String text : sentences) {
            Sentence s = new Sentence();
            s.setArticleId(article.getId());
            s.setSeq(seq++);
            s.setContentEn(text);
            sentenceMapper.insert(s);
        }
    }

    /** 正文变更重切分：删旧句子+标注+进度，再切分 */
    private void reSplit(Article article, String content) {
        List<Sentence> old = sentenceMapper.selectList(
                new LambdaQueryWrapper<Sentence>().eq(Sentence::getArticleId, article.getId()));
        if (!old.isEmpty()) {
            annotationMapper.deleteBySentenceIds(old.stream().map(Sentence::getId).collect(Collectors.toList()));
        }
        sentenceMapper.deleteByArticleId(article.getId());
        userProgressMapper.deleteByArticleId(article.getId());
        splitAndStore(article, content);
    }

    private int countWords(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return content.trim().split("\\s+").length;
    }

    private Page<ArticleDto> toDtoPage(Page<Article> page) {
        Page<ArticleDto> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(page.getRecords().stream().map(ArticleDto::from).collect(Collectors.toList()));
        return dtoPage;
    }
}
