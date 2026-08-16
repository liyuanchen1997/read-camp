# AGENTS.md — 后端开发规则

在根目录 AGENTS.md 铁律之上，后端专属补充：

## 必守

1. **Result + ApiException**：接口返回统一 `Result<T>`；业务异常抛 `ApiException(httpStatus, code, message)`（含 unauthorized/forbidden/notFound/conflict 快捷方法），禁止返回裸异常。
2. **鉴权分层**：AuthInterceptor 解析 JWT 写 ThreadLocal（UserContext）；AdminInterceptor 校验管理员。Controller 从 UserContext 取当前用户，禁止从请求参数取 userId。
3. **AI 生成可靠性**：状态只信任 DB；异步任务与请求生命周期解耦；同文章并发生成必须互斥（409）；批间检查取消标记；失败句可单句重试（gen_status 3→1→2）。
4. **SQL 安全**：MyBatis-Plus 条件构造器/LambdaQueryWrapper 防注入；自定义 SQL 用 #{} 占位符，禁止 ${} 拼接。
5. **敏感字段**：接口响应/日志禁止输出 password；密码 BCrypt 存储。
6. 编译与测试：`mvn -q compile` 必须通过；切分器等纯逻辑写单元测试（src/test）。

## 完成自检

- [ ] `mvn -q compile` 通过
- [ ] 新增接口 curl 走查（成功路径 + 401/403/参数错误路径）
- [ ] 涉及 AI 生成的状态机：重复调用 409、失败可重试、页面关闭任务继续
- [ ] 更新 backend/doc/ 与根 doc/changelog.md
- [ ] 提交前对照 diff 检查文档滞后（接口/表结构变更同步 doc/00-design.md，滞后先更新再提交）
