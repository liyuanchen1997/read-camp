# 前端说明文档（frontend/doc/）

> 技术设计基线见根目录 [doc/00-design.md](../doc/00-design.md)，UI 规范见 [doc/01-ui-design.md](../doc/01-ui-design.md)，开发进度见 [doc/02-project-plan.md](../doc/02-project-plan.md)。

## 1. 架构总览

```
用户操作 → views/ 页面组件
         → composables/（气泡、同步滚动、进度上报）
         → api/（axios 实例：token 注入、401 处理、Result 解包）
         → Vite proxy /api → 后端 8080
stores/（pinia）：user（登录态）、theme（双主题）、reading（阅读页共享状态）
services/：tts.ts（speechSynthesis 控制器，整篇/单句/单词朗读）
```

## 2. 页面清单（步骤 6-12 落地）

| 路由 | 页面 | 模块 | 步骤 |
|---|---|---|---|
| /login /register | 登录/注册 | 认证 | 6 |
| /（书架） | 书架（**公开浏览，无需登录**） | 文章展览 | 7 |
| /recent /vocab /favorites | 近期阅读/生词本/收藏 | 学习数据 | 7 |
| /profile | 个人中心（含后台入口） | 用户 | 7 |
| /reading/:id | 阅读页（核心） | 双语对照 | 8-9 |
| /admin/articles /admin/articles/new /admin/articles/:id/edit /admin/ai-config /admin/stats | 管理后台（**仅桌面端**） | 管理 | 11 |

## 3. 双主题机制

- `tokens.css`：`:root` 亮色 + `[data-theme='dark']` 暗黑，CSS 变量驱动
- 初始化：main.ts 顶部读 localStorage（`readcamp-theme`），无则跟随系统，防首屏闪烁
- 切换：stores/theme.ts `toggle()/set()/followSystem()`
- Element Plus 暗色：main.ts 引入 `element-plus/theme-chalk/dark/css-vars.css` + `document.documentElement` 加 `dark` class（管理端适配，步骤 11）

## 4. 阅读页（步骤 8-9 核心，见 doc/00-design.md §4）

- `GET /articles/{id}/reading` 一次拉全 → 按索引对齐双栏渲染
- hover 高亮：readingStore.hoveredIndex 状态驱动双向同步
- 同步滚动：预存 offsetTop + 二分锚句 + 瞬时 scrollIntoView + 互斥锁 + 主导源判定
- SentenceBubble/WordBubble：Teleport + fixed 定位，自动翻转，乐观更新收藏/生词；单词含音标（AI 标注 phonetic，可选）
- TTS：语音候选**女声优先**（持久化选择 → en-US 女声 → en-* 女声 → en-US → en-* → 默认）；整篇顺序朗读 + 当前句高亮；pause 不可靠用重播；语速持久化
- 进度：IntersectionObserver(threshold 0.2) + 3s/30条/pagehide 防抖上报
- 章节：readingStore chapterGroups 按 chapterId 分组，章标题双栏对称渲染；ChapterToc 目录（桌面左侧固定 + 移动端折叠下拉）+ 上一章/下一章

## 5. 移动端（<1024px）

- Tab 模式（英文/中文切换，单 pane）默认；上下对照模式可选；工具栏含"目录"按钮（折叠下拉）
- tap=click 开气泡；底部安全区适配

## 6. 变更记录

| 日期 | 变更 |
|---|---|
| 2026-08-16 | 骨架：Vite 脚手架、tokens.css/base.css、router、user/theme store、api/request.ts、HomeView 占位 |
| 2026-08-16 | 认证/书架/阅读页/气泡/TTS/双主题（步骤 6-12）、管理后台（步骤 11）、AI 配置页、生成弹窗模型名动态化、段落间距 |
