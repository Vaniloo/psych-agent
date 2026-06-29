# API 测试

启动后端及依赖后执行：

```bash
./scripts/curl-tests.sh
```

脚本检查 `/test/ping`、`/actuator/health` 和 `/help/resources`。业务回归测试：

```bash
cd backend
./mvnw test
```

登录接口使用 `POST /auth/login`，成功后将响应中的 `token` 作为 `Authorization: Bearer <token>` 访问受保护接口。普通用户不能访问 `/admin/**`、`/knowledge/**`、管理员画像和全局高风险数据。
