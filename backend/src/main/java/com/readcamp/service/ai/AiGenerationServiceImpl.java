package com.readcamp.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readcamp.common.ApiException;
import com.readcamp.dto.GenStatusResponse;
import com.readcamp.entity.Sentence;
import com.readcamp.entity.SentenceAnnotation;
import com.readcamp.mapper.ArticleMapper;
import com.readcamp.mapper.SentenceAnnotationMapper;
import com.readcamp.mapper.SentenceMapper;
import com.readcamp.service.AiConfigService;
import com.readcamp.service.ai.DeepSeekClient.AiCallException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * AI 生成服务（doc/00-design.md §3，最高复杂度模块）
 * 流程：分批（默认 3，优先 ai_config/请求参数）→ 每批一次 OpenAI 兼容调用（DeepSeekClient，json_object）→ 三层解析防护
 * → 逐项校验 UPSERT 落库（gen_status 实时更新）→ 批级失败重试 2 次（退避）
 * → 仍败该批标 3 + gen_error（可单句重试）。批间检查取消标记；DB 为唯一事实源。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationServiceImpl implements AiGenerationService {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_GENERATING = 1;
    public static final int STATUS_DONE = 2;
    public static final int STATUS_FAILED = 3;

    private static final String SYSTEM_PROMPT = """
            你是专业的英语精读讲解助手。你会收到一批英文句子（JSON 数组），请为每一句输出：
            中文翻译、中文句子讲解、句子成分、单词标注。所有解释性内容（句子讲解、成分作用、单词作用）一律使用中文。
            只输出一个 JSON 对象，不要输出任何解释性文字或 Markdown 代码块。
            """;

    private static final String OUTPUT_CONSTRAINTS = """
            输出结构（results 数量必须与输入一致，seq 一一对应）：
            {
              "results": [
                {
                  "seq": 0,
                  "content_zh": "中文翻译",
                  "explanation": "中文讲解：句子结构/时态/含义/关键语法点",
                  "components": [{"type": "主语", "text": "The cat", "detail": "作用说明"}],
                  "words": [{"word": "cat", "pos": "n.", "meaning": "猫", "role": "主语核心名词", "phonetic": "/kæt/"}]
                }
              ]
            }
            约束：
            1) words 覆盖句中全部实词（名词/动词/形容词/副词/代词/介词/连词），每个词带词性、中文意思、在句中的作用；
            2) 每个词尽量输出英语 IPA 音标（用 / / 包裹，如 /ˈbɪli/），个别词不确定可省略该字段，缺失不视为错误；
            3) components 用中文语法术语（主语/谓语/宾语/定语/状语/表语/宾补/从句等）完整覆盖主要成分；
            4) 所有中文内容保持简明；5) 严格 JSON，不要注释。
            """;

    private final SentenceMapper sentenceMapper;
    private final SentenceAnnotationMapper annotationMapper;
    private final ArticleMapper articleMapper;
    private final DeepSeekClient deepSeekClient;
    private final GenTaskRegistry registry;
    private final ObjectMapper objectMapper;
    private final AiConfigService configService;

    @Qualifier("aiGenExecutor")
    private final Executor executor;

    /** 批内字符上限（固定 3000，超长句自动降批） */
    private static final int MAX_CHARS_PER_BATCH = 3000;

    // ---------- 任务级 ----------

    @Override
    public int start(Long articleId, String target, Integer batchSize) {
        requireArticle(articleId);
        if (registry.isRunning(articleId)) {
            throw ApiException.conflict("该文章已有生成任务进行中");
        }
        // 重置孤儿"生成中"状态（上次任务中断/重启残留）
        resetOrphanGenerating(articleId);

        List<Sentence> sentences = targetSentences(articleId, "all".equals(target));
        if (sentences.isEmpty()) {
            return 0;
        }
        GenTaskRegistry.GenTask task = registry.register(articleId);
        if (task == null) {
            throw ApiException.conflict("该文章已有生成任务进行中");
        }
        // 批量大小优先取请求参数，其次取 ai_config 配置
        int batch = batchSize != null ? batchSize : configService.get().getBatchSize();
        executor.execute(() -> runTask(task, sentences, batch));
        return sentences.size();
    }

    /** 后台任务主体：分批执行，批间检查取消标记 */
    private void runTask(GenTaskRegistry.GenTask task, List<Sentence> sentences, int batchSize) {
        Long articleId = task.getArticleId();
        log.info("[ai-gen] 文章 {} 生成任务启动，共 {} 句", articleId, sentences.size());
        try {
            List<List<Sentence>> batches = partition(sentences, batchSize);
            for (List<Sentence> batch : batches) {
                if (task.isCancelled()) {
                    log.info("[ai-gen] 文章 {} 任务已取消", articleId);
                    break;
                }
                // 批内逐句状态复查：跳过已完成/生成中的句子（失败句包含在重试范围内）
                List<Sentence> todo = batch.stream()
                        .filter(s -> {
                            int st = currentStatus(s.getId());
                            return st != STATUS_DONE && st != STATUS_GENERATING;
                        })
                        .toList();
                if (todo.isEmpty()) {
                    continue;
                }
                generateBatchWithRetry(articleId, todo, 0);
            }
            log.info("[ai-gen] 文章 {} 生成任务结束", articleId);
        } catch (Exception e) {
            log.error("[ai-gen] 文章 {} 任务异常", articleId, e);
            markBatchFailed(sentences, "任务异常: " + e.getMessage());
        } finally {
            registry.remove(articleId);
        }
    }

    /** 批级执行 + 重试（指数退避 1s/3s，最多 2 次重试） */
    private void generateBatchWithRetry(Long articleId, List<Sentence> batch, int attempt) {
        try {
            Map<Integer, JsonNode> results = callAndParse(batch);
            upsertAnnotations(batch, results);
            return;
        } catch (AiCallException e) {
            if (attempt < 2) {
                sleepQuietly(1000L * (attempt + 1) * (attempt + 1) / 2);
                log.warn("[ai-gen] 文章 {} 批次重试 {}/2: {}", articleId, attempt + 1, e.getMessage());
                generateBatchWithRetry(articleId, batch, attempt + 1);
            } else {
                markBatchFailed(batch, "调用失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("[ai-gen] 文章 {} 批次解析异常", articleId, e);
            markBatchFailed(batch, "解析失败: " + e.getMessage());
        }
    }

    // ---------- 单句 ----------

    @Override
    public void generateOne(Long articleId, Long sentenceId) {
        requireArticle(articleId);
        Sentence sentence = sentenceMapper.selectById(sentenceId);
        if (sentence == null || !articleId.equals(sentence.getArticleId())) {
            throw ApiException.notFound("句子不存在");
        }
        generateBatchWithRetry(articleId, List.of(sentence), 0);
    }

    @Override
    public boolean cancel(Long articleId) {
        return registry.cancel(articleId);
    }

    // ---------- 进度 ----------

    @Override
    public GenStatusResponse genStatus(Long articleId) {
        List<Sentence> sentences = sentenceMapper.selectList(
                new LambdaQueryWrapper<Sentence>()
                        .eq(Sentence::getArticleId, articleId)
                        .orderByAsc(Sentence::getSeq));
        Map<Long, SentenceAnnotation> annMap = new HashMap<>();
        if (!sentences.isEmpty()) {
            annotationMapper.selectList(
                            new LambdaQueryWrapper<SentenceAnnotation>()
                                    .in(SentenceAnnotation::getSentenceId,
                                            sentences.stream().map(Sentence::getId).toList()))
                    .forEach(a -> annMap.put(a.getSentenceId(), a));
        }

        GenStatusResponse resp = new GenStatusResponse();
        resp.setTotal(sentences.size());
        resp.setRunning(registry.isRunning(articleId));
        List<GenStatusResponse.SentenceStatus> per = new ArrayList<>(sentences.size());
        for (Sentence s : sentences) {
            SentenceAnnotation ann = annMap.get(s.getId());
            int status = ann == null ? STATUS_PENDING : ann.getGenStatus();
            GenStatusResponse.SentenceStatus item = new GenStatusResponse.SentenceStatus();
            item.setSentenceId(s.getId());
            item.setSeq(s.getSeq());
            item.setGenStatus(status);
            if (ann != null) {
                item.setGenError(ann.getGenError());
            }
            per.add(item);
            switch (status) {
                case STATUS_PENDING -> resp.setPending(resp.getPending() + 1);
                case STATUS_GENERATING -> resp.setGenerating(resp.getGenerating() + 1);
                case STATUS_DONE -> resp.setDone(resp.getDone() + 1);
                case STATUS_FAILED -> resp.setFailed(resp.getFailed() + 1);
                default -> {
                }
            }
        }
        resp.setPerSentence(per);
        return resp;
    }

    // ---------- 内部方法 ----------

    private void requireArticle(Long articleId) {
        if (articleMapper.selectById(articleId) == null) {
            throw ApiException.notFound("文章不存在");
        }
    }

    /** 目标句子：missing=仅未生成；all=全部（重新生成则重置标注） */
    private List<Sentence> targetSentences(Long articleId, boolean all) {
        List<Sentence> sentences = sentenceMapper.selectList(
                new LambdaQueryWrapper<Sentence>()
                        .eq(Sentence::getArticleId, articleId)
                        .orderByAsc(Sentence::getSeq));
        if (!all) {
            List<Long> doneIds = annotationMapper.selectList(
                            new LambdaQueryWrapper<SentenceAnnotation>()
                                    .eq(SentenceAnnotation::getGenStatus, STATUS_DONE)
                                    .in(SentenceAnnotation::getSentenceId,
                                            sentences.stream().map(Sentence::getId).toList()))
                    .stream().map(SentenceAnnotation::getSentenceId).toList();
            sentences.removeIf(s -> doneIds.contains(s.getId()));
        } else {
            // 全部重生成：清空已有标注
            List<Long> ids = sentences.stream().map(Sentence::getId).toList();
            if (!ids.isEmpty()) {
                annotationMapper.deleteBySentenceIds(ids);
            }
        }
        return sentences;
    }

    private int currentStatus(Long sentenceId) {
        SentenceAnnotation ann = annotationMapper.selectOne(
                new LambdaQueryWrapper<SentenceAnnotation>()
                        .eq(SentenceAnnotation::getSentenceId, sentenceId));
        return ann == null ? STATUS_PENDING : ann.getGenStatus();
    }

    /** 重置该文章残留的"生成中"状态（上次任务中断/重启） */
    private void resetOrphanGenerating(Long articleId) {
        List<Sentence> sentences = sentenceMapper.selectList(
                new LambdaQueryWrapper<Sentence>().eq(Sentence::getArticleId, articleId));
        if (sentences.isEmpty()) {
            return;
        }
        List<Long> ids = sentences.stream().map(Sentence::getId).toList();
        List<SentenceAnnotation> generating = annotationMapper.selectList(
                new LambdaQueryWrapper<SentenceAnnotation>()
                        .eq(SentenceAnnotation::getGenStatus, STATUS_GENERATING)
                        .in(SentenceAnnotation::getSentenceId, ids));
        for (SentenceAnnotation ann : generating) {
            ann.setGenStatus(STATUS_PENDING);
            ann.setGenError(null);
            annotationMapper.updateById(ann);
        }
    }

    private List<List<Sentence>> partition(List<Sentence> sentences, int batchSize) {
        List<List<Sentence>> batches = new ArrayList<>();
        List<Sentence> current = new ArrayList<>();
        int chars = 0;
        for (Sentence s : sentences) {
            int len = s.getContentEn().length();
            if (!current.isEmpty() && (current.size() >= batchSize || chars + len > MAX_CHARS_PER_BATCH)) {
                batches.add(current);
                current = new ArrayList<>();
                chars = 0;
            }
            current.add(s);
            chars += len;
        }
        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }

    /** 一次调用：构建 Prompt → 请求 → 三层解析防护 → 结构校验 */
    private Map<Integer, JsonNode> callAndParse(List<Sentence> batch) throws AiCallException {
        markGenerating(batch);
        String userPrompt = buildUserPrompt(batch);
        // reasoning 模型思考会消耗 token：预算放宽（3000 + 1500/句，最低 8000），
        // 防止思考过长导致 content 被截断为空
        int maxTokens = Math.max(8000, 3000 + 1500 * batch.size());
        String raw = deepSeekClient.chatJson(SYSTEM_PROMPT, userPrompt, maxTokens);
        JsonNode root = parseJsonLoose(raw);
        return validateResults(root, batch);
    }

    private String buildUserPrompt(List<Sentence> batch) {
        StringBuilder input = new StringBuilder("[");
        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) {
                input.append(",");
            }
            Sentence s = batch.get(i);
            input.append("{\"seq\":").append(s.getSeq())
                    .append(",\"text\":").append(escapeJson(s.getContentEn())).append("}");
        }
        input.append("]");
        return "输入: " + input + "\n\n" + OUTPUT_CONSTRAINTS;
    }

    private String escapeJson(String text) {
        return objectMapper.createObjectNode().put("v", text).path("v").toString();
    }

    /** 三层解析：直接解析 → 剥 ```json 围栏 → 宽松提取（首 { 到末 }） */
    private JsonNode parseJsonLoose(String raw) throws AiCallException {
        List<String> candidates = new ArrayList<>();
        candidates.add(raw);
        candidates.add(raw.replaceAll("```(json)?", "").trim());
        int first = raw.indexOf('{');
        int last = raw.lastIndexOf('}');
        if (first >= 0 && last > first) {
            candidates.add(raw.substring(first, last + 1));
        }
        for (String candidate : candidates) {
            try {
                return objectMapper.readTree(candidate);
            } catch (Exception ignored) {
                // 尝试下一个候选
            }
        }
        throw new AiCallException("AI 返回内容无法解析为 JSON", null);
    }

    /** 结构校验：results 数组、数量一致、seq 对应、必填字段齐全 */
    private Map<Integer, JsonNode> validateResults(JsonNode root, List<Sentence> batch) throws AiCallException {
        JsonNode results = root.path("results");
        if (!results.isArray()) {
            throw new AiCallException("AI 返回缺少 results 数组", null);
        }
        Map<Integer, JsonNode> bySeq = new HashMap<>();
        List<Integer> missing = new ArrayList<>();
        for (JsonNode node : results) {
            int seq = node.path("seq").asInt(-1);
            boolean valid = seq >= 0
                    && node.hasNonNull("content_zh")
                    && node.hasNonNull("explanation")
                    && node.path("components").isArray()
                    && node.path("words").isArray();
            if (!valid) {
                missing.add(seq);
                continue;
            }
            bySeq.put(seq, node);
        }
        List<Sentence> todo = batch.stream()
                .filter(s -> !bySeq.containsKey(s.getSeq()))
                .toList();
        if (!todo.isEmpty()) {
            // 坏项单独补生成（小请求）
            for (Sentence s : todo) {
                try {
                    Map<Integer, JsonNode> single = callAndParseSingle(s);
                    bySeq.putAll(single);
                } catch (AiCallException e) {
                    markFailed(s, "补生成失败: " + e.getMessage());
                }
            }
        }
        return bySeq;
    }

    private Map<Integer, JsonNode> callAndParseSingle(Sentence s) throws AiCallException {
        String raw = deepSeekClient.chatJson(SYSTEM_PROMPT, buildUserPrompt(List.of(s)), 2400);
        JsonNode root = parseJsonLoose(raw);
        JsonNode results = root.path("results");
        for (JsonNode node : results) {
            if (node.path("seq").asInt(-1) == s.getSeq() && node.hasNonNull("content_zh")) {
                return Map.of(s.getSeq(), node);
            }
        }
        throw new AiCallException("补生成结果未包含该句", null);
    }

    /** 落库（UPSERT by sentence_id） */
    private void upsertAnnotations(List<Sentence> batch, Map<Integer, JsonNode> results) {
        for (Sentence s : batch) {
            JsonNode node = results.get(s.getSeq());
            if (node == null) {
                continue;
            }
            SentenceAnnotation ann = annotationMapper.selectOne(
                    new LambdaQueryWrapper<SentenceAnnotation>()
                            .eq(SentenceAnnotation::getSentenceId, s.getId()));
            if (ann == null) {
                ann = new SentenceAnnotation();
                ann.setSentenceId(s.getId());
                ann.setGenStatus(STATUS_DONE);
            }
            ann.setContentZh(node.path("content_zh").asText());
            ann.setExplanation(node.path("explanation").asText());
            ann.setComponents(node.path("components").isArray()
                    ? objectMapper.convertValue(node.path("components"), List.class) : List.of());
            ann.setWords(node.path("words").isArray()
                    ? objectMapper.convertValue(node.path("words"), List.class) : List.of());
            ann.setGenStatus(STATUS_DONE);
            ann.setGenError(null);
            if (ann.getId() == null) {
                annotationMapper.insert(ann);
            } else {
                annotationMapper.updateById(ann);
            }
        }
    }

    private void markGenerating(List<Sentence> batch) {
        for (Sentence s : batch) {
            SentenceAnnotation ann = annotationMapper.selectOne(
                    new LambdaQueryWrapper<SentenceAnnotation>()
                            .eq(SentenceAnnotation::getSentenceId, s.getId()));
            if (ann == null) {
                ann = new SentenceAnnotation();
                ann.setSentenceId(s.getId());
                ann.setGenStatus(STATUS_GENERATING);
                annotationMapper.insert(ann);
            } else if (ann.getGenStatus() != STATUS_GENERATING) {
                ann.setGenStatus(STATUS_GENERATING);
                annotationMapper.updateById(ann);
            }
        }
    }

    private void markFailed(Sentence s, String error) {
        SentenceAnnotation ann = annotationMapper.selectOne(
                new LambdaQueryWrapper<SentenceAnnotation>()
                        .eq(SentenceAnnotation::getSentenceId, s.getId()));
        if (ann == null) {
            ann = new SentenceAnnotation();
            ann.setSentenceId(s.getId());
            ann.setGenStatus(STATUS_FAILED);
        }
        ann.setGenStatus(STATUS_FAILED);
        ann.setGenError(error != null && error.length() > 500 ? error.substring(0, 500) : error);
        if (ann.getId() == null) {
            annotationMapper.insert(ann);
        } else {
            annotationMapper.updateById(ann);
        }
    }

    private void markBatchFailed(List<Sentence> batch, String error) {
        for (Sentence s : batch) {
            markFailed(s, error);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
