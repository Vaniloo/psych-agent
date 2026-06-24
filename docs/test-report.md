# 测试现状

本文档描述当前仓库里可以直接看到的测试现状。

## 已存在测试

### 1. 应用上下文测试

文件：

- `backend/src/test/java/com/vanilo/psych/agent/PsychAgentApplicationTests.java`

特点：

- 基于 `@SpringBootTest`
- 需要完整 Spring 上下文
- 通常依赖数据库等外部配置可用

### 2. 工具调用服务单测

文件：

- `backend/src/test/java/com/vanilo/psych/agent/service/ToolCallServiceTests.java`

覆盖点：

- 工具结果包装
- 必填参数校验
- 参数类型校验

## 当前测试特点

- 单测数量较少
- 集成测试依赖外部服务环境
- 文档中不应假定“所有接口都已经有自动化覆盖”

## 本地验证建议

### 轻量验证

```powershell
cd backend
$env:JAVA_HOME='C:\Program Files\Java\jdk-22'
.\mvnw.cmd -Dtest=ToolCallServiceTests test
```

### 编译验证

```powershell
cd backend
$env:JAVA_HOME='C:\Program Files\Java\jdk-22'
.\mvnw.cmd -DskipTests compile
```

### 完整上下文测试

```powershell
cd backend
$env:JAVA_HOME='C:\Program Files\Java\jdk-22'
.\mvnw.cmd test
```

注意：

- 如果本地 MySQL 不可用，完整测试很可能失败
- 这类失败通常是环境依赖问题，不一定代表业务代码本身编译错误
