# 后端说明文档（backend/doc/）

> 技术设计基线见根目录 [doc/00-design.md](../doc/00-design.md)，开发进度见 [doc/02-project-plan.md](../doc/02-project-plan.md)。

## 1. 架构总览

```
HTTP 请求 → 拦截器链（AuthInterceptor JWT → AdminInterceptor 角色校验）
         → controller（校验参数）→ service（业务逻辑）
         → mapper（MyBatis-Plus / XML）→ MySQL 8
异步任务：AiGenerationService（线程池）→ DeepSeekClient → OpenAI 兼容 AI 接口（ai_config 表配置，默认 DeepSeek）→ 逐句状态落库（DB 唯一事实源）
```

## 2. 模块与步骤对应

| 包/资源 | 内容 | 步骤 |
|---|---|---|
| common/ | Result、ApiException、GlobalExceptionHandler | 1（骨架） |
| controller/AuthController 等 | 认证接口 | 3 |
| service/ai/SentenceSplitter | 句子切分（上传时） | 4 |
| controller（Article/Progress/Vocab/Favorite） | 阅读与学习数据 | 4-5 |
| service/ai/DeepSeekClient + AiGenerationService + GenTaskRegistry | AI 批量生成（ai_config 动态指向 OpenAI 兼容服务） | 10 |
| entity/Chapter + mapper/ChapterMapper + ai_config（AiConfigService/AdminAiConfigController） | 文章章节 + AI 模型配置（管理后台可编辑） | 13-14 |
| resources/db/schema.sql + seed.sql | 建表（9 张）+ 预置管理员/示例文章 | 2 |

## 3. 配置说明（application.yml，环境变量覆盖）

| 配置 | 环境变量 | 默认 |
|---|---|---|
| 数据源 | DB_URL / DB_USERNAME / DB_PASSWORD | localhost:3306/readcamp |
| JWT secret | JWT_SECRET | 空（首次启动必填） |
| AI API Key | DEEPSEEK_API_KEY | 空（**仅初始默认**，可改为在管理后台「AI 配置」页填写） |
| AI 模型 | - | deepseek-v4-flash（**仅初始默认值**，运行时可于管理后台「AI 配置」页修改） |
| 批量大小 | - | 3（ai_config 可改） |

> **运行时配置以 `ai_config` 表为准**（管理后台「AI 配置」页编辑 baseUrl/apiKey/model/batchSize/temperature/timeout，每次调用实时读取，切换后下次生成即生效）；application.yml 仅作初始默认值（首次访问落库）。

本地覆盖写 `application-local.yml`（gitignore），启动加 `--spring.profiles.active=local`。

## 4. 关键设计回顾（详见 doc/00-design.md）

- 9 张表：user / article / **chapter** / sentence / sentence_annotation / user_progress / user_vocab / user_favorite_sentence / ai_config
- 句子切分在服务端上传时完成并落库；阅读页零切分；句子含 para（章内段落号）与 chapter_id（章节 id，NULL=旧数据单章）
- 整篇翻译 = 逐句 content_zh 按 seq 拼接，不单独生成
- gen_status 状态机：0未生成 → 1生成中 → 2已生成 / 3生成失败（可单句重试）
- 阅读载荷一次拉全（含章节列表，无章节合成单章）；进度上报服务端并集去重

## 5. 变更记录

| 日期 | 变更 |
|---|---|
| 2026-08-16 | 骨架：pom（SB3.4+MP3.5.7+jjwt）、application.yml、ReadCampApplication、Result/ApiException/GlobalExceptionHandler、MybatisPlusConfig、HealthController |
| 2026-08-16 | 认证（步骤 3）、文章切分与阅读载荷（步骤 4）、学习数据（步骤 5）、AI 批量生成 + ai_config 可配置（步骤 10）、章节数据层 chapter 表 + sentence.chapter_id（步骤 13） |
| 2026-08-16 | 章节后端（步骤 14，迭代①）：章节归一化三分支（正文变化重切分 / chapterize 补章 / 仅改标题）、ArticleRequest.chapters、reading 载荷 chapters 恒非空（旧文章合成单章 id=null）、管理端 detail 回显 chapters |
| 2026-08-16 | 音标（步骤 17，迭代②）：生成 prompt words 增加 phonetic（IPA，校验宽松缺失不拒）、VocabItem.phonetic 与 FavoriteItem.words 带出（自 annotation.words JSON，无独立列） |
