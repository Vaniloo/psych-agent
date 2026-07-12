# 缺陷记录

## 缺陷跟踪说明

本文档记录项目开发过程中发现的缺陷、问题和优化建议。每条记录包含缺陷描述、严重程度、影响范围、复现步骤、状态和处理建议。

**严重程度定义：**
- **P0 - 紧急**：阻塞性问题，影响核心功能或系统稳定性
- **P1 - 高**：重要功能缺陷，影响用户体验或业务流程
- **P2 - 中**：一般缺陷，不影响核心流程
- **P3 - 低**：优化建议，不影响功能

**状态定义：**
- **待处理**：已发现，等待修复
- **修复中**：正在修复
- **已修复**：已完成修复
- **已验证**：修复已通过测试验证
- **关闭**：问题已解决

---

## 缺陷列表

### DEF-001：pom.xml 中冗余的 Kotlin 编译器配置

| 属性 | 值 |
|------|------|
| 严重程度 | P2 |
| 状态 | 待处理 |
| 发现时间 | 2026-07-11 |
| 影响模块 | 构建系统 |

**缺陷描述：**
`pom.xml` 中配置了完整的 Kotlin 编译器插件（`kotlin-maven-plugin`），包括编译、测试编译、JPA 和 Spring 插件支持，但项目代码是 100% Java 编写的，没有任何 Kotlin 源代码。这增加了构建时间和配置复杂性，也会让新手开发者困惑。

**问题代码位置：**
[pom.xml](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/pom.xml#L164-L209)

**具体问题：**
1. 第 164-209 行：`kotlin-maven-plugin` 配置了完整的编译生命周期
2. 第 135-144 行：引入了 Kotlin 标准库和测试依赖
3. 第 237-244 行：Maven 编译器插件配置了 Lombok 注解处理器，但 Kotlin 插件也处理注解

**修复建议：**
1. 移除 `kotlin-maven-plugin` 配置
2. 移除 `kotlin-stdlib-jdk8` 和 `kotlin-test` 依赖
3. 保留 Maven 编译器插件的注解处理器配置（仅保留 Lombok）

**修复后预期效果：**
- 构建时间缩短
- pom.xml 配置更简洁
- 避免新手开发者误解项目使用 Kotlin

---

### DEF-002：AgentService.decideTool() 缺少 LLM 失败降级机制

| 属性 | 值 |
|------|------|
| 严重程度 | P1 |
| 状态 | 待处理 |
| 发现时间 | 2026-07-11 |
| 影响模块 | Agent 服务、工具调用 |

**缺陷描述：**
[AgentService.decideTool()](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/src/main/java/com/vanilo/psych/agent/service/AgentService.java#L126-L186) 方法在 LLM 调用返回 null 或无效 JSON 时直接抛出 `RuntimeException`，没有优雅的降级策略。当 Ollama 服务不可用或响应超时，会导致整个聊天请求失败。

**问题代码位置：**
[AgentService.java 第 169-185 行](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/src/main/java/com/vanilo/psych/agent/service/AgentService.java#L169-L185)

**具体问题：**
```java
// 第 178-179 行：LLM 返回 null 时直接抛出异常
if (result == null) {
    throw new RuntimeException("未返回合法json");
}

// 第 181-184 行：JSON 解析失败时直接抛出异常
try {
    return objectMapper.readValue(result, ToolDecisionResponse.class);
} catch (Exception e) {
    throw new RuntimeException("解析失败");
}
```

**修复建议：**
当 LLM 调用失败或返回无效结果时，降级为"不需要工具"，直接调用 `generatePlainReply()` 进行普通聊天回复。

```java
private ToolDecisionResponse decideTool(String message) {
    try {
        String result = llmService.complete(...);
        // ... JSON 解析逻辑 ...
        return objectMapper.readValue(result, ToolDecisionResponse.class);
    } catch (Exception e) {
        // 降级：不使用工具，直接回复
        ToolDecisionResponse fallback = new ToolDecisionResponse();
        fallback.setNeedTool(false);
        fallback.setReply("");
        return fallback;
    }
}
```

**修复后预期效果：**
- LLM 不可用时，聊天功能仍然可用（只是不调用工具）
- 用户体验更好，不会因为工具决策失败而完全无法聊天

---

### DEF-003：AdminMemoryController 冗余的认证检查

| 属性 | 值 |
|------|------|
| 严重程度 | P2 |
| 状态 | 待处理 |
| 发现时间 | 2026-07-11 |
| 影响模块 | 管理员控制器 |

**缺陷描述：**
[AdminMemoryController](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/src/main/java/com/vanilo/psych/agent/controller/AdminMemoryController.java) 中的每个方法都手动检查 `authentication == null`，但这些检查是冗余的，因为 [SecurityConfig](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/src/main/java/com/vanilo/psych/agent/config/SecurityConfig.java#L33) 已经通过 `requestMatchers("/admin/**").hasRole("ADMIN")` 强制要求管理员权限。Spring Security 会在到达 Controller 之前就拦截未认证的请求。

**问题代码位置：**
[AdminMemoryController.java 第 21-24、30-32、40-42 行](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/src/main/java/com/vanilo/psych/agent/controller/AdminMemoryController.java#L21-L42)

**具体问题：**
```java
@GetMapping
public List<AdminMemoryResponse> listMemories(Authentication authentication) {
    if (authentication == null) {  // 冗余检查
        throw new RuntimeException("未登录");
    }
    return conversationMemoryService.listAdminMemories();
}
```

**修复建议：**
移除所有 `if (authentication == null)` 检查，Spring Security 会自动处理认证和授权。

**修复后预期效果：**
- 代码更简洁
- 避免重复的认证逻辑
- 统一由 Spring Security 处理权限控制

---

### DEF-004：缺少 CI/CD 流水线配置

| 属性 | 值 |
|------|------|
| 严重程度 | P2 |
| 状态 | 待处理 |
| 发现时间 | 2026-07-11 |
| 影响模块 | 持续集成 |

**缺陷描述：**
项目没有配置任何 CI/CD 流水线（如 GitHub Actions、GitLab CI、Jenkins）。每次代码提交后需要手动运行测试，无法自动验证代码质量和构建状态。

**影响范围：**
- 无法自动化运行单元测试
- 无法自动验证 Docker Compose 配置
- 无法自动构建和部署
- 无法及时发现代码质量问题

**修复建议：**
在 `.github/workflows/` 目录下创建 `ci.yml` 文件，配置 GitHub Actions 流水线，包含以下步骤：
1. 检出代码
2. 运行 Maven 测试
3. 验证 Docker Compose 配置
4. 构建 Docker 镜像

**预期效果：**
- 每次代码推送自动运行测试
- 及时发现构建和测试失败
- 提高代码质量和开发效率

---

### DEF-005：ReportController 权限控制不一致

| 属性 | 值 |
|------|------|
| 严重程度 | P1 |
| 状态 | 待处理 |
| 发现时间 | 2026-07-11 |
| 影响模块 | 报告控制器 |

**缺陷描述：**
[ReportController](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/src/main/java/com/vanilo/psych/agent/controller/ReportController.java) 中的权限控制存在不一致问题。

**问题代码位置：**
[SecurityConfig.java 第 37 行](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/src/main/java/com/vanilo/psych/agent/config/SecurityConfig.java#L37)

**具体问题：**
1. SecurityConfig 中配置了 `requestMatchers("/reports/**").hasRole("ADMIN")`，意味着所有 `/reports/**` 路径都需要 ADMIN 角色
2. 但 ReportController 中有 `/reports/my` 和 `/reports/dashboard` 端点，这些应该允许普通用户访问自己的数据
3. 同时，Controller 内部又手动检查 `authentication == null`，与 SecurityConfig 的全局配置冲突

**修复建议：**
1. 修改 SecurityConfig，将 `/reports/my` 和 `/reports/dashboard` 改为 `authenticated()`（允许认证用户访问）
2. 将其他 `/reports/**` 端点保持为 `hasRole("ADMIN")`
3. 移除 Controller 内部冗余的认证检查

**修复后预期效果：**
- 普通用户可以访问自己的报告和仪表盘
- 管理员可以访问所有报告和全局仪表盘
- 权限控制统一且清晰

---

### DEF-006：缺少全局异常处理的统一格式

| 属性 | 值 |
|------|------|
| 严重程度 | P2 |
| 状态 | 待处理 |
| 发现时间 | 2026-07-11 |
| 影响模块 | 全局异常处理 |

**缺陷描述：**
项目中有 [GlobalExceptionHandler](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/src/main/java/com/vanilo/psych/agent/exception/GlobalExceptionHandler.java)，但 SecurityConfig 中的异常处理（第 40-51 行）直接写入 JSON 响应，与全局异常处理器的格式不一致。

**问题代码位置：**
[SecurityConfig.java 第 40-51 行](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/src/main/java/com/vanilo/psych/agent/config/SecurityConfig.java#L40-L51)

**具体问题：**
```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((request, response, authException) -> {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(401);
        response.getWriter().write("{\"success\":false,\"message\":\"未认证\"}");
    })
    .accessDeniedHandler((request, response, accessDeniedException) -> {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(403);
        response.getWriter().write("{\"success\":false,\"message\":\"无权限\"}");
    })
);
```

**修复建议：**
创建统一的错误响应格式，让 SecurityConfig 的异常处理和 GlobalExceptionHandler 使用相同的格式。可以使用 `ErrorResponse` DTO。

**修复后预期效果：**
- 所有错误响应格式统一
- 前端可以统一处理错误响应
- 便于调试和日志分析

---

### DEF-007：ConversationMemoryService 缺少事务回滚保护

| 属性 | 值 |
|------|------|
| 严重程度 | P2 |
| 状态 | 待处理 |
| 发现时间 | 2026-07-11 |
| 影响模块 | 记忆系统 |

**缺陷描述：**
[ConversationMemoryService.rememberTurn()](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/src/main/java/com/vanilo/psych/agent/service/ConversationMemoryService.java#L108-L115) 方法使用了 `@Transactional` 注解，但 `updateMemoryAndProfile()` 方法内部有自己的异常处理逻辑，可能导致部分操作成功而其他操作失败，破坏数据一致性。

**问题代码位置：**
[ConversationMemoryService.java 第 205-218 行](file:///C:/Users/28218/Desktop/work/SE/psych-agent-main/psych-agent-main/backend/src/main/java/com/vanilo/psych/agent/service/ConversationMemoryService.java#L205-L218)

**具体问题：**
```java
try {
    Map<String, String> updates = objectMapper.readValue(extractJson(json), new TypeReference<>() {});
    applyMemoryUpdates(session, memory, profile, updates);
    conversationSessionRepository.save(session);
    conversationMemoryRepository.save(memory);
    userProfileRepository.save(profile);
} catch (Exception e) {
    // 部分更新：只更新 session 和 memory，不更新 profile
    session.setSummary(...);
    session.setUpdatedAt(LocalDateTime.now());
    memory.setSummary(...);
    memory.setUpdatedAt(LocalDateTime.now());
    conversationSessionRepository.save(session);
    conversationMemoryRepository.save(memory);
}
```

**修复建议：**
考虑将异常处理逻辑改为完全回滚或完全成功，或者明确文档化这种部分更新的行为。

**修复后预期效果：**
- 数据一致性得到保证
- 避免部分更新导致的状态不一致

---

## 缺陷统计

| 严重程度 | 数量 | 待处理 | 修复中 | 已修复 |
|----------|------|--------|--------|--------|
| P0 | 0 | 0 | 0 | 0 |
| P1 | 2 | 2 | 0 | 0 |
| P2 | 5 | 5 | 0 | 0 |
| P3 | 0 | 0 | 0 | 0 |
| **合计** | **7** | **7** | **0** | **0** |

---

## 附录：缺陷记录模板

```markdown
### DEF-XXX：缺陷标题

| 属性 | 值 |
|------|------|
| 严重程度 | P0/P1/P2/P3 |
| 状态 | 待处理/修复中/已修复/已验证/关闭 |
| 发现时间 | YYYY-MM-DD |
| 影响模块 | 模块名称 |

**缺陷描述：**
详细描述缺陷现象和影响

**问题代码位置：**
[文件名](file:///绝对路径#L行号)

**具体问题：**
代码片段或详细说明

**修复建议：**
具体的修复方案或代码示例

**修复后预期效果：**
修复后的预期行为
```