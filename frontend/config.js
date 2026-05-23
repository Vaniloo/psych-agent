(() => {
  const pageHost = window.location.hostname;
  const apiHost = pageHost && pageHost !== "0.0.0.0" ? pageHost : "localhost";

  window.PSYCH_AGENT_CONFIG = {
    apiBaseUrl: `http://${apiHost}:8080`,
    localServices: {
      mysql: "localhost:3306/psych_agent",
      redis: "localhost:6379",
      ollama: "http://localhost:11434",
      chroma: "http://localhost:8000",
    },
  };
})();
