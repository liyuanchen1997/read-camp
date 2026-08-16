# 开发变更记录

> 追加式记录，每步开发完成后追加一条。格式：`- [步骤 N] 日期：变更内容（验收结果）`

## 规则与约定

- 2026-08-16：**阅读页章节/目录（步骤 16，迭代①）**——reading store 新增 chapters + chapterGroups 派生（按 chapterId 顺序分组，标题查 chapters ?? 文章标题，NULL 归 legacy 组）；SentencePane 改接 chapterGroups，渲染章标题（`data-chapter` 锚点、双栏对称同渲染、scroll-margin-top 避工具栏），章内段落分组 key 用 (chapterId, para) 二元组防跨章碰撞；新增 ChapterToc（aside 桌面固定列 + dropdown 移动端折叠下拉，章节列表/当前章高亮/上一章下一章，首末章 disabled，零 EP）；ReadingView 桌面 grid 200px 目录列（show-zh 时 200px 1fr 1fr）、移动端工具栏 ☰ 目录按钮 + 下拉、scrollToChapter 用 [data-chapter] 瞬时 scrollIntoView（避免 smooth 与同步滚动互斥锁冲突回弹）、当前章高亮 IntersectionObserver（root=滚动容器，顶部 25% 区，重建挂在 reObserve/onMqlChange）。vue-tsc + build 通过；浏览器走查待用户
- 2026-08-16：**管理端章节编辑器（步骤 15，迭代①）**——api/admin.ts 新增 ChapterForm/ChapterDto，ArticleForm/ArticleDetail 带 chapters；AdminArticleEditorView 正文 textarea 改为**章节卡片列表**（序号徽标 + 标题 input（留空自动补"第 N 章"）+ 正文 textarea + 上移/下移/删除 + 添加章节，恒 ≥1 章）；回显优先 chapters（旧文章回退单章=文章标题+contentEn）；保存 payload 带 chapters（content=各章 trim 后空行拼接，与后端 joinChapters 一致）；重切分弹窗比较改为 `contentEn.trim() !== joinedContent.trim()`（仅章标题变化不弹，文案补"含章节增删/排序"）。vue-tsc + build 通过；回显/弹窗/迁移行为由步骤 14 API 级验证覆盖
- 2026-08-16：**章节后端服务与 API（步骤 14，迭代①）**——DTO 新增 ChapterRequest/ChapterDto；ArticleRequest 可选 chapters（缺省=单章兼容旧调用）、ArticleDetailDto/ReadingPayload + chapters、SentenceDto + chapterId；ArticleServiceImpl：normalizeChapters/joinChapters（章间 `\n\n` 拼接）、splitAndStore 逐章切分（章内 para 0 起、seq 全局递增、chapter_id 落库）、update 三分支（正文变化→reSplit 级联删 chapter；正文未变无 chapter 行→**chapterize** 补章不删标注/进度；仅标题变化→只更新 title）、readingPayload 恒非空 chapters（无章节合成单章 id=null）、新增 detail service 方法（controller 改走 service）。**验收 9 项 curl 全过**：建文带/不带 chapters、reading 载荷、detail 回显、仅改标题不重切分、改正文重切分重建 chapter、删除级联、存量旧文章合成单章、chapterize 迁移（10009 补章节行，标注 20 条保留）
- 2026-08-16：**全仓库文档滞后审计修复**（用户反馈"AI 已可配置但文档仍写 DeepSeek"）——双代理扫描全仓库（文档+代码注释+用户可见文案），修复 20+ 处滞后：①AI 模型可配置化表述（README/CLAUDE/AGENTS/backend/00-design 头部/02-plan 技术栈，统一为"OpenAI 兼容接口，默认 DeepSeek，ai_config 可配置"）；②事实错误（表数量 7→9 张含 chapter+ai_config、batch 默认 5→3、批级退避 1s/3s→0.5s/2s、max_tokens 公式、同步滚动锁 50ms→120ms）；③功能回填（01-ui §3.6 补 AI 配置页/生成中标签+轮询/模型名动态提示语；frontend 路由清单补 /admin/ai-config；TTS 女声优先链多处补写）；④02-plan 步骤 13 状态置进行中、过期待办划除；⑤backend/CLAUDE.md 铁律 6 改为"运行时配置走 ai_config 表"；⑥代码注释（AiGenerationServiceImpl 批次 3 + OpenAI 兼容）。合理保留：类名 DeepSeekClient、环境变量 DEEPSEEK_API_KEY、yml 初始默认值、历史记录
- 2026-08-16：**迭代计划落地文档**（用户需求"先更新计划到文档再开发"）——两个新迭代：①文章分章节（章节编辑+阅读页目录/章节展示/翻阅）；②单词英语音标（讲解弹窗/WordBubble/生词本/收藏页）。已同步 doc/02-project-plan.md（步骤 13-17 + 迭代需求记录）、doc/00-design.md（chapter 表 + sentence.chapter_id + 章节归一化三分支 + words phonetic + API 表）、doc/01-ui-design.md（阅读页目录/章节标题/音标/收藏页单词列表/编辑器章节卡片）
- 2026-08-16：**生词本与例句收藏补全解释/翻译/语音**（用户需求）——生词列表按出处句 AI 标注带出 pos/meaning/role，卡片显示词性+中文意思+作用+🔊发音；收藏列表带出标注的 zh 翻译与 explanation 讲解，卡片显示翻译+讲解+🔊朗读；无标注时字段为 null 不显示；同步 doc/00-design.md API 表
- 2026-08-16：**句子粘连切分**（用户反馈"修复顺带发现"）——点后无空格紧跟大写字母时（如 strong.Tom 两句粘连），旧规则一律视为缩写内部不切。修复：点前为完整单词（≥3 字符）视为句界切开；缩写内部保护保留（A.B.C. 等每段点前 1 字符仍不切）；新增 2 个单元测试。10009 已按新规则重切分（19 句、3 段）
- 2026-08-16：**段落切分格式化容错**（用户反馈）——Billy and Tom（10008）正文以单换行分段、无空行，旧切分器只认空行（`\n\n`）导致全篇一段；用户明确"不能指望用户输入双换行"。修复：SentenceSplitter 段落判定改为**含空行→空行分段（段内单换行折叠为空格）；无空行→单个换行即段落边界**；空白段跳过、段落号保持连续；更新/新增 3 个单元测试（14 全过）；同步 doc/00-design.md §1。**注意：10009 添加时后端仍跑旧代码，已直接对 10009 数据执行重切分；后端需重启新代码才对后续保存生效**
- 2026-08-16：**阅读页段落间距修复**（用户反馈）——正文按 para 分组的渲染逻辑与数据均正确（句子 inline 连续成文），但 `.para` 缺少间距样式，段与段视觉上无分隔、看不出分段。修复：段落间距 1.5em（相对字号缩放，双栏一致；末段不追加）；同步 doc/01-ui-design.md §2.4
- 2026-08-16：**文章列表展示 AI 生成中状态**（用户反馈）——AI 标注列原来只显示 done/total 与失败数，生成任务运行中无任何标识。修复：运行中显示"⏳ 生成中"高亮标签；列表页任一任务运行中自动 2s 轮询进度（关闭生成弹窗后表格仍实时跟随，全部完成即停）
- 2026-08-16：**生成弹窗提示语跟随 AI 模型配置**（用户反馈）——生成注解弹窗原提示语硬编码"DeepSeek 模型按需生成"，更换模型后文案不匹配。修复：弹窗打开时读取当前 ai_config，提示语动态显示实际模型名（`{{model}} 按需生成…`），拉取失败兜底"AI 模型"
- 2026-08-16：**气泡视口溢出修复**（用户反馈）——根因：Teleport 内容挂载前 bubbleEl 为 null，单次测量取默认高度 200 导致翻转条件不触发，气泡按全高渲染溢出视口。修复：新增 useBubblePosition composable（onMounted 后重测 + **ResizeObserver 持续校正**翻转/贴底 + 保守高度兜底）；气泡 maxHeight 内联输出（vh→dvh 动态视口、visualViewport 口径统一）；SentenceBubble/WordBubble 统一接入
- 2026-08-16：**单词解释改点击展示**（用户反馈迭代）——经 hover 浮层两轮修复（relatedTarget/跟随鼠标）仍不理想后，按用户要求放弃并改为**点击单词展示浮层**：常驻可操作（发音/生词本）、再点同一单词关闭、点其他单词切换、点气泡其他区域关闭；同步 doc/01-ui-design.md §3.4
- 2026-08-16：**AI 模型可配置**（用户需求）——新增 ai_config 表（单行 id=1）：base_url/api_key/model/batch_size/temperature/timeout_seconds；管理后台"AI 配置"页编辑 + **测试连接**（先保存再用当前配置发测试请求）；支持任意 OpenAI 兼容服务（DeepSeek/通义/本地 Ollama 等），切换后下次生成即生效；DeepSeekClient 按 (baseUrl+apiKey) 动态重建连接，批量大小改从配置读取；yml 仅作初始默认值（首次访问落库）；已同步 doc/00-design.md §3 与 API 表
- 2026-08-16：**管理后台仅桌面端**（用户需求）——手机浏览不提供管理后台：AppHeader 导航与用户菜单、个人中心入口在 <1024px 隐藏；路由守卫对手机端访问 /admin 重定向回书架；新增 utils/device.ts 响应式桌面判断
- 2026-08-16：**单词解释改 hover 浮层**（用户反馈）——句子气泡内单词项鼠标移入即冒泡单词解释（word/pos/meaning/role + 发音 + 加入生词本），不再另开弹窗；180ms 延迟关闭可移入操作；WordBubble 改为紧凑浮层（280px），移除 ReadingView 点击切换弹窗逻辑；同步 doc/01-ui-design.md §3.4
- 2026-08-16：**句子气泡增强**（用户反馈）——①气泡顶部展示英文原句（衬线突出）；②句子成分标签展示"成分类型 + 对应原文片段"（主语 · The lion），hover 显示作用说明；同步 doc/01-ui-design.md §3.4
- 2026-08-16：**句子讲解改中文**（用户反馈）——Prompt 中 explanation 由"英文讲解"改为"中文讲解"（系统提示明确"所有解释性内容一律使用中文"）；存量 3 篇文章（10001/10002/10006）target=all 重新生成，验证全部讲解以中文为主体（英文仅为原文词汇引用，符合精读讲解规范）；同步 schema.sql 注释与 doc/00-design.md
- 2026-08-16：用户要求——**每次 git 提交前必须检查并更新相关文档**（对照 diff 检查 README/CLAUDE/AGENTS/doc/**，滞后先更新再提交），已同步至根及前后端 CLAUDE.md / AGENTS.md
- 2026-08-16：配置 PreToolUse hook 强制文档同步——`.claude/settings.json` + `.claude/scripts/check-doc-sync.py`（git commit/push 前自动检查：非文档变更未更新 changelog、或代码变更无 doc/ 更新 → 拦截并提示调用 git-docs-sync）
- 2026-08-16：**品牌名调整**（用户需求）——"ReadCamp" 改为"英语精读训练营"（AppHeader/登录注册页/首页/index.html 标题/README/设计文档/种子数据注释）；代码内部标识保留（com.readcamp 包、readcamp 库名、artifactId），如需一并重命名需另行评估
- 2026-08-16：**TTS 音色调整**（用户需求）——优先女性英文语音（Samantha/Karen/Moira 等 22 个常见女声名单）：en-US 女声 → en-* 女声 → en-US → en-* → 默认；用户手选音色仍优先（localStorage）

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

## 步骤 5 — 学习数据后端

- 2026-08-16：ProgressService——进度批量上报（服务端并集去重、越界索引过滤、total_count 快照、100% 置 is_completed+completed_at）、近期阅读（last_read_at 倒序 10 条联表）、聚合统计（完成数/进行中/平均进度）
- 2026-08-16：VocabService——生词本分页/加入（小写规范化、已存在幂等）/删除；FavoriteService——例句收藏分页（联表带文章标题）/收藏/取消（均幂等）
- 2026-08-16：接口接入：POST /api/articles/{id}/progress、GET /api/users/me/recent-reading、/api/vocab CRUD、/api/favorites/sentences CRUD；/users/me 补聚合统计（completedCount/readingCount/totalProgress）；reading 载荷补全 progress/vocabWords/favSentenceIds
- 2026-08-16：**步骤 5 验收通过**（14 项 curl）：并集去重（重复上报仍 23%）、越界过滤（999/-1 忽略）、部分进度 5/13=38%、100%→is_completed=true、近期阅读排序、聚合 (100+38)/2=69、生词大小写幂等、收藏联表标题、reading 载荷三字段已填充

## 规则与约定

- 2026-08-16：**书架公开浏览**（用户需求）——首页/书架无需登录即可浏览文章列表与详情，进入精读阅读（/reading、进度、生词、收藏）才需登录；后端 WebConfig 放行 /api/articles 与 /api/articles/*，前端路由 / 移除 requiresAuth，已同步 doc/00-design.md §2 与 frontend/doc/README.md

## 步骤 12 — 移动端+打磨（完成）

- 2026-08-16：移动端阅读页 **Tab 模式**（工具栏 [英文|中文] 切换，单 pane 渲染）+ **上下对照模式**（⇅ 按钮切换，单容器上下堆叠天然同滚）；Tab/模式切换后重建进度观察（IntersectionObserver）
- 2026-08-16：移动端细节——工具栏紧凑化（标题截断/进度条收窄/语速滑块隐藏）、阅读区底部安全区 env(safe-area-inset-bottom)、桌面"显示翻译"开关在移动端由 Tab 替代
- 2026-08-16：**双主题适配**——theme store 同步 Element Plus dark class（管理后台暗色模式生效）；双主题全量走查待用户验收
- 2026-08-16：**性能**——后端开启 gzip 压缩（阅读载荷实测 5.6KB）；段落 content-visibility:auto 分片渲染（长文）；Element Plus 独立 chunk 分包（前台首屏不加载 EP 代码）
- 2026-08-16：**步骤 12 验收**：build 通过（分包生效）；gzip Content-Encoding 验证通过；6 个路由 200。手机尺寸/双主题浏览器走查待用户验收
- 2026-08-16：**12 步全部完成**——功能开发完毕，进入体验验收与迭代阶段

## 步骤 11 — 管理后台前端

- 2026-08-16：后端补充——GET /api/admin/stats 仪表盘（用户/文章/上架/句子/生成统计）、GET /api/admin/articles/{id} 管理端详情（含正文原文，编辑回显）
- 2026-08-16：api/admin.ts（文章 CRUD/生成/进度/取消/单句重试/stats）
- 2026-08-16：AdminArticlesView——列表（难度标签/句子数/AI 标注进度汇总列/失败红标）、状态筛选、上下架开关、删除确认、新建入口、生成按钮
- 2026-08-16：GenProgressDialog——生成模式选择（增量/全量）、四态计数卡、进度条、**2s 轮询**（关闭窗口不中断提示）、取消任务、**失败句红色列表+单句重试**、预估 token 成本提示
- 2026-08-16：AdminArticleEditorView——新建/编辑（标题/简介/难度/标签/正文），**正文变更弹窗确认重切分**（警告清空标注与进度），保存提示切分句数与词数
- 2026-08-16：AdminStatsView 仪表盘六卡统计；路由注册 /admin/articles/new、/:id/edit
- 2026-08-16：**步骤 11 验收**：build 通过（修复模板 TS 注解致 rolldown 崩溃、#footer 插槽位置）；全流程 API 验证：建文→生成 2 句 8s 完成→上架→书架可见→stats 数据正确；4 个后台路由 200。浏览器交互（列表/对话框轮询/重切分确认）待用户验收

## 步骤 10 — AI 生成后端

- 2026-08-16：DeepSeekClient（OpenAI 兼容 /chat/completions，json_object，120s 连接/读取超时，密钥走配置）；AsyncConfig AI 生成线程池（任务与请求生命周期解耦，页面关闭不中断）
- 2026-08-16：GenTaskRegistry 内存任务注册（互斥 409 / 取消标记 / ETA）；DB gen_status 为唯一事实源，启动时重置孤儿"生成中"状态
- 2026-08-16：AiGenerationService——分批（默认 3，字符上限 3000）、Prompt 构建（系统+输出约束）、三层解析防护（剥围栏→宽松提取→批级重试 2 次退避）、逐项校验+坏项单补、UPSERT 落库、单句生成/失败重试（3→1→2）、批间取消检查
- 2026-08-16：管理接口——POST /{id}/generate（missing 增量/all 全量）、GET /{id}/gen-status（四态计数+逐句）、POST /sentences/{sid}/generate（单句重试）、POST /generate/cancel
- 2026-08-16：**实测发现并修复**：① deepseek-v4-flash 为 reasoning 模型，batch=5 时思考 token 爆炸导致 content 为空 → 默认 batchSize 改 3 + max_tokens 放宽（max(8000, 3000+1500/句)）；② 批内复查过滤条件误排除 FAILED 句（任务空转）→ 改为排除 DONE/GENERATING；③ RestClient 无超时可能挂起 → 应用 120s
- 2026-08-16：**步骤 10 验收通过**（真实 DeepSeek API）：10001 全 13 句 done（含失败句自动重试）、10002 2 句完成、重复启动 409、单句重试 200、cancel 返回正确、gen-status 四态计数准确、reading 载荷带完整标注（zh/explanation/components/words，气泡数据源就绪）

## 步骤 9 — 阅读页 v2（气泡 + TTS）

- 2026-08-16：services/tts.ts——speechSynthesis 单例控制器：整篇顺序朗读（currentIndex 推进 + onProgress 回调）、语音候选 en-US→en-*→默认（voiceschanged 监听 + 无英文音色提示一次）、语速 0.5-1.5 持久化（播放中调整重播当前句）、pause 不可靠降级方案（resume 后 250ms 检查未在说话则 cancel+当前句重播）、独立朗读（单句/单词）先打断
- 2026-08-16：reading store 扩展 vocabWords/favSentenceIds（载荷初始化）+ isFav/setFav/hasVocab/setVocab 乐观更新辅助
- 2026-08-16：SentenceBubble——句子讲解气泡（fixed 锚定 + 底部空间不足自动上翻 + 左右回夹）：句子解释/中文意思/成分标签行（hover 见 detail）/单词列表（点击开单词气泡）/收藏例句（❤ 乐观更新+回滚）/朗读本句；未生成标注显示提示
- 2026-08-16：WordBubble——单词气泡：word/pos/meaning/role + 发音 + 加入生词本（✓已加入可移除，乐观更新+回滚，带出处句）
- 2026-08-16：ReadingView 接入——点击句子开气泡（同屏单实例）、单词点击切换词气泡、点击外部/Esc 关闭；工具栏朗读控制（▶播放/⏸暂停/⏹停止 + 语速滑块），朗读当前句高亮 + 双侧滚动跟随，离开页面 stop
- 2026-08-16：**步骤 9 验收**：npm run build 通过（修复 SentencePane 事件透传 3 参签名）；气泡完整内容（解释/成分/单词标注）需步骤 10 生成 AI 标注后统一浏览器验收

## 步骤 8 — 阅读页 v1

- 2026-08-16：stores/reading.ts——hoveredIndex 单一数据源（双向高亮由两栏索引相等派生，零跨 DOM 查询）、playingIndex 预留、进度状态
- 2026-08-16：styles/reading.css——纸感双栏布局（grid 1fr/0fr↔1fr/1fr 过渡）、句子 hover 淡金底+左缘色条、朗读句样式预留、移动端单列上下对照（<1024px 页面整体滚动）
- 2026-08-16：SentenceBlock/SentencePane——句子块组件（data-seq 锚点、mouseenter/leave、未生成中文占位"标注未生成"）
- 2026-08-16：useScrollSync——锚句二分（视口顶部 25% 处句子）+ scrollIntoView 瞬时定位 + 120ms 互斥锁防反馈环，桌面双栏启用
- 2026-08-16：useReadTracking——IntersectionObserver(threshold 0.2) 已读标记，3s/30 条/pagehide 防抖批量上报，失败回填重试；桌面 root=英文栏、移动端 root=视口
- 2026-08-16：ArticleToolbar——标题/4px 进度条/翻译开关（show-zh 列宽过渡）；ReadingView 根组件（载荷加载/错误态/返回书架）
- 2026-08-16：**步骤 8 验收**：npm run build 通过（修复 start 导出缺失、toolbar null 类型）；/reading/10001 页面 200；阅读载荷经代理完整（13 句/进度 38%）。同步滚动/双向高亮交互体验待用户浏览器验收
- 2026-08-16：**排版修正（用户反馈）**——阅读页改为"正常文章"展示：sentence 表新增 `para` 段落号（schema.sql/seed.sql/本地库 ALTER 同步），SentenceSplitter 按空行分段（单换行折叠为空格不切段）、测试更新为 12 用例；SentenceDto 载荷带 para；前端按段落分组渲染、段内句子 **inline 流式**（句间空格衔接，视觉连续成文），段落间空行；句子级元素保留用于悬停高亮/点击/同步滚动锚点。验证：两段文章切分 p0/p0/p1 正确，载荷与前端均生效

## 步骤 7 — 书架+近期+个人页面

- 2026-08-16：api/article.ts（书架/详情/reading/进度上报）、api/vocab.ts、api/favorite.ts 模块
- 2026-08-16：工具：formatRelativeTime 相对时间、DIFFICULTY 难度徽章、coverGradient/coverInitial（标题 hash 暖色渐变封面）
- 2026-08-16：ShelfView——筛选栏（关键词+难度 chips）+ 网格卡片（渐变封面+首字母水印+进度角标+难度徽章+词句数）+ 加载更多 + 空态；登录后从近期阅读匹配进度角标；响应式栅格
- 2026-08-16：RecentReadingView——卡片列表（封面/标题/4px 细进度条/相对时间/已完成徽章）+ 空态
- 2026-08-16：ProfileView——头像昵称编辑弹窗、统计卡（完成数/进行中/平均进度）、学习数据入口（近期/生词/收藏/改密）、管理后台入口按钮（admin 可见）、首登强制改密自动弹窗
- 2026-08-16：VocabView（搜索/分页/出处句/删除）、FavoritesView（例句+文章链接/取消收藏）分页列表
- 2026-08-16：**步骤 7 验收**：npm run build 通过（修复模板 as 断言/fromEntries 等 4 处类型问题）；/ /recent /vocab /favorites /profile 全部 200；书架接口经代理返回数据。浏览器视觉走查待用户验收

## 步骤 6 — 前端框架+认证

- 2026-08-16：api/auth.ts、api/user.ts 模块；userStore 补 login/register/fetchMe（刷新恢复登录态）
- 2026-08-16：路由全表（/ /login /register /reading/:id /profile /vocab /favorites /recent /admin/* /404）+ guard.ts（requiresAuth 跳登录带 redirect 回跳、requiresAdmin 校验角色、刷新后 fetchMe 恢复）
- 2026-08-16：LoginView/RegisterView（纸感卡片、衬线品牌标题、内联错误提示、redirect 回跳；登录后 mustChangePassword 跳个人中心改密）；AppHeader（Logo/导航/主题切换按钮/用户头像菜单下拉/未登录态登录注册按钮，登录注册页隐藏头部）；App.vue 布局；各页面占位 + NotFoundView + 管理后台布局骨架
- 2026-08-16：**步骤 6 验收**：npm run build（vue-tsc）通过；dev server 启动后 / /login /register /admin 全部 200，/api 代理连通。浏览器交互走查（注册→登录→刷新→登出、主题切换持久）待用户验收
