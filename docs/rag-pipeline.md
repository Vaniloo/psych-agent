# RAG 流程说明

当前代码已经实现了一个以 `KnowledgeService` 为核心的知识检索链路。

## 入口

当前与 RAG 相关的主要入口包括：

- `GET /knowledge/search`
- `POST /message`
- `POST /agent/chat`
- 工具 `search_knowledge`
- 工具 `recommend_strategy`

## 主要流程

### 1. 知识导入

通过：

- `POST /knowledge/add`
- `POST /knowledge/import`

进入知识库服务。

### 2. 文本切分

由 `TextChunkService` 负责处理长文本切分。

### 3. 向量存储

后端使用 Spring AI 的 Chroma 向量存储配置。

### 4. 检索与过滤

`KnowledgeService.searchKnowledge(...)` 支持：

- query 检索
- category 过滤
- limit 限制

### 5. 重排

当前代码中与重排相关的服务包括：

- `RerankService`
- `RuleBasedRerankService`
- `FallbackRerankService`
- `LocalModelRerankService`

说明：

- 仓库中已经有重排服务结构
- 是否完全可用仍依赖本地模型与外部服务环境

## 在聊天链路中的使用

### `MessageRouterService`

当消息意图被识别为咨询或风险相关时，会走 `ragChat(...)` 路径。

### `AgentService`

当 Agent 决定调用 `search_knowledge` 工具时，会先执行检索，再基于工具结果生成最终回复。
