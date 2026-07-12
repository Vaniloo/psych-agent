# 测试计划

## 1. 概述

### 1.1 项目背景

Psych Agent 是一个基于 Spring Boot、Spring AI、RAG、Tool Calling 和分层记忆的心理支持 Agent 平台。项目核心功能包括：用户认证、智能聊天、风险检测与危机干预、知识库检索（RAG）、多轮对话记忆管理、角色卡系统、管理员后台等。

### 1.2 测试目标

- 确保核心业务功能正确实现
- 验证安全边界（权限隔离、危机响应不可绕过）
- 保证系统稳定性和可用性
- 确保数据一致性和完整性
- 提供可重复的测试流程，支持持续集成

### 1.3 测试范围

| 模块 | 测试范围 |
|------|----------|
| 用户认证 | 注册、登录、JWT 验证、权限隔离 |
| 智能聊天 | 普通聊天、心理咨询、意图路由 |
| 风险检测 | 高风险词识别、危机响应、告警流程 |
| RAG 检索 | 知识库导入、分块、检索、重排 |
| 工具调用 | 工具决策、执行、参数校验 |
| 记忆系统 | 短期/中期/长期记忆、用户画像 |
| 管理员功能 | 记忆中心、报告管理、用户管理 |
| 留言反馈 | 用户反馈提交、管理员查看与回复 |

## 2. 测试环境

### 2.1 本地开发环境

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 项目编译和运行环境 |
| Maven | 3.8+ | 构建工具（通过 mvnw） |
| MySQL | 8.4 | 关系型数据库 |
| Redis | 7.x | 缓存和消息队列 |
| Chroma | latest | 向量数据库 |
| Ollama | latest | 本地大语言模型服务 |
| Docker | 24+ | 容器化部署 |

### 2.2 环境准备步骤

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

### 2.3 Docker Compose 环境（推荐）

```bash
cp .env.example .env
docker compose up --build
```

## 3. 测试策略

### 3.1 测试类型

| 测试类型 | 工具/方法 | 覆盖范围 |
|----------|-----------|----------|
| 单元测试 | JUnit 5 | 服务层逻辑、工具类、风险检测 |
| 集成测试 | Spring Boot Test | Controller、Service、Repository 联动 |
| 接口测试 | curl / Postman | REST API 验证 |
| 安全测试 | 手动验证 | 权限隔离、JWT 安全 |
| 性能测试 | JMeter / k6 | 并发聊天、RAG 检索 |
| 冒烟测试 | 手动验证 | 核心功能可用性 |

### 3.2 测试优先级

| 优先级 | 定义 | 示例 |
|--------|------|------|
| P0 | 阻塞性问题，影响核心功能 | 登录失败、危机响应失效 |
| P1 | 重要功能缺陷，影响用户体验 | RAG 检索结果不准确 |
| P2 | 一般缺陷，不影响核心流程 | UI 显示问题、非关键 API 异常 |
| P3 | 优化建议，不影响功能 | 代码规范、性能优化 |

### 3.3 测试矩阵

| 用户角色 | 功能模块 | 测试重点 |
|----------|----------|----------|
| 普通用户 | 登录/注册 | 认证流程、密码安全 |
| 普通用户 | 聊天 | 普通聊天、心理咨询、意图路由 |
| 普通用户 | 风险检测 | 高风险词触发危机响应 |
| 普通用户 | 记忆 | 对话记忆延续、用户画像 |
| 普通用户 | 留言反馈 | 提交反馈、查看状态 |
| 管理员 | 记忆中心 | 查看所有用户记忆 |
| 管理员 | 报告管理 | 风险报告、仪表盘 |
| 管理员 | 知识库管理 | 文档导入、分类检索 |
| 管理员 | 用户管理 | 用户列表、角色管理 |

## 4. 测试流程

### 4.1 开发阶段测试

1. **编写代码**：实现功能模块
2. **编写单元测试**：覆盖核心逻辑
3. **运行单元测试**：`cd backend && ./mvnw test`
4. **代码审查**：检查测试覆盖率和代码质量

### 4.2 集成测试阶段

1. **启动依赖服务**：MySQL、Redis、Chroma、Ollama
2. **运行集成测试**：`cd backend && ./mvnw test -Dtest=*IntegrationTest`
3. **接口测试**：使用 curl 或 Postman 验证 API

### 4.3 回归测试阶段

1. **运行全量测试**：`cd backend && ./mvnw test`
2. **冒烟测试**：验证核心功能可用性
3. **性能测试**：验证系统响应时间

### 4.4 发布前测试

1. **Docker Compose 验证**：`docker compose config --quiet`
2. **健康检查**：访问 `/actuator/health`
3. **端到端验证**：登录、聊天、风险检测全流程

## 5. 测试工具与框架

### 5.1 单元测试框架

- **JUnit 5**：Java 单元测试标准框架
- **Mockito**：Mock 对象框架，用于隔离依赖
- **Spring Boot Test**：Spring Boot 集成测试支持

### 5.2 接口测试工具

- **curl**：命令行 HTTP 客户端
- **Postman**：可视化 API 测试工具
- **Spring REST Docs**：自动生成 API 文档

### 5.3 测试覆盖率

```bash
# 生成测试覆盖率报告
cd backend
./mvnw jacoco:report
# 报告路径：target/site/jacoco/index.html
```

## 6. 风险与注意事项

### 6.1 外部依赖风险

| 依赖 | 风险描述 | 缓解措施 |
|------|----------|----------|
| Ollama | 模型加载失败或响应慢 | 增加超时配置、降级策略 |
| Chroma | 向量检索不可用 | Redis 缓存降级、本地文件备份 |
| MySQL | 数据库连接失败 | 连接池配置、重试机制 |

### 6.2 安全风险

- **危机响应不可绕过**：确保高风险消息强制进入危机处理流程
- **权限隔离**：普通用户不能访问管理员接口
- **JWT 安全**：密钥不进入版本库，设置合理过期时间

### 6.3 性能风险

- **LLM 调用延迟**：增加缓存机制、异步处理
- **RAG 检索性能**：索引优化、分块策略调整

## 7. 测试交付物

| 交付物 | 说明 |
|--------|------|
| 测试计划文档 | 本文档 |
| 测试用例文档 | 详细测试用例设计 |
| 测试报告 | 测试执行结果汇总 |
| 缺陷记录 | 发现的问题及修复状态 |
| 覆盖率报告 | 代码覆盖率统计 |

## 8. 附录

### 8.1 测试命令速查

```bash
# 运行所有单元测试
cd backend && ./mvnw test

# 运行指定测试类
cd backend && ./mvnw test -Dtest=RiskDetectionServiceTests

# 跳过测试构建
cd backend && ./mvnw package -DskipTests

# 启动应用
cd backend && ./mvnw spring-boot:run
```

### 8.2 测试数据准备

```bash
# 创建测试用户（通过 API）
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123456"}'

# 创建管理员用户（需数据库直接插入或注册后手动修改角色）
```