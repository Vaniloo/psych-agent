# 测试报告

默认单元测试覆盖：

- 明确高风险表达识别
- 普通焦虑不误判为高风险
- 危机回复包含 12356 / 120 / 110 资源
- 工具参数校验与结构化执行结果
- 策略推荐在高风险输入时绕过模型扫描
- 心理知识初始化失败时不阻断应用启动
- 已删除用户的旧令牌安全降级为未认证请求

运行：

```bash
cd backend
./mvnw test
```

`PsychAgentApplicationTests` 是外部服务集成测试，需要 MySQL、Redis、Chroma、Ollama，默认标记为 Disabled。

当前结果：10 个测试通过，1 个外部依赖集成测试跳过。

本次回归还包含：

- `node --check frontend/app.js` 与 `frontend/config.js`
- `docker compose config --quiet`
- 浏览器登录、退出登录及失效登录态清理
- MySQL `EXPLAIN` 验证两条报告查询分别命中对应复合索引

本机实启动已验证 MySQL、Docker Redis、Docker Chroma 与 Docker Ollama；健康检查为 `UP`，聊天和 RAG 检索均可用。
