package com.readcamp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readcamp.common.ApiException;
import com.readcamp.dto.ArticleDetailDto;
import com.readcamp.dto.ArticleDto;
import com.readcamp.dto.ArticleRequest;
import com.readcamp.dto.ChapterDto;
import com.readcamp.dto.ChapterRequest;
import com.readcamp.dto.ProgressView;
import com.readcamp.dto.ReadingPayload;
import com.readcamp.dto.SentenceDto;
import com.readcamp.entity.Article;
import com.readcamp.entity.Chapter;
import com.readcamp.entity.Sentence;
import com.readcamp.entity.SentenceAnnotation;
import com.readcamp.entity.UserProgress;
import com.readcamp.entity.UserVocab;
import com.readcamp.mapper.ArticleMapper;
import com.readcamp.mapper.ChapterMapper;
import com.readcamp.mapper.SentenceAnnotationMapper;
import com.readcamp.mapper.SentenceMapper;
import com.readcamp.mapper.UserFavoriteSentenceMapper;
import com.readcamp.mapper.UserProgressMapper;
import com.readcamp.mapper.UserVocabMapper;
import com.readcamp.service.ArticleService;
import com.readcamp.service.FavoriteService;
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
    private final ChapterMapper chapterMapper;
    private final SentenceMapper sentenceMapper;
    private final SentenceAnnotationMapper annotationMapper;
    private final UserProgressMapper userProgressMapper;
    private final UserFavoriteSentenceMapper favoriteMapper;
    private final UserVocabMapper userVocabMapper;
    private final FavoriteService favoriteService;

    @Override
    @Transactional
    public ArticleDto create(ArticleRequest request, Long createdBy) {
        List<ChapterRequest> chs = normalizeChapters(request);
        Article article = new Article();
        applyRequest(article, request, joinChapters(chs));
        article.setStatus(0);
        article.setCreatedBy(createdBy);
        articleMapper.insert(article);
        splitAndStore(article, chs);
        return ArticleDto.from(article);
    }

    @Override
    @Transactional
    public ArticleDto update(Long id, ArticleRequest request) {
        Article article = requireArticle(id);
        String oldContent = article.getContentEn();
        List<ChapterRequest> chs = normalizeChapters(request);
        String joined = joinChapters(chs);
        boolean contentChanged = !oldContent.trim().equals(joined.trim());
        applyRequest(article, request, joined);
        articleMapper.updateById(article);

        if (contentChanged) {
            // 正文/章节结构变更 → 重切分：删旧句子+标注+进度+章节（前端已确认）
            reSplit(article, chs);
        } else if (chapterMapper.selectCount(
                new LambdaQueryWrapper<Chapter>().eq(Chapter::getArticleId, id)) == 0) {
            // 正文未变但文章尚无章节（旧数据首次保存）：补章节行 + 填存量句 chapter_id，不删句不弹窗
            chapterize(article, chs);
        } else {
            // 仅章标题变化：只更新 title，不重切分
            updateChapterTitlesOnly(article, chs);
        }
        return ArticleDto.from(article);
    }

    @Override
    @Transactional
    public ArticleDetailDto detail(Long id) {
        Article article = requireArticle(id);
        ArticleDetailDto dto = ArticleDetailDto.from(article);
        dto.setChapters(chapterMapper.selectList(
                        new LambdaQueryWrapper<Chapter>()
                                .eq(Chapter::getArticleId, id)
                                .orderByAsc(Chapter::getSeq))
                .stream()
                .map(c -> {
                    ChapterDto cd = new ChapterDto();
                    cd.setId(c.getId());
                    cd.setSeq(c.getSeq());
                    cd.setTitle(c.getTitle());
                    cd.setContent(c.getContentEn());
                    return cd;
                })
                .collect(Collectors.toList()));
        return dto;
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
        chapterMapper.deleteByArticleId(id);
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

        // 章节列表：恒非空（无章节旧文章合成单章，标题=文章标题）
        List<Chapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getArticleId, id)
                        .orderByAsc(Chapter::getSeq));
        List<ChapterDto> chapterDtos = chapters.isEmpty()
                ? List.of(legacySingleChapter(article))
                : chapters.stream().map(c -> toDtoWithoutContent(c)).collect(Collectors.toList());

        List<SentenceDto> sentenceDtos = new ArrayList<>(sentences.size());
        for (Sentence s : sentences) {
            SentenceDto dto = new SentenceDto();
            dto.setId(s.getId());
            dto.setChapterId(s.getChapterId());
            dto.setSeq(s.getSeq());
            dto.setPara(s.getPara() == null ? 0 : s.getPara());
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
        payload.setChapters(chapterDtos);
        payload.setSentences(sentenceDtos);
        payload.setProgress(loadProgress(id, userId));
        payload.setVocabWords(userVocabMapper.selectList(
                        new LambdaQueryWrapper<UserVocab>().eq(UserVocab::getUserId, userId))
                .stream()
                .map(UserVocab::getWord)
                .collect(Collectors.toList()));
        payload.setFavSentenceIds(favoriteService.favSentenceIds(userId,
                sentences.stream().map(Sentence::getId).collect(Collectors.toList())));
        return payload;
    }

    /** 我的进度视图（无记录返回空进度） */
    private ProgressView loadProgress(Long articleId, Long userId) {
        UserProgress p = userProgressMapper.selectOne(
                new LambdaQueryWrapper<UserProgress>()
                        .eq(UserProgress::getUserId, userId)
                        .eq(UserProgress::getArticleId, articleId));
        ProgressView view = new ProgressView();
        if (p == null) {
            view.setReadSentences(List.of());
            view.setProgress(0);
            view.setIsCompleted(false);
        } else {
            view.setReadSentences(p.getReadSentences() == null ? List.of() : p.getReadSentences());
            view.setProgress(p.getProgress());
            view.setIsCompleted(p.getIsCompleted());
        }
        return view;
    }

    // ---------- 内部方法 ----------

    private Article requireArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw ApiException.notFound("文章不存在");
        }
        return article;
    }

    private void applyRequest(Article article, ArticleRequest request, String joinedContent) {
        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary() == null ? "" : request.getSummary().trim());
        article.setContentEn(joinedContent);
        article.setTags(request.getTags() == null ? List.of() : request.getTags());
        article.setDifficulty(request.getDifficulty());
    }

    /**
     * 章节归一化：缺省/空列表 → 单章（title=文章标题、content=content 字段，兼容旧调用）；
     * 每章正文 trim。
     */
    private List<ChapterRequest> normalizeChapters(ArticleRequest request) {
        List<ChapterRequest> chs = request.getChapters();
        if (chs == null || chs.isEmpty()) {
            return List.of(new ChapterRequest(request.getTitle(), request.getContent()));
        }
        for (ChapterRequest c : chs) {
            c.setContent(c.getContent().trim());
        }
        return chs;
    }

    /** 章节拼接全文：各章 content 用空行连接（与前端 payload 拼接逐字节一致） */
    private String joinChapters(List<ChapterRequest> chs) {
        return chs.stream().map(ChapterRequest::getContent).collect(Collectors.joining("\n\n"));
    }

    /**
     * 逐章切分落库：每章 insert chapter 行 → SentenceSplitter.split（章内 para 0 起）
     * → seq 全局递增、chapter_id 落库；回填 word_count / sentence_count。
     */
    private void splitAndStore(Article article, List<ChapterRequest> chs) {
        article.setWordCount(countWords(joinChapters(chs)));
        int seq = 0;
        int total = 0;
        int chapterSeq = 0;
        for (ChapterRequest c : chs) {
            Chapter chapter = new Chapter();
            chapter.setArticleId(article.getId());
            chapter.setSeq(chapterSeq++);
            chapter.setTitle(c.getTitle().trim());
            chapter.setContentEn(c.getContent());
            chapterMapper.insert(chapter);
            for (SentenceSplitter.SentencePart part : SentenceSplitter.split(c.getContent())) {
                Sentence s = new Sentence();
                s.setArticleId(article.getId());
                s.setChapterId(chapter.getId());
                s.setSeq(seq++);
                s.setPara(part.para());
                s.setContentEn(part.text());
                sentenceMapper.insert(s);
                total++;
            }
        }
        article.setSentenceCount(total);
        articleMapper.updateById(article);
    }

    /** 正文变更重切分：删旧句子+标注+进度+章节行，再逐章切分 */
    private void reSplit(Article article, List<ChapterRequest> chs) {
        List<Sentence> old = sentenceMapper.selectList(
                new LambdaQueryWrapper<Sentence>().eq(Sentence::getArticleId, article.getId()));
        if (!old.isEmpty()) {
            annotationMapper.deleteBySentenceIds(old.stream().map(Sentence::getId).collect(Collectors.toList()));
        }
        sentenceMapper.deleteByArticleId(article.getId());
        chapterMapper.deleteByArticleId(article.getId());
        userProgressMapper.deleteByArticleId(article.getId());
        splitAndStore(article, chs);
    }

    /**
     * 旧数据首次保存（正文未变）：补 chapter 行 + 给存量句填 chapter_id 与章内 para。
     * 依赖 splitter 确定性——同内容产生同 part 序列，按 seq 顺序走位。
     */
    private void chapterize(Article article, List<ChapterRequest> chs) {
        List<Sentence> sentences = sentenceMapper.selectList(
                new LambdaQueryWrapper<Sentence>()
                        .eq(Sentence::getArticleId, article.getId())
                        .orderByAsc(Sentence::getSeq));
        int seq = 0;
        int chapterSeq = 0;
        for (ChapterRequest c : chs) {
            Chapter chapter = new Chapter();
            chapter.setArticleId(article.getId());
            chapter.setSeq(chapterSeq++);
            chapter.setTitle(c.getTitle().trim());
            chapter.setContentEn(c.getContent());
            chapterMapper.insert(chapter);
            for (SentenceSplitter.SentencePart part : SentenceSplitter.split(c.getContent())) {
                if (seq < sentences.size()) {
                    Sentence s = sentences.get(seq++);
                    s.setChapterId(chapter.getId());
                    s.setPara(part.para());
                    sentenceMapper.updateById(s);
                }
            }
        }
    }

    /** 仅章标题变化：按 seq 更新既有章节行的 title，不重切分 */
    private void updateChapterTitlesOnly(Article article, List<ChapterRequest> chs) {
        List<Chapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getArticleId, article.getId())
                        .orderByAsc(Chapter::getSeq));
        for (int i = 0; i < Math.min(chapters.size(), chs.size()); i++) {
            Chapter c = chapters.get(i);
            String newTitle = chs.get(i).getTitle().trim();
            if (!c.getTitle().equals(newTitle)) {
                c.setTitle(newTitle);
                chapterMapper.updateById(c);
            }
        }
    }

    /** 无章节旧文章的合成单章（id=null，前端归入 legacy 组） */
    private ChapterDto legacySingleChapter(ArticleDto article) {
        ChapterDto dto = new ChapterDto();
        dto.setId(null);
        dto.setSeq(0);
        dto.setTitle(article.getTitle());
        return dto;
    }

    /** 章节 → 阅读载荷 DTO（不含 content） */
    private ChapterDto toDtoWithoutContent(Chapter c) {
        ChapterDto dto = new ChapterDto();
        dto.setId(c.getId());
        dto.setSeq(c.getSeq());
        dto.setTitle(c.getTitle());
        return dto;
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
