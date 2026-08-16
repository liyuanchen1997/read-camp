# 后端说明文档（backend/doc/）

> 技术设计基线见根目录 [doc/00-design.md](../doc/00-design.md)，开发进度见 [doc/02-project-plan.md](../doc/02-project-plan.md)。

## 1. 架构总览

```
HTTP 请求 → 拦截器链（AuthInterceptor JWT → AdminInterceptor 角色校验）
         → controller（校验参数）→ service（业务逻辑）
         → mapper（MyBatis-Plus / XML）→ MySQL 8
异步任务：AiGenerationService（线程池）→ DeepSeek API → 逐句状态落库（DB 唯一事实源）
```

## 2. 模块与步骤对应

| 包/资源 | 内容 | 步骤 |
|---|---|---|
| common/ | Result、ApiException、GlobalExceptionHandler | 1（骨架） |
| controller/AuthController 等 | 认证接口 | 3 |
| service/ai/SentenceSplitter | 句子切分（上传时） | 4 |
| controller（Article/Progress/Vocab/Favorite） | 阅读与学习数据 | 4-5 |
| service/ai/DeepSeekClient + AiGenerationService + GenTaskRegistry | AI 批量生成 | 10 |
| resources/db/schema.sql + seed.sql | 建表 + 预置管理员/示例文章 | 2 |

## 3. 配置说明（application.yml，环境变量覆盖）

| 配置 | 环境变量 | 默认 |
|---|---|---|
| 数据源 | DB_URL / DB_USERNAME / DB_PASSWORD | localhost:3306/readcamp |
| JWT secret | JWT_SECRET | 空（步骤 3 必填） |
| DeepSeek | DEEPSEEK_API_KEY | 空（步骤 10 必填） |
| AI 模型 | - | deepseek-v4-flash（yml 可改） |
| 批量大小 | - | 5 |

本地覆盖写 `application-local.yml`（gitignore），启动加 `--spring.profiles.active=local`。

## 4. 关键设计回顾（详见 doc/00-design.md）

- 8 张表：user / article / sentence / sentence_annotation / user_progress / user_vocab / user_favorite_sentence
- 句子切分在服务端上传时完成并落库；阅读页零切分
- 整篇翻译 = 逐句 content_zh 按 seq 拼接，不单独生成
- gen_status 状态机：0未生成 → 1生成中 → 2已生成 / 3生成失败（可单句重试）
- 阅读载荷一次拉全；进度上报服务端并集去重

## 5. 变更记录

| 日期 | 变更 |
|---|---|
| 2026-08-16 | 骨架：pom（SB3.4+MP3.5.7+jjwt）、application.yml、ReadCampApplication、Result/ApiException/GlobalExceptionHandler、MybatisPlusConfig、HealthController |
