# 接口说明

本文档基于当前 `main` 分支源码中的 Controller 编写，描述的是实际存在的接口。

## 1. 认证

### `POST /auth/register`

请求体：

```json
{
  "username": "alice",
  "password": "123456"
}
```

### `POST /auth/login`

请求体：

```json
{
  "username": "alice",
  "password": "123456"
}
```

返回值包含登录态信息，前端通常需要保存 JWT。

## 2. 普通聊天与风险分析

### `POST /chat`

说明：

- 当前接口直接接收原始字符串请求体
- 不要求登录

示例：

```text
最近总是睡不好
```

### `POST /chat/analyze`

说明：

- 需要登录
- 会对消息做心理风险分析并写入报告

## 3. 消息路由

### `POST /message`

说明：

- 需要登录
- 请求体为 `MessageRequest`
- 后端会执行：
  - 意图分类
  - 风险分析
  - 普通聊天或 RAG 回复
  - 记忆更新

请求体：

```json
{
  "message": "我最近很焦虑，总觉得喘不过气"
}
```

## 4. Agent 对话

### `POST /agent/chat`

说明：

- 需要登录
- 请求体为 `AgentChatRequest`
- 后端会根据用户消息判断是否调用工具，再生成最终回复

常见字段：

- `message`
- `sessionId`

## 5. 会话与记忆

### `GET /conversations`

- 需要登录
- 获取当前用户会话列表

### `GET /conversations/{sessionId}/messages`

- 需要登录
- 获取指定会话消息列表

### `GET /admin/memories`

- 需要管理员权限
- 获取所有用户记忆摘要

### `GET /admin/memories/{username}`

- 需要管理员权限
- 获取指定用户记忆详情

### `GET /admin/memories/{username}/sessions/{sessionId}/messages`

- 需要管理员权限
- 获取指定用户某会话的消息

## 6. 用户画像

### `GET /profile`

- 需要管理员权限
- 获取全部画像

### `GET /profile/{username}`

- 需要管理员权限
- 获取指定用户画像

### `PUT /profile/{username}`

- 需要管理员权限
- 更新指定用户画像

## 7. 知识库

### `POST /knowledge/add`

- 需要管理员权限
- 添加单条知识

### `GET /knowledge/search`

- 需要管理员权限
- 按 `query` 搜索知识
- 支持 `category` 和 `limit`

### `DELETE /knowledge/{id}`

- 需要管理员权限
- 删除知识文档

### `GET /knowledge/all`

- 需要管理员权限
- 获取所有知识文档

### `GET /knowledge/page`

- 需要管理员权限
- 分页获取知识文档

### `POST /knowledge/import`

- 需要管理员权限
- 导入文档

## 8. 报告

### `GET /reports/my`

- 需要登录
- 获取当前用户自己的报告

### `GET /reports/dashboard`

- 需要登录
- 获取当前用户最近报告和高风险统计

支持参数：

- `recentLimit`
- `topRiskLimit`

### `GET /reports`

- 需要管理员权限
- 获取全部报告

### `GET /reports/page`

- 需要管理员权限
- 报告分页查询

### `GET /reports/{id}`

- 需要管理员权限
- 获取单条报告

### `GET /reports/top-risk-users`

- 需要管理员权限
- 获取高风险用户统计

## 9. 工具接口

### `GET /tools`

说明：

- 获取当前注册的工具定义
- 返回工具名、描述、参数信息

### `POST /tools/call`

请求体：

```json
{
  "tool": "search_knowledge",
  "arguments": {
    "query": "焦虑时怎么缓解",
    "category": "anxiety",
    "limit": 3
  }
}
```

当前返回的是统一工具执行结果对象。
