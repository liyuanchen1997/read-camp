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
