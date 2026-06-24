# Agent Tools

本文档只描述当前 `main` 分支源码中已经实现并通过 `ToolRegistry` 注册的工具能力。

## 工具暴露方式

当前 HTTP 层提供两个工具相关接口：

- `GET /tools`
  返回所有已注册工具的元信息
- `POST /tools/call`
  统一执行工具调用

请求格式：

```json
{
  "tool": "search_knowledge",
  "arguments": {
    "query": "焦虑时怎么缓解"
  }
}
```

返回格式：

```json
{
  "tool": "search_knowledge",
  "success": true,
  "result": [],
  "executedAt": "2026-06-24T12:00:00"
}
```

## 工具列表

### 1. `search_knowledge`

作用：

- 在心理知识库中检索相关内容

参数：

- `query: string` 必填
- `category: string` 可选
- `limit: integer` 可选

### 2. `get_dashboard`

作用：

- 获取用户近期心理报告和高风险统计

参数：

- `username: string` 必填
- `recentLimit: integer` 可选
- `topRiskLimit: integer` 可选

说明：

- 在 Agent 场景里，`username` 是由后端根据登录态自动注入的

### 3. `risk_scan`

作用：

- 对一条消息做无副作用风险扫描

参数：

- `message: string` 必填

说明：

- 该工具调用 `PsychologicalService.scan`
- 不写入正式心理报告
- 不触发邮件告警

### 4. `get_user_memory`

作用：

- 获取用户记忆摘要、长期记忆和画像信息

参数：

- `username: string` 必填
- `includeSessions: boolean` 可选
- `sessionLimit: integer` 可选

### 5. `recommend_strategy`

作用：

- 综合风险扫描、用户记忆和知识检索结果，生成结构化陪伴建议

参数：

- `username: string` 必填
- `message: string` 必填
- `category: string` 可选
- `knowledgeLimit: integer` 可选

## 当前实际情况说明

- `ToolCallService` 已经实现了基础参数校验
- `ToolRegistry` 会统一收集所有 `ToolExecutor` 实现
- 当前 `AgentService` 的提示词中明确列出的工具仍主要是：
  - `search_knowledge`
  - `get_dashboard`

这意味着：

- 工具 HTTP 接口已经可以直接调用更多工具
- 但 Agent 自动决策是否会主动使用新工具，还取决于 `AgentService` 的提示词设计
