# 部署

## Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

Compose 包含 MySQL、Redis、Chroma、Ollama、模型初始化、后端和 Nginx 前端。

## 必填生产配置

- `JWT_SECRET`
- `MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD`
- `MAIL_USERNAME` / `MAIL_PASSWORD`（启用邮件告警时）

## 健康依赖

后端启动依赖 MySQL、Redis、Chroma 和 Ollama。Docker Compose 使用健康检查控制启动顺序。

本地混合部署可执行 `scripts/start-backend.sh`：脚本会检查 MySQL，并自动启动已有的 `redis`、`chroma` Docker 容器。Apple M5 上桌面版 Ollama 无法加载模型时，先执行 `scripts/start-ollama-docker.sh`，再把 `OLLAMA_BASE_URL` 设为 `http://localhost:11435`。

启动后检查 `http://localhost:8080/actuator/health`。`/actuator/metrics` 需要 ADMIN JWT。
