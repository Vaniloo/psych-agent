# RAG Pipeline

1. 管理员添加或导入知识。
2. 长文按 ChunkSize / Overlap 切分。
3. Ollama Embedding 生成向量并写入 Chroma。
4. 查询规范化后召回最多 18 个候选。
5. 分类过滤、去重、本地模型重排；模型失败时规则重排。
6. Chroma 无结果时从 MySQL 做关键词兜底。
7. 最终返回默认 6 条，包含排名、来源、分类、相关度、置信标签和匹配原因。
8. Agent 调用 `search_knowledge` 或 `recommend_strategy` 时，会在响应中返回 `ragTrace`，包含本轮 query、tool、引用数量和可展示的 citation 摘要。
9. 查询结果缓存 Redis 10 分钟；知识变更后递增 `rag:cache:version`，新查询会进入新的 `rag:v4:*` 缓存空间。
10. 管理员可通过 `/knowledge/reindex?source=...` 按来源重建向量索引。

回答模型必须标注自然语言来源，知识不足时明确说明，不得把低相关结果硬套到用户问题。
