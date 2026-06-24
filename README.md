# Psych Agent

`Psych Agent` 是一个基于 Spring Boot 的心理支持后端与静态前端示例项目。当前仓库已经实现了用户鉴权、普通聊天、风险分析、RAG 知识检索、工具调用、会话记忆、用户画像、报告查询和简单管理接口。

仓库当前代码以本地开发运行为主，依赖以下外部服务：

- MySQL
- Redis
- Ollama
- Chroma
- SMTP 邮件服务

## 当前已实现能力

后端当前源码中已经实现的能力包括：

- JWT 注册与登录
- 普通聊天接口 `POST /chat`
- 心理风险分析接口 `POST /chat/analyze`
- 基于消息意图的路由处理 `POST /message`
- Agent 工具决策与最终回复生成 `POST /agent/chat`
- 知识库导入、检索、分页和删除
- 心理报告列表、分页、个人报告和 dashboard
- 会话列表、会话消息、记忆管理与管理员视图
- 用户画像查询与更新
- 工具注册、工具发现与工具统一调用

当前仓库中可发现的工具包括：

- `search_knowledge`
- `get_dashboard`
- `risk_scan`
- `get_user_memory`
- `recommend_strategy`

## 目录结构

```text
backend/   Spring Boot 后端
frontend/  零依赖静态前端
docs/      项目说明文档
scripts/   启动与调试脚本
```

## 本地运行前提

后端默认配置来自 `backend/src/main/resources/application.yaml`：

- 后端端口：`8080`
- MySQL：`jdbc:mysql://localhost:3306/psych_agent`
- Redis：`localhost:6379`
- Ollama：`http://localhost:11434`
- Chroma：`http://localhost:8000`

前端默认从 `frontend/config.js` 读取：

- API 基地址：`http://<当前主机>:8080`
- 本地服务展示信息：MySQL / Redis / Ollama / Chroma

建议准备：

- JDK 17+
- Maven Wrapper 可用环境
- Python 3 或任意静态文件服务器
- 可访问的 MySQL / Redis / Ollama / Chroma

## 启动方式

### 1. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

如果在 Windows PowerShell 下：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### 2. 启动前端

```bash
cd frontend
python3 -m http.server 5173
```

然后访问：

```text
http://localhost:5173
```

## 主要接口

### 公开接口

- `POST /auth/register`
- `POST /auth/login`
- `POST /chat`
- `GET /test/ping`
- `GET /test/db`
- `POST /test/mail`

### 需要登录

- `POST /chat/analyze`
- `POST /message`
- `POST /agent/chat`
- `GET /conversations`
- `GET /conversations/{sessionId}/messages`
- `GET /reports/my`
- `GET /reports/dashboard`

### 需要管理员权限

- `GET/PUT /profile/**`
- `GET /admin/memories/**`
- `GET/POST/DELETE /knowledge/**`
- `GET /reports/**`

### 工具接口

- `GET /tools`
- `POST /tools/call`

## 文档索引

- [架构说明](./docs/architecture.md)
- [接口说明](./docs/api-test.md)
- [工具说明](./docs/agent-tools.md)
- [部署说明](./docs/deployment.md)
- [数据库说明](./docs/database.md)
- [RAG 流程说明](./docs/rag-pipeline.md)
- [隐私与安全边界](./docs/privacy-design.md)
- [测试现状](./docs/test-report.md)

## 当前限制

当前代码里仍然存在一些明显的开发态限制，文档这里直接按实际情况说明：

- `application.yaml` 中存在本地开发默认配置，应在真实部署时改为环境变量管理
- 多个管理员接口当前只基于 `ROLE_ADMIN` 做权限控制，没有更细粒度授权
- `POST /chat` 直接接收原始字符串请求体，不是结构化 JSON
- 工具系统的 HTTP 暴露当前只有 `/tools` 和 `/tools/call`
- 测试覆盖较少，完整 `SpringBootTest` 依赖可用数据库环境

## 测试说明

当前仓库中可直接看到的测试包括：

- `backend/src/test/java/com/vanilo/psych/agent/PsychAgentApplicationTests.java`
- `backend/src/test/java/com/vanilo/psych/agent/service/ToolCallServiceTests.java`

在没有本地 MySQL 的情况下，更容易通过的是工具服务单测；完整应用上下文测试依赖数据库可连通。
