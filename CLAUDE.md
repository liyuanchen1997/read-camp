# CLAUDE.md — 项目说明

## 项目

英语精读训练网站（Intensive Reading Studio）。双语对照精读、句级/词级 AI 讲解、TTS 朗读、生词本、例句收藏、进度统计、管理后台按需生成标注。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + TypeScript + Vite + Pinia + Vue Router + Axios + Element Plus（仅管理后台） |
| 后端 | Spring Boot 3 + MyBatis-Plus + MySQL 8 + JWT（HandlerInterceptor 轻量鉴权） |
| AI | DeepSeek（OpenAI 兼容接口，模型 deepseek-v4-flash），管理员点击才生成，结果入库 |
| 发音 | 浏览器原生 speechSynthesis（免费） |

## 本地开发命令

```bash
# 前端（5173 端口，代理 /api → 8080）
cd frontend && npm run dev

# 后端（8080 端口）
cd backend && /Users/liyuanchen/Desktop/code/utils/apache-maven-3.9.9/bin/mvn spring-boot:run

# 后端编译检查
cd backend && /Users/liyuanchen/Desktop/code/utils/apache-maven-3.9.9/bin/mvn -q compile
```

Maven 在本机未加入 PATH，路径：`/Users/liyuanchen/Desktop/code/utils/apache-maven-3.9.9/bin/mvn`。

## 文档索引

| 文档 | 内容 |
|---|---|
| [doc/00-design.md](doc/00-design.md) | 技术架构设计（数据库/API/AI 生成/阅读页架构/移动端） |
| [doc/01-ui-design.md](doc/01-ui-design.md) | UI 设计（设计语言/token/配色/字体/页面线框/交互规范） |
| [doc/02-project-plan.md](doc/02-project-plan.md) | **项目计划表（含当前进度，新会话先看这里）** |
| [doc/changelog.md](doc/changelog.md) | 开发变更记录（追加式） |

## 开发流程规则（重要）

1. **分步开发**：按 `doc/02-project-plan.md` 的步骤表执行，每步一个模块。**每完成一步必须暂停，等待用户指令后才能进入下一步。**
2. 每步完成后：更新计划表状态与日期 → 追加 changelog → 展示验收结果。
3. 新会话/子代理接手前：先读 `doc/02-project-plan.md` 了解当前进度，再读对应模块设计文档。
4. 不自动 git commit/push（除非用户明确要求）；提交前展示变更摘要。

## 目录约定

```
read-camp/
├── AGENTS.md  CLAUDE.md  README.md
├── doc/                    # 设计文档 + 计划表 + 变更记录
├── frontend/
│   └── src/
│       ├── api/            # axios 实例 + 各模块接口
│       ├── stores/         # pinia（user / reading / theme）
│       ├── router/         # 路由 + 守卫
│       ├── views/          # 页面（含 admin/）
│       ├── components/     # reading/ common/ admin/
│       ├── composables/    # 气泡、同步滚动、进度上报等
│       ├── services/       # tts.ts 等
│       ├── styles/         # tokens.css / base.css / reading.css
│       └── types/          # 与后端 DTO 对齐的类型
└── backend/
    └── src/main/
        ├── java/com/readcamp/   # config/ common/ controller/ service/ mapper/ entity/ dto/
        └── resources/
            ├── application.yml
            ├── mapper/*.xml
            └── db/schema.sql  seed.sql
```

## 预置账号

- 管理员：建库脚本预置（见 `backend/src/main/resources/db/seed.sql`），首登强制改密
- 数据库初始化见 `doc/00-design.md` §1
