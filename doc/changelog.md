# 开发变更记录

> 追加式记录，每步开发完成后追加一条。格式：`- [步骤 N] 日期：变更内容（验收结果）`

## 规则与约定

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
