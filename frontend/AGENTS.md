# AGENTS.md — 前端开发规则

在根目录 AGENTS.md 铁律之上，前端专属补充：

## 必守

1. **阅读区零 Element Plus**：EP 仅限管理后台。阅读页/组件手写样式，走 tokens.css 变量。
2. **双主题 + 双尺寸验收**：样式改动后必须在亮/暗主题、桌面/移动端各验证一次（`npm run build` + 浏览器走查）。
3. **不写死颜色/尺寸**：一律用 tokens.css / 间距阶梯变量；新增 token 先更新 doc/01-ui-design.md。
4. **接口统一走 api/ 模块**：禁止组件内裸 axios；类型定义放 types/api.ts 与后端 DTO 对齐。
5. **状态管理**：跨组件共享状态（登录态、主题、阅读页 hoveredIndex/气泡）放 pinia store，禁止组件间 eventBus 式传参。
6. 路由：新页面在 router/index.ts 注册并声明 meta（requiresAuth / requiresAdmin），守卫逻辑见步骤 6 的 guard.ts。

## 完成自检

- [ ] `npm run build`（含 vue-tsc 类型检查）通过
- [ ] 亮/暗主题无样式错乱
- [ ] 桌面 + 移动端关键路径走查
- [ ] 更新 frontend/doc/ 与根 doc/changelog.md
- [ ] 提交前对照 diff 检查文档滞后（README/doc/01-ui-design.md/根 doc/**），滞后先更新再提交
