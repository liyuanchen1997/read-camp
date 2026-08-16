# CLAUDE.md — 前端项目（frontend/）

## 技术栈

Vue 3 + TypeScript + Vite + Pinia + Vue Router + Axios + Element Plus（**仅管理后台**）

## 命令

```bash
npm install        # 安装依赖
npm run dev        # 开发服务（5173，/api 代理到 8080）
npm run build      # 类型检查 + 构建（vue-tsc -b && vite build）
npm run preview    # 预览构建产物
```

## 目录约定

```
src/
├── api/          # request.ts（axios 实例）+ 各模块接口
├── stores/       # pinia：user.ts / theme.ts / reading.ts（阅读页状态）
├── router/       # index.ts + guard.ts（登录/admin 守卫，步骤 6 落地）
├── views/        # 页面（Login/Shelf/Reading/Profile/... admin/）
├── components/   # reading/（阅读页专属）common/（通用）admin/
├── composables/  # 气泡、同步滚动、进度上报等逻辑
├── services/     # tts.ts（朗读控制器）等
├── styles/       # tokens.css（双主题变量，唯一颜色来源）/ base.css / reading.css
└── types/        # api.ts（与后端 DTO 对齐）
```

## 铁律

1. **阅读区零 Element Plus 依赖**：EP 只允许出现在管理后台（路由级懒加载隔离）。阅读页组件（双语对照/气泡/进度条）全部手写。
2. **颜色一律走 tokens.css 变量**（doc/01-ui-design.md §2），禁止写死色值；新增组件先查 token 是否已有。
3. **双主题必检**：任何样式改动后，亮色 + 暗黑两种主题都要验证。
4. **响应式必检**：桌面（≥1024px 双栏）+ 移动端（<1024px Tab 模式）两种尺寸都要验证。
5. 接口调用必须走 `src/api/` 模块（统一 token 注入与 401 处理），不在组件里直接 new axios。
6. 全局细节以根目录 CLAUDE.md / AGENTS.md 为准（分步开发、每步暂停、changelog 记录）。

## 关键实现说明（步骤 8-9 细化）

- 双语对照：`GET /articles/{id}/reading` 一次拉全，按索引对齐 enList/zhList，零 DOM 测量
- 同步滚动：锚句二分 + 瞬时 scrollIntoView + 互斥锁 + 主导源判定（见 doc/00-design.md §4）
- 气泡：SentenceBubble / WordBubble，fixed 定位自动翻转，同屏单实例
- TTS：services/tts.ts 单例，语音候选女声优先（持久化选择 → en-US 女声 → en-* 女声 → en-US → en-* → 默认），pause 不可靠用重播
- 进度：IntersectionObserver + 防抖批量上报
