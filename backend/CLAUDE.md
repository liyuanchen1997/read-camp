# CLAUDE.md — 后端项目（backend/）

## 技术栈

Spring Boot 3.4 + Java 21 + MyBatis-Plus 3.5.7 + MySQL 8 + JWT（jjwt）+ spring-security-crypto（仅 BCrypt）+ Lombok

## 命令

```bash
# Maven 未加入 PATH，使用完整路径
/Users/liyuanchen/Desktop/code/utils/apache-maven-3.9.9/bin/mvn -q compile
/Users/liyuanchen/Desktop/code/utils/apache-maven-3.9.9/bin/mvn spring-boot:run
```

## 分层约定（com.readcamp）

```
config/     # WebConfig(拦截器) MybatisPlusConfig(分页) AsyncConfig(线程池)
common/     # Result 统一包装 / ApiException / GlobalExceptionHandler
controller/ # 控制器，只做参数校验与调用 service
service/    # 业务逻辑 + impl/ + ai/（DeepSeekClient/AiGenerationService/SentenceSplitter/GenTaskRegistry）
mapper/     # MyBatis-Plus Mapper（复杂 SQL 用 resources/mapper/*.xml）
entity/     # 与表一一对应的实体（@TableName/@TableField 对齐下划线列名）
dto/        # 请求/响应 DTO
```

## 铁律

1. **统一 Result 包装**：所有接口返回 `Result<T>`；业务异常抛 `ApiException`（带 HTTP 语义），禁止 controller 里 try-catch 吞异常。
2. **鉴权**：JWT 解析走 HandlerInterceptor（AuthInterceptor → ThreadLocal UserContext；AdminInterceptor 校验 role==1）。不引入完整 Spring Security。
3. **密钥零硬编码**：JWT secret / DeepSeek key / DB 密码全部 `${ENV_VAR:默认值}` 形式放 application.yml，禁止写死在代码里。
4. **AI 生成状态机**：gen_status 0未生成/1生成中/2已生成/3生成失败，**DB 是唯一事实源**；任务在异步线程池执行，与 HTTP 请求生命周期解耦（页面关闭不中断）；任务互斥（同文章同时仅一个任务，否则 409）。
5. **表结构变更**：先更新根 doc/00-design.md，再改 `resources/db/schema.sql`，再写代码。
6. 配置项（batchSize、超时、model）走 application.yml，不写死在代码里。
7. 全局细节以根目录 CLAUDE.md / AGENTS.md 为准（分步开发、每步暂停、changelog 记录）。

## 本地数据库

- 本机 MySQL：/usr/local/mysql（root，密码见用户提供）
- 初始化：`CREATE DATABASE readcamp` → 执行 `resources/db/schema.sql` → `resources/db/seed.sql`（步骤 2 交付）
- 本地连接可在 `application-local.yml` 覆盖（已被 gitignore）

## 关键模块说明

- SentenceSplitter：上传时切分句子（`.` `!` `?` 句界、缩写白名单、数字保护、引号归属），doc/00-design.md §1
- AiGenerationService：分批调用 DeepSeek、JSON 三层解析防护、批级重试 2 次、坏项单补、逐句状态落库，doc/00-design.md §3
- 阅读载荷 `GET /articles/{id}/reading`：一次拉全（元信息+句子+标注+进度+生词集合+收藏集合）
