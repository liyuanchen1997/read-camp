# AGENTS.md — 给 AI 代理的开发规则

本文件约束所有参与本仓库开发的 AI 代理（Claude Code 及子代理）。违反规则的行为会被拒绝。

## 项目一句话

英语精读训练网站：Vue3+TS 前端 + Spring Boot 3 后端 + MySQL 8，AI（OpenAI 兼容，默认 DeepSeek，可配置切换）按需生成句级精读标注。

## 铁律

1. **每步一个模块，完成后必须暂停**：分步开发计划见 `doc/02-project-plan.md`。每完成一步，更新计划表状态 + 写 changelog，然后等待用户指令，**不得擅自进入下一步**。
2. **文档同步**：任何涉及模块/接口/表结构的变更，必须先更新对应设计文档（`doc/00-design.md` 技术设计 / `doc/01-ui-design.md` UI 设计），再写代码；完成后在 `doc/changelog.md` 追加记录（步骤/日期/变更/验收结果）。
3. **不越权改表结构**：表结构变更必须先在 `doc/00-design.md` 评审更新，再改 `backend/src/main/resources/db/schema.sql`。
4. **密钥禁止硬编码**：AI API Key（DEEPSEEK_API_KEY 环境变量 / `ai_config` 表，yml 仅初始默认值）、JWT secret、数据库密码一律禁止写死在代码或文档里。
5. **阅读区禁用 Element Plus**：`Element Plus` 仅允许用于管理后台与表单；阅读页组件零 EP 依赖（路由级懒加载隔离）。
6. **不自动 git commit/push**：除非用户明确要求。提交前必须展示变更摘要，commit message 用简洁英文附带简洁中文。
7. **红线操作先问**：删除文件/目录、修改 .env/密钥/CI 配置、git push/rebase/reset --hard、公开发布——即使在 auto-accept 模式也必须先征求用户确认。
8. **提交前文档同步（强制）**：每次 git commit/push 前必须先检查文档滞后：对照 diff 检查 README、CLAUDE.md、AGENTS.md、doc/**、frontend/doc/**、backend/doc/**；新增/变更模块、接口、表结构、配置、UI 必须同步更新对应设计文档与 changelog；确认无滞后后才允许提交。

## 技术约定

- 前端：Vue3 + TS + Vite + Pinia + Vue Router + Axios；状态用 pinia store，请求走 `src/api/` 模块。
- 后端：Spring Boot 3 + MyBatis-Plus + MySQL 8；统一 `Result<T>` 包装；JWT 走 HandlerInterceptor（非完整 Spring Security）。
- AI 生成状态机：`gen_status` 0未生成/1生成中/2已生成/3生成失败，DB 为唯一事实源，任务与请求生命周期解耦。
- 响应式：PC（≥1024px 双语双栏）+ 移动端（Tab 模式 / 上下对照），断点遵守 `doc/01-ui-design.md`。
- 双主题：亮色/暗黑，CSS 变量 tokens（`frontend/src/styles/tokens.css`），禁止写死颜色值。

## 验收前自检

- 后端改动：`mvn -q compile` 通过；相关接口 curl 走查通过。
- 前端改动：`npm run build`（vue-tsc + vite build）通过；浏览器关键路径人工走查。
- 样式改动：亮/暗双主题 + 桌面/移动两种尺寸都检查。
- 完成后：更新 `doc/02-project-plan.md`（状态+日期）与 `doc/changelog.md`。
