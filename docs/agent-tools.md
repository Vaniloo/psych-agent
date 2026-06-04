# Agent Tool Bridge

当前 backend 通过 `ToolRegistry` 统一注册和发现 agent 可调用能力，并同时保留了兼容旧链路的 `/tools` 接口。

## Endpoints

- `GET /tools`
  返回所有已注册工具的名称、描述和参数 Schema。
- `GET /tools/{toolName}`
  返回单个工具的元信息，适合外部 agent 做按需发现。
- `POST /tools/call`
  兼容旧调用方式，请求体格式为 `{ "tool": "...", "arguments": { ... } }`。
- `POST /tools/{toolName}`
  新的按名称调用方式，请求体直接传参数对象。

所有调用都会返回统一结构：

```json
{
  "tool": "recommend_strategy",
  "success": true,
  "result": {},
  "executedAt": "2026-06-04T10:00:00"
}
```

## Registered Tools

- `search_knowledge`
  在心理知识库中搜索相关内容，支持分类过滤和数量限制。
- `get_dashboard`
  获取指定用户的近期心理报告与全局高风险用户统计。
- `risk_scan`
  对单条消息做无副作用的风险扫描，返回 `risk / emotion / confidence`。
- `get_user_memory`
  获取用户的中长期记忆摘要、画像信息，以及可选的最近会话概览。
- `recommend_strategy`
  综合风险扫描、用户画像和知识检索结果，生成一轮对话的陪伴/干预策略建议。

## Design Notes

- 工具调用前会根据声明的参数 Schema 做必填项和基础类型校验。
- `risk_scan` 只做分析，不落库、不触发正式告警，适合外部 agent 的预判场景。
- 正式风险报告写入、邮件通知和高风险兜底仍由原有消息主链负责。
