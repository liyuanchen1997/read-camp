# ReadCamp · 英语精读训练网站

一间安静的线上阅览室：双语对照精读英文文章，句子悬停高亮、点击即得 AI 讲解（句子成分、逐词释义），内置浏览器 TTS 朗读与整篇发音，生词本、例句收藏、阅读进度一目了然；管理后台上传文章后按需生成精读标注并上架。

## 功能总览

- **双语精读**：英文/中文左右对照（PC）、逐句同步滚动、双向悬停高亮；整篇中文翻译一键开关
- **句级/词级讲解**：点击句子弹出气泡 —— 句子解释、中文意思、句子成分、每个单词的词性与作用；单词可单独查看、发音、加入生词本
- **发音**：整篇顺序朗读（当前句高亮跟随）、单句/单词发音，浏览器原生 TTS，免费
- **学习数据**：生词本、例句收藏、近期阅读（进度条）、精读文章数与总进度统计
- **管理后台**：文章上传/编辑/上架下架；点击按钮按需调用 DeepSeek 生成句子标注（生成中/成功/失败状态可视化，失败可单句重试，页面关闭不中断）
- **体验**：亮色/暗黑双主题、PC + 移动端响应式

## 技术栈

Vue 3 + TypeScript + Vite + Pinia + Element Plus（管理端）｜Spring Boot 3 + MyBatis-Plus + MySQL 8 + JWT｜DeepSeek（deepseek-v4-flash）

## 快速启动

### 1. 初始化数据库

```bash
# 前置：本地 MySQL 8 运行中，创建数据库 readcamp
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS readcamp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p readcamp < backend/src/main/resources/db/schema.sql
mysql -uroot -p readcamp < backend/src/main/resources/db/seed.sql
```

### 2. 配置后端环境变量

在 `backend` 目录创建 `application-local.yml`（已被 gitignore），或直接导出环境变量：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/readcamp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: <你的密码>
readcamp:
  jwt:
    secret: <32 字节以上随机字符串>
  ai:
    api-key: <DeepSeek API Key>
```

### 3. 启动后端（8080）

```bash
cd backend
/Users/liyuanchen/Desktop/code/utils/apache-maven-3.9.9/bin/mvn spring-boot:run
```

### 4. 启动前端（5173，/api 自动代理到 8080）

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173

## 预置管理员

数据库初始化时预置管理员账号（见 `db/seed.sql`），**首次登录需强制修改密码**。更多管理员由管理员在后台添加。

## 文档

- 技术设计：[doc/00-design.md](doc/00-design.md)
- UI 设计：[doc/01-ui-design.md](doc/01-ui-design.md)
- 项目计划与进度：[doc/02-project-plan.md](doc/02-project-plan.md)
- 开发变更记录：[doc/changelog.md](doc/changelog.md)

## 状态

开发中（分步进行，每步完成后暂停等待验收）。
