# Git 分支管理规范

## 1. 概述

本文档定义了 Psych Agent 项目的 Git 分支管理规范，包括分支策略、命名约定、提交规范和代码审查流程。规范旨在：

- 确保代码版本管理清晰有序
- 支持团队协作开发
- 保障代码质量和稳定性
- 便于追踪和回滚变更

## 2. 分支策略

本项目采用 **Git Flow** 分支策略的简化版本，适合中小团队使用。

### 2.1 分支类型

| 分支类型 | 命名格式 | 用途 | 生命周期 |
|----------|----------|------|----------|
| **主分支** | `main` | 生产环境代码 | 永久存在 |
| **开发分支** | `develop` | 集成开发的最新代码 | 永久存在 |
| **功能分支** | `feature/xxx` | 开发新功能 | 临时，合并后删除 |
| **修复分支** | `fix/xxx` | 修复 Bug | 临时，合并后删除 |
| **发布分支** | `release/xxx` | 准备发布版本 | 临时，发布后删除 |
| **热修复分支** | `hotfix/xxx` | 紧急修复生产问题 | 临时，修复后删除 |

### 2.2 分支流程图

```
main ──────────────────────────────────────────────────────────────
       ↓ (merge)              ↓ (merge)              ↓ (merge)
develop ←─── feature/xxx ←─── fix/xxx ←─── release/xxx ←─── hotfix/xxx
       ↑ (merge)              ↑ (merge)              ↑ (merge)
```

## 3. 分支命名约定

### 3.1 主分支

- **main**：生产环境稳定代码
- **develop**：开发集成分支

### 3.2 功能分支

```
feature/<功能名称>
```

**示例：**
- `feature/feedback-module` - 留言反馈模块
- `feature/rag-rerank` - RAG 重排功能
- `feature/user-profile` - 用户画像功能

### 3.3 修复分支

```
fix/<缺陷描述>
```

**示例：**
- `fix/llm-fallback` - 修复 LLM 降级逻辑
- `fix/auth-permission` - 修复权限控制问题
- `fix/memory-transaction` - 修复记忆事务问题

### 3.4 发布分支

```
release/<版本号>
```

**示例：**
- `release/0.1.0`
- `release/0.2.0`

### 3.5 热修复分支

```
hotfix/<问题描述>
```

**示例：**
- `hotfix/crisis-response` - 紧急修复危机响应
- `hotfix/login-failure` - 紧急修复登录问题

## 4. 开发流程

### 4.1 开发新功能

```bash
# 1. 从 develop 分支创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/feedback-module

# 2. 开发代码，频繁提交
git add .
git commit -m "feat: 实现留言反馈模块"

# 3. 完成开发后，推送分支
git push origin feature/feedback-module

# 4. 创建 Pull Request 到 develop
# 5. 代码审查通过后，合并到 develop
# 6. 删除本地和远程分支
git checkout develop
git branch -d feature/feedback-module
git push origin --delete feature/feedback-module
```

### 4.2 修复 Bug

```bash
# 1. 从 develop 分支创建修复分支
git checkout develop
git pull origin develop
git checkout -b fix/llm-fallback

# 2. 修复代码
git add .
git commit -m "fix: 添加 LLM 降级机制"

# 3. 推送并创建 PR
git push origin fix/llm-fallback
```

### 4.3 发布版本

```bash
# 1. 从 develop 创建发布分支
git checkout develop
git pull origin develop
git checkout -b release/0.1.0

# 2. 进行发布前准备（版本号更新、文档更新）
git add .
git commit -m "chore: 准备发布版本 0.1.0"

# 3. 推送发布分支
git push origin release/0.1.0

# 4. 测试通过后，合并到 main 和 develop
git checkout main
git merge --no-ff release/0.1.0
git push origin main

git checkout develop
git merge --no-ff release/0.1.0
git push origin develop

# 5. 删除发布分支
git branch -d release/0.1.0
git push origin --delete release/0.1.0

# 6. 创建标签
git tag -a v0.1.0 -m "版本 0.1.0"
git push origin v0.1.0
```

### 4.4 紧急热修复

```bash
# 1. 从 main 创建热修复分支
git checkout main
git pull origin main
git checkout -b hotfix/crisis-response

# 2. 修复问题
git add .
git commit -m "fix: 紧急修复危机响应失效问题"

# 3. 推送并测试
git push origin hotfix/crisis-response

# 4. 合并到 main 和 develop
git checkout main
git merge --no-ff hotfix/crisis-response
git push origin main

git checkout develop
git merge --no-ff hotfix/crisis-response
git push origin develop

# 5. 删除分支
git branch -d hotfix/crisis-response
git push origin --delete hotfix/crisis-response

# 6. 创建新标签
git tag -a v0.1.1 -m "版本 0.1.1（紧急修复）"
git push origin v0.1.1
```

## 5. 提交规范

### 5.1 提交信息格式

```
<类型>: <描述>

<详细说明（可选）>

<关联信息（可选）>
```

### 5.2 提交类型

| 类型 | 说明 | 示例 |
|------|------|------|
| **feat** | 新功能 | `feat: 添加留言反馈模块` |
| **fix** | Bug 修复 | `fix: 修复 LLM 降级机制` |
| **docs** | 文档更新 | `docs: 更新部署文档` |
| **style** | 代码格式（不影响逻辑） | `style: 格式化代码` |
| **refactor** | 代码重构 | `refactor: 优化记忆服务` |
| **test** | 测试代码 | `test: 添加风险检测测试` |
| **chore** | 构建/工具/依赖 | `chore: 更新 Maven 依赖` |
| **perf** | 性能优化 | `perf: 优化 RAG 检索` |

### 5.3 提交信息示例

**好的示例：**
```
feat: 实现留言反馈模块

- 创建 FeedbackMessage 实体
- 实现 FeedbackService 服务层
- 添加 FeedbackController 控制器
- 实现用户提交和管理员回复功能

Closes: #123
```

**不好的示例：**
```
更新代码
```

### 5.4 提交频率

- 每个提交应该是一个完整的、可测试的单元
- 频繁提交，避免一次提交包含大量无关变更
- 每个提交应该通过单元测试

## 6. 代码审查流程

### 6.1 发起 Pull Request

1. 推送功能分支到远程仓库
2. 在 GitHub/GitLab 上创建 Pull Request
3. 指定审查人
4. 填写 PR 描述，包括：
   - 变更内容
   - 测试方法
   - 相关问题编号

### 6.2 代码审查检查清单

| 检查项 | 说明 |
|--------|------|
| **代码质量** | 代码是否清晰、简洁、符合规范 |
| **功能正确性** | 是否实现了预期功能 |
| **测试覆盖** | 是否有足够的单元测试 |
| **安全性** | 是否有安全隐患（SQL 注入、XSS 等） |
| **性能** | 是否有性能问题 |
| **文档** | 是否更新了相关文档 |

### 6.3 审查反馈

- 使用 GitHub/GitLab 的审查功能进行评论
- 审查通过后，点击"批准"
- 需要修改时，在评论中明确指出问题和建议

### 6.4 合并规则

- **必须**至少有一个审查人批准
- **必须**所有测试通过（CI 流水线）
- **必须**解决所有审查意见
- 使用 `--no-ff` 合并，保留分支历史

## 7. 版本号规范

采用 **语义化版本号**（Semantic Versioning）：

```
MAJOR.MINOR.PATCH
```

| 部分 | 说明 | 示例 |
|------|------|------|
| **MAJOR** | 不兼容的 API 变更 | 1.0.0 → 2.0.0 |
| **MINOR** | 向后兼容的功能新增 | 1.0.0 → 1.1.0 |
| **PATCH** | 向后兼容的 Bug 修复 | 1.0.0 → 1.0.1 |

### 7.1 版本号更新时机

- **PATCH**：修复 Bug、小改进
- **MINOR**：添加新功能、改进现有功能
- **MAJOR**：API 不兼容变更、架构重大调整

## 8. 协作注意事项

### 8.1 冲突解决

- 定期从 `develop` 分支拉取最新代码
- 遇到冲突时，仔细分析并手动解决
- 解决冲突后，确保测试通过

### 8.2 分支清理

- 功能完成并合并后，及时删除本地和远程分支
- 定期清理过时的本地分支

```bash
# 查看已合并的分支
git branch --merged

# 删除已合并的本地分支
git branch -d <branch-name>

# 删除远程分支
git push origin --delete <branch-name>
```

### 8.3 提交历史

- 保持提交历史清晰、有意义
- 使用 `git rebase` 整理提交历史（仅限本地分支）
- 不要改写已推送到远程的提交历史

## 9. 工具推荐

### 9.1 Git 客户端

| 工具 | 类型 | 说明 |
|------|------|------|
| **Git CLI** | 命令行 | 最基础的 Git 操作 |
| **GitHub Desktop** | GUI | 简单易用的图形界面 |
| **SourceTree** | GUI | 功能强大的 Git 客户端 |
| **GitKraken** | GUI | 现代化的 Git 客户端 |

### 9.2 代码审查工具

- **GitHub Pull Requests**：内置代码审查功能
- **GitLab Merge Requests**：内置代码审查功能
- **CodeStream**：IDE 内代码审查

## 10. 常见问题

### 10.1 如何撤销已提交的更改？

```bash
# 撤销最后一次提交（保留更改）
git reset --soft HEAD~1

# 撤销最后一次提交（丢弃更改）
git reset --hard HEAD~1

# 撤销指定提交
git revert <commit-hash>
```

### 10.2 如何将多个提交合并为一个？

```bash
# 合并最近 3 个提交
git rebase -i HEAD~3
```

在编辑器中，将需要合并的提交前面的 `pick` 改为 `squash` 或 `fixup`。

### 10.3 如何同步远程分支？

```bash
# 拉取远程分支并合并
git pull origin develop

# 强制同步（慎用）
git fetch origin
git reset --hard origin/develop
```

## 附录：常用 Git 命令

```bash
# 查看分支
git branch

# 查看远程分支
git branch -r

# 创建分支
git checkout -b <branch-name>

# 切换分支
git checkout <branch-name>

# 合并分支
git merge <branch-name>

# 拉取远程代码
git pull <remote> <branch>

# 推送到远程
git push <remote> <branch>

# 删除分支
git branch -d <branch-name>

# 查看提交历史
git log

# 查看变更
git diff

# 暂存更改
git stash

# 恢复暂存
git stash pop
```