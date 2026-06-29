# 系统架构

## 请求链路

1. 前端携带 JWT 访问 Spring Boot。
2. `JwtAuthenticationFilter` 建立用户身份。
3. Redis 限流器保护登录、聊天和分析入口。
4. `RiskDetectionService` 先执行不可绕过的确定性风险扫描。
5. 高风险消息进入 `CrisisWorkflowService`；普通消息进入 Agent 或 Message Router。
6. Agent 使用 `ToolRegistry` 调用 RAG、记忆、Dashboard、风险扫描和策略推荐。
7. `LlmService` 统一管理模型调用、重试和熔断。

## 数据层

- MySQL：用户、报告、知识原文、会话、长期记忆、画像、角色卡、告警事件。
- Redis：RAG/意图缓存、分布式锁、限流、最近 12 条会话热记忆、风险告警 Stream。
- Chroma：知识 Chunk 向量索引。
- Ollama：Chat 与 Embedding 模型。

## 安全边界

- 危机回复不由角色卡或自由生成覆盖。
- 用户只能访问自己的会话；画像、知识库、记忆中心和调试页仅管理员访问。
- 普通用户 Dashboard 不返回全局高风险用户列表。
- 配置密钥从环境变量读取，不进入版本库。

## 可用性

- 风险告警先落 MySQL，再进入 Redis Stream；Redis 故障时回退到本地异步分发，未完成事件由定时任务恢复。
- LLM 调用失败时重试一次，连续失败触发短时熔断。
- `/actuator/health` 和 `/actuator/info` 可匿名读取，详细指标仅管理员访问。
