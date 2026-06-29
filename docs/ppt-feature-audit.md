# PPT 功能承诺审计

来源：`心理支持Agent平台项目汇报.pptx`（19 页）。

## 已实现

- JWT 登录、用户身份识别、USER / ADMIN 权限隔离
- CHAT / CONSULT / RISK 意图分类，情绪、风险和置信度分析
- 确定性高风险处理、结构化危机回复、帮助中心和 12356 / 120 / 110 资源
- 风险报告、持久化告警事件、Redis Stream、失败重试与 Redis 故障回退
- RAG 文档导入、Chunk、Embedding、分类过滤、重排、Redis 缓存和 MySQL 降级
- ToolRegistry、工具发现与执行、Agent 自动选择工具
- `knowledge_search`、`get_dashboard`、`get_user_memory`、`risk_scan`、`recommend_strategy`
- Redis List 短期记忆，MySQL 会话摘要、长期记忆和用户画像
- 历史会话保存、恢复和继续对话
- 管理员全量用户画像、长期记忆与会话摘要可视化
- 预设和自定义角色卡，支持语气、建议方式和禁忌表达；安全约束不可覆盖
- Dashboard 风险分布、30 天情绪趋势和数据库复合索引
- 登录、聊天、角色卡、报告、帮助中心、知识库、记忆中心、配置与工具调试前端
- API 限流、LLM 重试/熔断、Actuator 健康与指标监控
- Docker Compose、Nginx、环境变量和本地启动脚本
- 单元测试及架构、部署、数据库、RAG、工具、隐私、测试文档

## 部分实现

- Provider 切换：服务层已抽象，当前仓库内置 Ollama 实现；OpenAI/第三方适配器仍需对应密钥与依赖。
- 工具透明化：前端展示已调用工具和管理员调试结果，未实现逐 Token 的实时轨迹流。
- 横向扩展：服务无状态部分可多副本部署，仓库提供 Compose，未提供 Kubernetes 清单。

## 规划项

- Rust Skill Server / MCP 技能服务。PPT 将其列为后续扩展，不影响当前 Java 工具链业务闭环。
