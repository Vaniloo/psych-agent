(() => {
  const pageHost = window.location.hostname;
  const apiHost = pageHost && pageHost !== "0.0.0.0" ? pageHost : "localhost";
  const developmentPort = window.location.port === "5173";

  window.PSYCH_AGENT_CONFIG = {
    apiBaseUrl: developmentPort ? `http://${apiHost}:8080` : `${window.location.origin}/api`,
    localServices: {
      mysql: "localhost:3306/psych_agent",
      redis: "localhost:6379",
      ollama: "http://localhost:11435",
      chroma: "http://localhost:8000",
    },
  };
})();
