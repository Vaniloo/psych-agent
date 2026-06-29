# Psych Agent

基于 Spring Boot、Spring AI、RAG、Tool Calling 和分层记忆的心理支持 Agent 平台。

## 已实现能力

- JWT 登录、USER / ADMIN 权限隔离
- CHAT / CONSULT / RISK 意图路由
- 确定性危机回复、12356 / 120 / 110 帮助中心资源
- 风险报告、持久化告警事件、Redis Stream 异步邮件/Excel 通知与重试
- RAG 文档导入、分块、Embedding、分类检索、重排、Redis 缓存与 MySQL 降级
- Agent 工具调用：知识检索、Dashboard、用户记忆、风险扫描、策略推荐
- Redis 短期会话记忆；MySQL 会话摘要、长期记忆和用户画像
- 历史会话恢复与继续对话
- 预设/自定义角色卡和禁忌表达
- 风险分布、情绪趋势、管理员记忆中心与工具调试页
- Redis 限流、LLM 重试/熔断、健康监控、Docker Compose 与 Nginx

## 本地启动

需要 MySQL 8、Redis 7、Chroma、Ollama。先创建数据库并启动依赖：

```bash
mysql -uroot -p -e 'CREATE DATABASE IF NOT EXISTS psych_agent CHARACTER SET utf8mb4;'
brew services start redis
chroma run --host localhost --port 8000
ollama serve
```

本机尚未安装 Redis 或 Chroma 时，可先执行 `brew install redis` 和 `pipx install chromadb`；详见 [Chroma CLI 官方安装说明](https://docs.trychroma.com/docs/cli/install)。也可以直接使用下方 Docker Compose 一次启动全部依赖。

首次准备模型：

```bash
ollama pull qwen2.5:3b
ollama pull nomic-embed-text
```

复制配置并按本机数据库信息修改 `local.env`：

```bash
cp local.env.example local.env
```

启动后端：

```bash
./scripts/start-backend.sh
```

启动前端：

```bash
./scripts/start-frontend.sh
```

访问 `http://localhost:5173`。

健康检查：`http://localhost:8080/actuator/health`。如果本机没有安装 Chroma，可直接使用下面的 Docker Compose 方式启动全部服务。

## Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

访问 `http://localhost`。生产环境务必修改 `.env` 中的数据库密码和 `JWT_SECRET`。

## 测试

```bash
cd backend
./mvnw test
```

外部服务集成测试需要先启动 MySQL、Redis、Chroma 和 Ollama。
