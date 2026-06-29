# 测试报告

默认单元测试覆盖：

- 明确高风险表达识别
- 普通焦虑不误判为高风险
- 危机回复包含 12356 / 120 / 110 资源
- 工具参数校验与结构化执行结果
- 策略推荐在高风险输入时绕过模型扫描

运行：

```bash
cd backend
./mvnw test
```

`PsychAgentApplicationTests` 是外部服务集成测试，需要 MySQL、Redis、Chroma、Ollama，默认标记为 Disabled。

当前结果：8 个测试通过，1 个外部依赖集成测试跳过。

本次回归还包含：

- `node --check frontend/app.js` 与 `frontend/config.js`
- `docker compose config --quiet`
- 1440 x 1000 桌面端与 390 x 844 移动端页面检查，无横向溢出
- MySQL `EXPLAIN` 验证两条报告查询分别命中对应复合索引

本机实启动已验证 MySQL 与 Ollama 配置；Chroma 未运行时，后端会在向量库初始化阶段明确停止并输出连接错误。
