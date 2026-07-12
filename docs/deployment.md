# 部署文档

## 1. 概述

本文档详细说明 Psych Agent 项目的部署方式，包括本地开发环境、Docker Compose 部署和生产环境部署。

## 2. 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 后端运行环境 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 7.0+ | 缓存和消息队列 |
| Chroma | latest | 向量数据库 |
| Ollama | latest | 本地大语言模型服务 |
| Docker | 24.0+ | 容器化部署 |
| Docker Compose | 2.0+ | 多容器编排 |

## 3. 本地开发环境

### 3.1 手动安装依赖

```bash
# 1. 创建数据库
mysql -uroot -p -e 'CREATE DATABASE IF NOT EXISTS psych_agent CHARACTER SET utf8mb4;'

# 2. 启动 Redis
redis-server

# 3. 启动 Chroma
chroma run --host localhost --port 8000

# 4. 启动 Ollama
ollama serve

# 5. 下载模型
ollama pull qwen2.5:3b
ollama pull nomic-embed-text
```

### 3.2 配置文件

复制并修改配置文件：

```bash
cp local.env.example local.env
```

编辑 `local.env` 文件：

```env
# 数据库配置
MYSQL_URL=jdbc:mysql://localhost:3306/psych_agent
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_password

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379

# Chroma 配置
CHROMA_HOST=http://localhost
CHROMA_PORT=8000

# Ollama 配置
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=qwen2.5:3b
OLLAMA_EMBEDDING_MODEL=nomic-embed-text

# JWT 配置
JWT_SECRET=your-long-random-secret-key

# 邮件配置（可选，用于告警）
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your_email@qq.com
MAIL_PASSWORD=your_email_password
```

### 3.3 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

### 3.4 启动前端

```bash
cd frontend
python -m http.server 5173
# 或者使用 nginx
```

### 3.5 健康检查

```bash
# 后端健康检查
curl http://localhost:8080/actuator/health

# 预期响应：{"status":"UP"}

# 前端访问
# 打开浏览器访问 http://localhost:5173
```

## 4. Docker Compose 部署

### 4.1 环境准备

```bash
# 复制环境变量配置
cp .env.example .env
```

编辑 `.env` 文件，至少修改以下配置：

```env
# 必填配置
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_PASSWORD=your_db_password
JWT_SECRET=your-long-random-secret-key-at-least-32-characters

# 可选配置（启用邮件告警时）
MAIL_USERNAME=your_email@example.com
MAIL_PASSWORD=your_email_password
```

### 4.2 启动服务

```bash
# 构建并启动所有服务
docker compose up --build

# 后台运行
docker compose up --build -d

# 查看日志
docker compose logs -f

# 停止服务
docker compose down

# 停止服务并删除数据卷（谨慎使用）
docker compose down -v
```

### 4.3 服务说明

Docker Compose 启动以下服务：

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Chroma | 8000 | 向量数据库 |
| Ollama | 11434 | LLM 服务 |
| backend | 8080 | 后端 API |
| frontend | 80 | 前端页面（通过 Nginx） |

### 4.4 访问地址

- **前端页面**：http://localhost
- **后端 API**：http://localhost:8080
- **健康检查**：http://localhost:8080/actuator/health

### 4.5 Apple M5 Ollama 兼容模式

如果桌面版 Ollama 因 Metal 内核错误无法加载模型：

```bash
# 启动 Docker 版 Ollama
./scripts/start-ollama-docker.sh
```

在 `.env` 中设置：

```env
OLLAMA_BASE_URL=http://localhost:11435
```

## 5. 生产环境部署

### 5.1 安全配置

**必须修改的配置：**

```env
# JWT 密钥（至少 32 位随机字符）
JWT_SECRET=generate-a-long-random-secret-here

# 数据库密码
MYSQL_ROOT_PASSWORD=strong-root-password
MYSQL_PASSWORD=strong-db-password

# 邮件密码（如果启用）
MAIL_PASSWORD=your-email-app-password
```

### 5.2 数据库优化

在生产环境中，建议：

1. 创建专用数据库用户，限制权限
2. 启用 MySQL 慢查询日志
3. 配置合适的连接池大小
4. 创建必要的索引

```sql
-- 创建专用用户
CREATE USER 'psych_agent'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON psych_agent.* TO 'psych_agent'@'%';
FLUSH PRIVILEGES;

-- 常用索引（项目会自动创建）
-- conversation_messages: user_id, session_id, created_at
-- psychological_reports: user_id, risk_level, created_at
-- risk_alert_events: username, status, created_at
```

### 5.3 Redis 配置

```bash
# 生产环境建议配置
# redis.conf
requirepass your_redis_password
maxmemory 2gb
maxmemory-policy allkeys-lru
```

### 5.4 后端配置优化

在 `application.yaml` 或环境变量中配置：

```yaml
server:
  tomcat:
    max-threads: 200
    min-spare-threads: 20

spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000

  data:
    redis:
      timeout: 5000ms
```

### 5.5 HTTPS 配置

生产环境建议使用 HTTPS：

```bash
# 使用 Let's Encrypt 免费证书
certbot certonly --nginx -d your-domain.com
```

修改 Nginx 配置（`frontend/nginx.conf`）：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # ... 其他配置
}
```

### 5.6 监控和日志

```bash
# 查看后端日志
docker compose logs -f backend

# 查看数据库日志
docker compose logs -f mysql

# 查看 Redis 日志
docker compose logs -f redis
```

### 5.7 备份策略

```bash
# 数据库备份
mysqldump -u root -p psych_agent > backup_$(date +%Y%m%d).sql

# Redis 备份
redis-cli SAVE
cp /var/lib/redis/dump.rdb backup_$(date +%Y%m%d).rdb

# Chroma 备份（数据目录）
tar -czf chroma_backup_$(date +%Y%m%d).tar.gz /path/to/chroma/data
```

## 6. CI/CD 流水线

### 6.1 GitHub Actions

项目已配置 GitHub Actions CI 流水线（`.github/workflows/ci.yml`）：

**触发条件：**
- 推送到 `main` 或 `develop` 分支
- 创建 Pull Request 到 `main` 或 `develop` 分支

**执行步骤：**

1. **build-and-test**：
   - 检出代码
   - 设置 Java 17 环境
   - Maven 编译
   - 运行单元测试

2. **docker-validate**：
   - 验证 Docker Compose 配置
   - 构建 Docker 镜像

3. **lint**：
   - 检查前端 JavaScript 语法
   - 检查 Java 代码格式

### 6.2 手动触发 CI

```bash
# 推送代码自动触发
git push origin develop

# 创建 Pull Request 自动触发
# 在 GitHub 上创建 PR 到 main 或 develop 分支
```

### 6.3 查看 CI 结果

访问 GitHub 仓库的 **Actions** 标签页查看流水线执行结果。

## 7. 部署故障排查

### 7.1 端口占用

```bash
# 查看端口占用
netstat -tlnp | grep 8080

# 杀死占用进程（Linux）
kill -9 $(lsof -t -i:8080)

# 杀死占用进程（Windows PowerShell）
Get-NetTCPConnection -LocalPort 8080 | Select-Object -Unique OwningProcess | Where-Object { $_.OwningProcess -gt 0 } | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

### 7.2 数据库连接失败

```bash
# 检查 MySQL 是否启动
docker compose ps mysql

# 检查数据库连接
mysql -h localhost -u root -p -e "SELECT 1"

# 检查数据库是否存在
mysql -h localhost -u root -p -e "SHOW DATABASES LIKE 'psych_agent'"
```

### 7.3 Ollama 模型加载失败

```bash
# 检查 Ollama 服务状态
ollama list

# 重新拉取模型
ollama pull qwen2.5:3b
ollama pull nomic-embed-text

# 检查模型文件
ls ~/.ollama/models/manifests/registry.ollama.ai/library/
```

### 7.4 健康检查失败

```bash
# 检查健康状态
curl http://localhost:8080/actuator/health

# 查看详细健康信息（需要 ADMIN 权限）
curl -H "Authorization: Bearer YOUR_ADMIN_TOKEN" http://localhost:8080/actuator/health/detail
```

### 7.5 Docker 日志排查

```bash
# 查看所有服务日志
docker compose logs

# 查看特定服务日志
docker compose logs backend

# 实时查看日志
docker compose logs -f backend

# 查看最近 100 行日志
docker compose logs --tail=100 backend
```

## 8. 环境变量参考

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `JWT_SECRET` | - | JWT 签名密钥（必填） |
| `JWT_EXPIRATION` | 86400000 | JWT 过期时间（毫秒） |
| `MYSQL_URL` | jdbc:mysql://localhost:3306/psych_agent | 数据库连接 URL |
| `MYSQL_USERNAME` | root | 数据库用户名 |
| `MYSQL_PASSWORD` | - | 数据库密码（必填） |
| `REDIS_HOST` | localhost | Redis 主机 |
| `REDIS_PORT` | 6379 | Redis 端口 |
| `REDIS_PASSWORD` | - | Redis 密码 |
| `CHROMA_HOST` | http://localhost | Chroma 主机 |
| `CHROMA_PORT` | 8000 | Chroma 端口 |
| `OLLAMA_BASE_URL` | http://localhost:11434 | Ollama API 地址 |
| `OLLAMA_CHAT_MODEL` | qwen2.5:3b | 聊天模型 |
| `OLLAMA_EMBEDDING_MODEL` | nomic-embed-text | Embedding 模型 |
| `MAIL_HOST` | smtp.qq.com | 邮件服务器 |
| `MAIL_PORT` | 587 | 邮件端口 |
| `MAIL_USERNAME` | - | 邮件用户名 |
| `MAIL_PASSWORD` | - | 邮件密码 |
| `AI_PROVIDER` | ollama | AI 提供商 |

## 附录：常用命令

```bash
# 启动所有服务
docker compose up --build -d

# 停止所有服务
docker compose down

# 重启后端服务
docker compose restart backend

# 查看服务状态
docker compose ps

# 进入后端容器
docker compose exec backend bash

# 进入数据库容器
docker compose exec mysql mysql -u root -p

# 查看容器资源使用
docker stats

# 清理未使用的镜像和容器
docker system prune -a
```