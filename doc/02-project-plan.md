# 项目计划表（doc/02）

> **当前进度：第一版开发完成** —— 步骤 1-17 全部完成 ✅（含迭代①文章分章节、迭代②单词音标），功能交付，进入使用/维护阶段
> 迭代需求（2026-08-16 确认）：①文章分章节编辑 + 阅读页章节展示/目录/翻阅；②单词英语音标（讲解弹窗/WordBubble/生词本/收藏页）。设计见 doc/00-design.md §1/§3/§4 与 doc/01-ui-design.md §3。
> 规则：每步一个模块，完成后更新状态与日期、追加 changelog，然后**暂停等待用户指令**。

## 总体信息

- 项目：英语精读训练营（原 ReadCamp）
- 技术栈：Vue3+TS+Vite+Pinia+Element Plus(管理端) ｜ Spring Boot 3 + MyBatis-Plus + MySQL 8 + JWT ｜ AI（OpenAI 兼容，默认 DeepSeek，可配置）
- 设计基线：doc/00-design.md（技术）、doc/01-ui-design.md（UI）
- 环境：node 24 / Java 21 / Maven 3.9.9（/Users/liyuanchen/Desktop/code/utils/apache-maven-3.9.9/bin/mvn）/ MySQL 8（本地）

## 步骤状态表

| 步骤 | 模块 | 预计交付 | 验收要点 | 状态 | 完成日期 | 备注 |
|---|---|---|---|---|---|---|
| 1 | 项目骨架+文档 | 根文档 4 份 + doc/ 四份设计文档 + frontend/ backend/ 各自 CLAUDE.md、AGENTS.md、doc/ 说明文档；前端 Vite 骨架；后端 Spring Boot 骨架 + /api/health；Vite proxy→8080 | 两端可启动、health 通 | 🟢 已完成 | 2026-08-16 | 8080 曾与 memo-lanbitou 冲突，已停其进程；前端 build 通过；health 直连与代理均 200 |
| 2 | 数据库 | db/schema.sql（7 表）+ seed.sql（预置 admin 首登改密标记 + 示例文章） | 脚本幂等可重复执行、JSON 列抽查 | 🟢 已完成 | 2026-08-16 | 已入库执行验证：幂等 2 次行数不变（1/1/13）；JSON 列/唯一键/索引抽查通过 |
| 3 | 认证后端 | 注册/登录/改密、BCrypt、JwtUtil、AuthInterceptor+UserContext、全局异常处理 | curl 全流程：注册→登录→带 token 访问 me→改密 | 🟢 已完成 | 2026-08-16 | 13 项 curl 验证全通过（含 admin 首登改密/403/404）；修复 JDBC characterEncoding=utf8mb4 编码错误 |
| 4 | 文章后端 | 管理端文章 CRUD+上下架；SentenceSplitter（缩写保护）；上传即切分；GET /reading 载荷 | 切分数与人工一致、缩写不误切 | 🟢 已完成 | 2026-08-16 | 单测 10/10；curl 验证：8 句切分正确（Mr./U.S./3.14/引号）、上下架、书架过滤、reading 载荷、编辑重切分、级联删除 |
| 5 | 学习数据后端 | 进度并集上报+快照；近期阅读；生词 CRUD；收藏 CRUD；/users/me 聚合 | 重复上报不重复计数、100%→is_completed=1 | 🟢 已完成 | 2026-08-16 | 14 项 curl 验证通过：并集去重/越界过滤/完成标记/近期阅读/生词幂等/收藏联表/聚合统计/reading 载荷补全 |
| 6 | 前端框架+认证 | axios 实例、userStore、路由守卫、登录/注册页、AppHeader、双主题机制 | 注册→登录→刷新保持→登出；主题切换即时+持久 | 🟢 已完成 | 2026-08-16 | build 通过、dev 路由全部 200、代理连通；浏览器走查待用户验收 |
| 7 | 书架+近期+个人 | 书架网格、近期阅读进度条、个人中心（资料/生词/收藏页）、空态 | 桌面+手机各过一遍 | 🟢 已完成 | 2026-08-16 | build 通过、6 路由 200、书架数据链路通；浏览器视觉走查待用户验收 |
| 8 | 阅读页 v1 | 双语双栏、hover 双向高亮、同步滚动、进度上报、翻译开关、纸张视觉 | 悬停联动、滚动不抖动不回弹、刷新进度保留 | 🟢 已完成 | 2026-08-16 | build 通过、/reading 200、载荷完整（13 句/进度 38%）；交互体验待用户浏览器验收 |
| 9 | 阅读页 v2 | 句子/单词气泡、收藏/生词乐观更新、TTS（整篇+单句+单词+语速） | 逐项人工体验 | 🟢 已完成 | 2026-08-16 | build 通过；气泡完整内容需步骤 10 生成标注后统一浏览器验收 |
| 10 | AI 生成后端 | DeepSeekClient、AiGenerationService（分批/校验/重试/状态机/取消/互斥）、gen-status、单句重试 | 示例文章全量生成成功、失败重试、重复生成 409 | 🟢 已完成 | 2026-08-16 | 真实 API 验证：10001 全 13 句生成、10002 2 句、互斥 409、单句重试、取消、reading 载荷带完整标注；修复 reasoning 模型 token 截断（batch=3+max_tokens 8000）、批内复查过滤 bug、超时配置 |
| 11 | 管理后台前端 | 文章列表/新建/编辑（重切分确认）/上下架、生成对话框（轮询+取消+预估成本）、失败标记+单句重试、stats | 建文→生成→上架→书架可见全流程 | 🟢 已完成 | 2026-08-16 | 全流程 API 验证通过（建文→生成 8s→上架→书架可见→stats）；后端补 /admin/stats 与管理端详情接口；4 个后台路由 200 |
| 12 | 移动端+打磨 | Tab/上下对照模式、工具栏紧凑化、加载/错误/空态全铺、双主题全量走查、性能检查 | 手机尺寸全流程无报错；双主题无样式错乱 | 🟢 已完成 | 2026-08-16 | 移动端 Tab/对照模式、EP 暗色同步、gzip 压缩（载荷 5.6KB）、段落 content-visibility、EP chunk 分包；浏览器双主题/移动端走查待用户验收 |
| 13 | 章节数据层 | doc/00-design.md §1 → schema.sql（chapter 表 + sentence.chapter_id）→ Chapter 实体/Mapper → 本地库迁移 | schema 幂等、列存在、存量 chapter_id 全 NULL | 🟢 已完成 | 2026-08-16 | 迭代①；schema 已建、迁移已验证（存量 501 句 chapter_id 全 NULL）、编译通过 |
| 14 | 章节后端 | DTO（ArticleRequest.chapters / ChapterDto / ReadingPayload.chapters / ArticleDetailDto / SentenceDto.chapterId）→ ArticleServiceImpl（normalizeChapters/joinChapters/splitAndStore/reSplit 级联 chapter/chapterize/updateChapterTitlesOnly/readingPayload/detail）→ controller.detail 走 service | curl 全套：建文带/不带 chapters、重切分、仅改标题不重切分、reading 载荷恒非空、删除级联、旧文章合成单章 | 🟢 已完成 | 2026-08-16 | 迭代①；9 项 curl 验收全过（含 chapterize 迁移不删标注/进度、旧文章合成单章 id=null）；10009 已由 chapterize 补章节行 |
| 15 | 管理端章节编辑器 | api 类型 → 编辑器章节卡片（标题+正文+增删/上下移，≥1 章）+ 回显回退 + payload 带 chapters + 重切分弹窗比较（仅标题变化不弹） | 建分章节文章回显正确、弹窗触发矩阵、旧文章单章回退保存不弹窗且完成 chapterize | 🟢 已完成 | 2026-08-16 | 迭代①；前端类型+build 通过、后端链路已由步骤 14 覆盖（回显/弹窗/迁移行为 API 级验证），浏览器交互走查待用户 |
| 16 | 阅读页章节 | store chapterGroups → SentencePane 章渲染（(chapterId,para) 分组、章标题双栏对称）→ ChapterToc（aside/dropdown）→ ReadingView 布局/跳转/高亮/prev-next → reading.css | 桌面目录跳转+高亮、移动端折叠目录、单章兼容、同步滚动无回弹回归、TTS 定位回归、双主题+<1024px 走查、npm build | 🟢 已完成 | 2026-08-16 | 迭代①；vue-tsc + build 通过（Vue3 template v-for key 语法修正）；浏览器交互走查待用户 |
| 17 | 单词音标 | AiGenerationService prompt+校验（words 加 phonetic）→ FavoriteItem/VocabItem 带出 → 前端四处展示（SentenceBubble/WordBubble/VocabView/FavoritesView 收藏句单词列表） | 新生成文章四处显示音标；存量不显示且无布局异常；双主题+移动端走查 | 🟢 已完成 | 2026-08-16 | 迭代②；真实 API 验证：新生成 9/9 词带音标（/ˈbɪli/ 等）、收藏/生词列表带出 phonetic、存量标注 None 不显示；build 通过 |

状态图例：🔵 进行中 · 🟢 已完成 · ⚪ 未开始 · 🔴 阻塞/需用户决策

## 迭代需求记录（2026-08-16）

**迭代① 文章分章节**
- 管理后台按章节编辑（章节标题 + 各章正文，增删/上下移排序）
- 阅读页展示章节名称、目录（桌面左侧固定，移动端顶部折叠）、上/下一章、当前章高亮
- 存量无章节文章：阅读页视为单章（标题=文章标题），目录恒可用
- 数据模型：新表 `chapter`（article_id/seq/title/content_en）+ `sentence.chapter_id`（NULL=旧数据）；`article.content_en` 保留拼接全文；章内 para 段落号 0 起
- 重切分语义不变：正文/章节增删排序变化 → 重切分（清标注+进度，确认弹窗）；仅章标题变化 → 不重切分

**迭代② 单词音标**
- AI 生成标注 words JSON 增加 phonetic（IPA），校验宽容（缺失不拒）
- 展示位置：句子讲解弹窗单词列表、WordBubble、生词本卡片、例句收藏（新增可收起的句子单词列表）
- 仅新生成标注带音标，存量不重新生成（无音标不显示）

## 已知待办/决策记录

- ~~步骤 10 需要用户提供 DeepSeek API Key~~（已解决：AI 配置现可于管理后台「AI 配置」页填写 apiKey/baseUrl/model，环境变量仅作初始默认值）
- ~~数据库账号密码需要用户提供~~（已解决：application-local.yml 已配置，已被 gitignore）
