# 技术架构设计文档（doc/00）

> 版本 v1.0 · 2026-08-16 · 与 [01-ui-design.md](01-ui-design.md)、[02-project-plan.md](02-project-plan.md) 配套
> 技术栈固定：Vue3+TS+Vite+Pinia+Element Plus(仅管理端) ｜ Spring Boot 3 + MyBatis-Plus + MySQL 8 + JWT ｜ DeepSeek (deepseek-v4-flash) ｜ 原生 speechSynthesis ｜ PC+移动端

---

## 1. 数据库设计（7 张表）

引擎 InnoDB，字符集 utf8mb4，时间字段 DATETIME，`updated_at` 用 `ON UPDATE CURRENT_TIMESTAMP`。无物理外键，应用层保证一致性。

| 表名 | 用途 |
|---|---|
| `user` | 用户（role 0普通/1管理员，must_change_password 首登改密标记） |
| `article` | 文章元信息 + 英文全文原文（content_en MEDIUMTEXT） |
| `sentence` | 逐句英文，上传时服务端切分落库（article_id + seq 唯一） |
| `sentence_annotation` | AI 产出 1:1 sentence：中文翻译/句子解释/句子成分 JSON/单词标注 JSON/gen_status 状态机 |
| `user_progress` | 阅读进度（read_sentences 已读索引 JSON + progress 冗余值 + is_completed + last_read_at） |
| `user_vocab` | 生词本（user_id+word 唯一，小写规范化，存出处句） |
| `user_favorite_sentence` | 例句收藏（user_id+sentence_id 唯一） |

### 关键设计决策

- **正文切分**：管理端上传/编辑时，原文存 `article.content_en`，同时 `SentenceSplitter` 切分逐句写 `sentence` 表（含 seq）。阅读页不做切分。切分规则：`.` `!` `?` 为句界；缩写白名单保护（Mr. U.S. e.g. 等）+ 数字小数点不切；引号句尾句号归入引号内。
- **整篇翻译不单独生成**：= 逐句 `content_zh` 按 seq 拼接。零额外成本且天然逐句对齐；"连贯语体全文"列为 v2。
- **JSON 字段**：`article.tags`、`sentence_annotation.components`（`[{type:"主语", text, detail}]` 中文语法术语）、`sentence_annotation.words`（`[{word, pos, meaning, role}]` 覆盖全部实词）、`user_progress.read_sentences`（索引数组）。
- **衍生指标不落库**：精读文章数量 = `COUNT(user_progress WHERE user_id=? AND is_completed=1)` 实时聚合。
- **sentence 与 annotation 分离**：上传数据与 AI 数据生命周期不同（编辑重切分 vs 反复生成重试），分离后重切分只删旧 sentence+annotation，gen_status 独立索引高效。

完整 DDL 见 `backend/src/main/resources/db/schema.sql`，种子数据见 `db/seed.sql`（预置 admin + 示例文章）。

## 2. 后端 API（前缀 /api，统一 Result{code,message,data}，JWT Bearer）

| 模块 | 接口 | 说明 | 鉴权 |
|---|---|---|---|
| 认证 | POST /auth/register | 用户名唯一校验 | 公开 |
| 认证 | POST /auth/login | 返回 {token, user}，含 must_change_password 标记 | 公开 |
| 认证 | PUT /auth/password | 改密，首登改密后清标记 | 登录 |
| 用户 | GET /users/me | 资料 + {completedCount, readingCount, totalProgress} | 登录 |
| 用户 | PUT /users/me | 改昵称/头像 | 登录 |
| 用户 | GET /users/me/recent-reading | 按 last_read_at 倒序带进度 | 登录 |
| 文章 | GET /articles?page&size&keyword&difficulty&tag | 书架分页（仅上架） | **公开** |
| 文章 | GET /articles/{id} | 元信息 | **公开** |
| 阅读 | GET /articles/{id}/reading | **一次拉全**：元信息+全部句子(含标注)+我的进度+生词集合+收藏集合 | 登录 |
| 阅读 | POST /articles/{id}/progress | 批量上报已读索引，服务端并集去重，返回 {progress,isCompleted} | 登录 |
| 生词 | GET/POST /vocab，DELETE /vocab/{word} | 分页列表/加入(幂等)/删除 | 登录 |
| 收藏 | GET/POST /favorites/sentences，DELETE /favorites/sentences/{sentenceId} | 分页/收藏/取消 | 登录 |
| 管理 | POST/PUT/DELETE /admin/articles[/{id}] | CRUD；编辑正文 → 重切分 + 清标注/进度（前端确认弹窗） | 管理员 |
| 管理 | POST /admin/articles/{id}/status | 上架/下架 {status:0\|1} | 管理员 |
| 管理 | GET /admin/articles | 管理列表（含生成进度汇总） | 管理员 |
| 管理 | GET /admin/articles/{id}/gen-status | 四态计数 + 逐句状态 | 管理员 |
| 管理 | POST /admin/articles/{id}/generate | 启动异步生成 {target:"missing"\|"all"}，进行中 409 | 管理员 |
| 管理 | POST /admin/articles/{id}/sentences/{sentenceId}/generate | 单句生成/失败重试 | 管理员 |
| 管理 | POST /admin/articles/{id}/generate/cancel | 取消任务（批间生效） | 管理员 |
| 管理 | GET /admin/stats | 用户数/文章数/上架数等 | 管理员 |

**鉴权**：HandlerInterceptor —— AuthInterceptor 解析 JWT 写入 ThreadLocal UserContext；AdminInterceptor 校验 role==1。密码编码 spring-security-crypto 的 BCryptPasswordEncoder（单依赖）。

## 3. AI 生成模块（最高复杂度）

### 流程

```
管理员点"生成" → POST /generate → 校验无进行中任务（否则 409）
→ 按 seq 分批（默认 batchSize=5，可配；批内字符上限 3000）
→ 每批调 DeepSeek /chat/completions（response_format=json_object, temperature=0.3, 超时 120s）
→ 解析+校验 → 逐句 UPSERT annotation（gen_status 实时落库）
→ 批级失败重试 2 次（1s/3s 退避）→ 仍败逐句标 3 + gen_error，可单句重试
```

### 句子级状态机（DB 持久化，唯一事实源）

```
0 未生成 → 1 生成中 → 2 已生成
                   ↘ 3 生成失败（单句重试：3 → 1 → 2）
```

文章级汇总由句子级推导（未开始/生成中/部分失败/已完成）。

**可靠性保证**：任务在服务端线程池后台执行，与浏览器请求生命周期解耦——页面关闭/刷新不影响，每句状态实时落库，重开页面轮询即见进度；服务重启后残留"生成中"在下次任务启动时重置为"未生成"。

### Prompt 要点

System：`你是专业的英语精读讲解助手…只输出一个 JSON 对象，不要输出任何解释性文字或 Markdown 代码块。`
User：传入 `[{seq, text}...]`，要求输出 `{results:[{seq, content_zh, explanation, components:[{type,text,detail}], words:[{word,pos,meaning,role}]}]}`，results 数量与输入一致、seq 一一对应；words 覆盖全部实词；components 用中文语法术语。

### 解析三层防护

① 剥 ```json 围栏 → ② JSON.parse 失败则宽松提取（首 `{` 到末 `}`）→ ③ 仍败则该批重试。逐项校验（字段齐全、seq 对应），坏项单独补发小请求。

### 成本控制

默认 target=missing 增量生成；预估 token 提示（句数 × ~250 output）；batchSize 可配；单句试生成；批间检查取消标记；max_tokens 封顶（2000 + 400×batchSize）。

## 4. 阅读页前端架构

- **数据**：`GET /reading` 一次拉全，按索引构造 enList/zhList 对齐数组（索引对齐即对齐，零 DOM 测量）。
- **布局**：桌面 grid 双栏（≥1024px），左右独立滚动容器；移动端 Tab 模式（单 pane，同步滚动/双向高亮自动禁用）+ 可选上下对照模式。
- **双向高亮**：readingStore 单一 hoveredIndex，两栏句子 index 相等派生 synced class，纯状态驱动。
- **同步滚动**：预存各句 offsetTop，二分查找锚句，scrollIntoView(瞬时)，互斥锁 50ms 窗口，主导源判定（用户滚动差值 >4px 才算）。
- **气泡**：SentenceBubble（解释/中文/成分标签行/单词列表/收藏/整句朗读）+ WordBubble（词性/意思/作用/发音/加入生词本）；fixed 定位自动翻转；同屏单实例；点击外部/Esc/切换句子关闭；收藏与生词乐观更新。
- **TTS 控制器**（services/tts.ts 单例）：语音候选 en-US→en-*→默认（voiceschanged 监听 + 无英文音色兜底提示，音色 id 持久化可手选）；整篇顺序朗读 + 当前句高亮 + 双向滚动跟随；pause 不可靠 → cancel+记录索引+重播；语速 0.5–1.5 持久化；离开页面 cancel。
- **进度**：IntersectionObserver(threshold 0.2) 收集已读索引，3s/30条/pagehide 防抖批量上报。
- **翻译开关**：工具栏"显示翻译"切换 zh-pane 显隐，默认关。

## 5. 移动端（<1024px）

- 默认 Tab 模式：`[英文 | 中文]` 切换标签 + 朗读 + 翻译开关，同时渲染一个 pane。
- 上下对照模式（设置内可选）：单滚动容器内 EN/ZH 配对卡片，天然同滚。
- tap=click 开气泡；气泡底部留白；进度观察者 root 换为当前滚动容器；底部安全区 env(safe-area-inset-bottom)。

## 6. 关键技术风险与对策

| 风险 | 对策 |
|---|---|
| 中文系统无英文音色 | voiceschanged 监听；候选 en-US→en-*；无则 toast 提示一次；音色可手选持久化 |
| Chrome pause 不稳 / iOS 需手势 | cancel+记录索引+重播；所有 speak 在用户事件链内触发 |
| 同步滚动反馈环 | 锚句二分 + 瞬时定位 + 互斥锁 + 主导源判定；移动端 Tab 天然禁用 |
| AI 返回 JSON 漂移 | response_format=json_object + 三层解析 + 批级重试 + 坏项单补 + gen_error 记录 |
| 长文生成耗时 | 后台异步 + DB 状态 + 2s 轮询 + 批间取消检查 + 单句重试 |
| 越权访问管理接口 | 服务端 AdminInterceptor 强校验，前端只做展示控制 |
| 句子切分边界 | 缩写白名单 + 数字保护 + 引号规则 + 单元测试覆盖 |
| 阅读载荷过大 | 后端 gzip 压缩 + 骨架屏 + content-visibility:auto |
| EP 样式污染阅读区 | 管理端路由级懒加载 + 阅读区零 EP 依赖 |
| 编辑重切分导致进度失效 | 确认弹窗 + 级联清进度 + total_count 快照 |
