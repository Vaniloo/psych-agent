# 架构说明

本文档仅描述当前 `main` 分支源码中已经实现的结构，不包含尚未落地的规划能力。

## 总体结构

项目由四部分组成：

- `frontend/`
  零依赖静态前端页面
- `backend/`
  Spring Boot 核心服务
- `docs/`
  文档
- `scripts/`
  启动和调试脚本

## 后端主要模块

### 1. 鉴权与用户

相关类：

- `AuthController`
- `UserService`
- `CustomUserDetailsService`
- `JwtUtil`
- `JwtAuthenticationFilter`
- `SecurityConfig`

当前实现：

- 注册与登录
- JWT 鉴权
- 基于 Spring Security 的接口放行与权限控制

### 2. 聊天与消息路由

相关类：

- `ChatController`
- `MessageController`
- `ChatService`
- `MessageRouterService`
- `IntentService`

当前实现：

- 普通聊天
- 消息意图分类
- 根据意图决定走普通聊天还是心理分析 / RAG 路径

### 3. 心理分析与报告

相关类：

- `PsychologicalService`
- `ReportService`
- `ReportController`
- `PsychologicalReportRepository`

当前实现：

- 单条消息心理风险分析
- 心理报告入库
- 最近报告查询
- 高风险用户统计
- dashboard 聚合接口

### 4. 知识库与 RAG

相关类：

- `KnowledgeController`
- `KnowledgeService`
- `TextChunkService`
- `RerankService`
- `RuleBasedRerankService`
- `FallbackRerankService`
- `LocalModelRerankService`

当前实现：

- 知识添加
- 文档导入
- 向量检索
- 分类过滤
- 重排
- 文档分页和删除

### 5. Agent 与工具调用

相关类：

- `AgentController`
- `AgentService`
- `ToolCallController`
- `ToolCallService`
- `ToolRegistry`
- `ToolExecutor`

当前实现：

- Agent 判断是否需要工具
- 后端统一调用工具
- 使用工具结果生成最终回复
- 工具列表发现

### 6. 会话记忆与用户画像

相关类：

- `ConversationController`
- `AdminMemoryController`
- `ConversationMemoryService`
- `UserProfileController`
- `UserProfileService`

当前实现：

- 会话创建与会话消息持久化
- 会话摘要和中长期记忆维护
- 用户画像维护
- 管理员查看记忆与会话

## 数据依赖

当前代码依赖以下外部组件：

- MySQL
  用户、报告、会话、画像、知识文档等持久化
- Redis
  锁和缓存相关逻辑
- Ollama
  聊天、分析和部分记忆整理模型调用
- Chroma
  向量存储
- SMTP
  高风险邮件告警

## 接口权限边界

根据 `SecurityConfig`，当前大致分为三类：

- 公开接口
  - `/auth/**`
  - `/test/**`
  - `/chat`
- 登录后可访问
  - `/reports/my`
  - `/reports/dashboard`
  - `/chat/analyze`
  - `/message`
  - `/agent/chat`
  - `/conversations/**`
- 管理员接口
  - `/admin/**`
  - `/profile/**`
  - `/knowledge/**`
  - `/reports/**`

## 当前代码里的几个事实

- 工具 HTTP 接口当前公开的是 `GET /tools` 和 `POST /tools/call`
- `AgentService` 的工具决策提示词里仍只明确列出 `search_knowledge` 和 `get_dashboard`
- 仓库中虽然有更多工具类实现，但是否被 Agent 主动选中，还取决于 `AgentService` 的决策提示词
- 记忆服务和用户画像已经接入聊天主链，不只是单独的管理接口
