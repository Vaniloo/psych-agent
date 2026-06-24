# 数据库说明

当前代码中的持久化主要由 MySQL 承担，Redis 用于缓存和锁相关逻辑。

## MySQL 中的主要实体

从 `entity/` 目录可以看到当前核心实体包括：

- `User`
- `UserProfile`
- `PsychologicalReport`
- `KnowledgeDocument`
- `ConversationSession`
- `ConversationMessage`
- `ConversationMemory`

## 主要用途

### `User`

- 用户注册与登录
- 角色区分

### `UserProfile`

- 用户画像摘要
- 困扰、偏好、支持目标等长期信息

### `PsychologicalReport`

- 心理分析结果
- 风险等级
- 情绪标签
- 置信度
- 原始消息

### `KnowledgeDocument`

- 知识库文档元信息

### `ConversationSession`

- 会话标题
- 会话摘要
- 会话创建和更新时间

### `ConversationMessage`

- 某会话中的逐条消息

### `ConversationMemory`

- 中长期记忆摘要

## Redis 的当前用途

从服务命名可以看出，Redis 主要服务于：

- `KnowledgeLockService`
- `RiskAlertLockService`
- `UserMessageLockService`

当前代码意图上主要用于：

- 防止重复高风险告警
- 防止重复消息频繁提交
- 知识导入等场景的并发控制

## 当前说明

- JPA `ddl-auto` 当前配置为 `update`
- 本地开发时会根据实体自动更新表结构
- 完整运行需要可用 MySQL
