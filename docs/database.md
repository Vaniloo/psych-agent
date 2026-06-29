# 数据库设计

核心表：

- `users`：账号、角色、当前角色卡。
- `psychological_reports`：消息风险、情绪、置信度；含用户时间与风险时间复合索引。
- `knowledge_document`：知识原文、分类和来源。
- `conversation_sessions`：会话标题、摘要和更新时间。
- `conversation_messages`：用户/助手原始消息。
- `conversation_memories`：用户级中期摘要和长期稳定信息。
- `user_profiles`：困扰、偏好、应对方式、风险信号和支持目标。
- `role_cards`：预设和自定义陪伴风格。
- `risk_alert_events`：PENDING / PROCESSING / SENT / FAILED 告警状态与重试次数。

Hibernate 默认使用 `ddl-auto=update`。正式生产建议改用 Flyway 管理迁移。

索引验证：

```bash
mysql -uroot -p psych_agent < scripts/explain-report-indexes.sql
```

两条查询应分别优先使用 `idx_reports_user_created_at` 和 `idx_reports_risk_created_at`。
