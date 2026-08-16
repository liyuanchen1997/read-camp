# 开发变更记录

> 追加式记录，每步开发完成后追加一条。格式：`- [步骤 N] 日期：变更内容（验收结果）`

## 规则与约定

- 2026-08-16：用户要求——**每次 git 提交前必须检查并更新相关文档**（对照 diff 检查 README/CLAUDE/AGENTS/doc/**，滞后先更新再提交），已同步至根及前后端 CLAUDE.md / AGENTS.md
- 2026-08-16：配置 PreToolUse hook 强制文档同步——`.claude/settings.json` + `.claude/scripts/check-doc-sync.py`（git commit/push 前自动检查：非文档变更未更新 changelog、或代码变更无 doc/ 更新 → 拦截并提示调用 git-docs-sync）

## 步骤 1 — 项目骨架与文档

- 2026-08-16：初始化 git 仓库（main 分支）+ .gitignore
- 2026-08-16：创建根文档 AGENTS.md（代理开发规则）、CLAUDE.md（项目说明）、README.md（简介与快速启动）
- 2026-08-16：创建 doc/00-design.md（技术架构设计 v1.0）、doc/01-ui-design.md（UI 设计 v1.0，亮/暗双主题）、doc/02-project-plan.md（12 步项目计划表）、doc/changelog.md（本文件）
- 2026-08-16：按用户补充要求，前后端各自目录下将生成 CLAUDE.md、AGENTS.md、doc/ 说明文档（随各自骨架落地）
- 2026-08-16：搭建前端 Vite+Vue3+TS 骨架（路由/Pinia/Axios/双主题 tokens/Vite proxy→8080）
- 2026-08-16：创建 frontend/CLAUDE.md、frontend/AGENTS.md、frontend/doc/README.md（前端项目文档）
- 2026-08-16：搭建后端 Spring Boot 3 骨架（pom/application.yml/HealthController/统一 Result/ApiException/全局异常处理）
- 2026-08-16：创建 backend/CLAUDE.md、backend/AGENTS.md、backend/doc/README.md（后端项目文档）
- 2026-08-16：创建 readcamp 数据库；写 application-local.yml（gitignore 排除）
- 2026-08-16：**步骤 1 验收通过**：前端 `npm run build` 通过、dev server 5173 正常；后端编译通过、`GET /api/health` 返回 `{"code":0,"message":"ok","data":"ok"}`（直连 8080 与 Vite 代理均 200）。期间处理：8080 端口被 memo-lanbitou 项目占用，经用户确认后停止其进程（PID 15200）

## 步骤 2 — 数据库设计

- 2026-08-16：创建 db/schema.sql（7 张表：user/article/sentence/sentence_annotation/user_progress/user_vocab/user_favorite_sentence，CREATE TABLE IF NOT EXISTS 幂等）
- 2026-08-16：创建 db/seed.sql（预置管理员 admin / 默认密码 admin123 / 首登强制改密；示例文章 The Lion and the Mouse 13 句 126 词，固定 id=10001 幂等；ON DUPLICATE KEY UPDATE 自赋值实现"存在即跳过"）
- 2026-08-16：修正文档笔误"8 张表"→"7 张表"（doc/00-design.md、doc/02-project-plan.md、backend/doc/README.md）
- 2026-08-16：**步骤 2 验收通过**：schema+seed 各执行 2 次均 exit=0，行数不变（user=1/article=1/sentence=13）；SHOW CREATE TABLE 抽查确认 JSON 列（components/words/tags/read_sentences）、唯一键（uk_sentence/uk_user_article 等）、索引（idx_gen_status/idx_recent）齐全

## 步骤 3 — 认证后端

- 2026-08-16：注册/登录/改密接口（BCrypt 密码编码，spring-security-crypto 单依赖）；JwtUtil（jjwt 0.12，HS384，7 天过期，secret 走配置）；AuthInterceptor（JWT→ThreadLocal UserContext，校验用户状态）+ AdminInterceptor（role==1）+ WebConfig 注册（放行 /api/health、/api/auth/register、/api/auth/login）
- 2026-08-16：GET/PUT /api/users/me（个人资料，聚合统计留到步骤 5）
- 2026-08-16：DTO 拆分为独立文件（AuthDtos/UserDtos 多 public 类违反 Java 规范，经用户同意删除）
- 2026-08-16：修复 JDBC URL `characterEncoding=utf8mb4` 编码错误（Connector/J 不支持，改 utf8；同步修正 application.yml、application-local.yml、README）
- 2026-08-16：GlobalExceptionHandler 增加 NoResourceFoundException → 404（此前不存在接口返回 500）
- 2026-08-16：**步骤 3 验收通过**（13 项 curl 验证）：注册/重复注册 400/登录/token 访问 me/无 token 401/旧密码错误 400/改密成功/新密码登录/admin 登录带 mustChangePassword=true/改密后标记清除/普通用户访问 admin 403/admin 访问不存在接口 404

## 步骤 4 — 文章后端

- 2026-08-16：SentenceSplitter 句子切分器（. ! ? 句界、缩写白名单 + 多段缩写 u.s/e.g、数字小数点保护、点后字母视为缩写中间点、句尾引号归属、空白折叠）；单元测试 10 用例（含 Mr./U.S./e.g./3.14/引号/空输入）
- 2026-08-16：Article/Sentence/SentenceAnnotation/UserProgress/UserFavoriteSentence 实体 + Mapper（含级联删除注解 SQL）
- 2026-08-16：管理端文章 CRUD + 上/下架（POST/PUT/DELETE /api/admin/articles、POST /{id}/status、GET 列表）；用户侧书架分页（仅上架，难度/标签过滤）、文章元信息、GET /{id}/reading 阅读载荷（元信息+句子+标注；进度/生词/收藏集合留待步骤 5 填充）
- 2026-08-16：编辑正文变更 → 重切分（删旧句子/标注/进度）；删除 → 级联句子/标注/进度/收藏
- 2026-08-16：**步骤 4 验收通过**：单测 10/10；curl 建文切分 8 句与人工一致（Mr./U.S./3.14/引号句均未误切）→ 默认下架书架不可见 → 上架后可见 → reading 载荷完整 → 编辑重切分为 2 句（Dr. 保护）→ 删除级联干净（文章与句子均 0 行）
