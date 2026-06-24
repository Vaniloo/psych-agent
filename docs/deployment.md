# 部署与运行说明

本文档描述当前源码在本地或开发环境中的基本运行方式。

## 依赖服务

后端默认依赖：

- MySQL
- Redis
- Ollama
- Chroma
- SMTP 邮件服务

## 默认配置

来自 `backend/src/main/resources/application.yaml`：

- `server.port=8080`
- `MYSQL_URL=jdbc:mysql://localhost:3306/psych_agent?...`
- `MYSQL_USERNAME=root`
- `REDIS_HOST=localhost`
- `REDIS_PORT=6379`
- `OLLAMA_BASE_URL=http://localhost:11434`
- `CHROMA_HOST=http://localhost`
- `CHROMA_PORT=8000`

## 启动后端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

## 启动前端

```powershell
cd frontend
python -m http.server 5173
```

## 当前部署层面的注意事项

- 默认配置中包含本地开发值，应在真实环境中改为环境变量注入
- 邮件配置当前写在 `application.yaml` 中，生产环境不应直接沿用
- 完整功能依赖 Ollama、Chroma 和数据库服务同时可用
- 完整测试依赖数据库连接正常
