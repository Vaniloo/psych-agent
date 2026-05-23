Psych Agent 前端是一个零依赖静态应用，可以用任意静态服务器托管。

默认本地配置在 `config.js`：

- Backend API: `http://localhost:8080`
- MySQL: `localhost:3306/psych_agent`
- Redis: `localhost:6379`
- Ollama: `http://localhost:11434`
- Chroma: `http://localhost:8000`

本地启动：

```bash
cd frontend
python3 -m http.server 5173
```

然后访问 `http://localhost:5173`。
