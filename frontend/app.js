const config = window.PSYCH_AGENT_CONFIG || {};
const state = {
  apiBaseUrl: localStorage.getItem("psychAgentApiBaseUrl") || config.apiBaseUrl || "http://localhost:8080",
  token: localStorage.getItem("psychAgentToken") || "",
  user: JSON.parse(localStorage.getItem("psychAgentUser") || "null"),
  chatMode: "agent",
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

function init() {
  bindNavigation();
  bindAuth();
  bindChat();
  bindProfile();
  bindReports();
  bindKnowledge();
  bindSettings();
  syncConnectionLabel();
  hydrateSettings();
  addMessage("assistant", "你好，我是 Psych Agent。登录后可以开始对话、查看画像和报告。");
  if (state.token) {
    refreshProfile();
  }
}

async function api(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  if (state.token && options.auth !== false) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  const { auth, ...fetchOptions } = options;
  return requestJson(path, { ...fetchOptions, headers }, true);
}

async function requestJson(path, options, allowFallback) {
  try {
    const response = await fetch(`${state.apiBaseUrl}${path}`, options);
    const text = await response.text();
    const data = text ? parseJson(text) : null;
    if (!response.ok || data?.success === false) {
      throw new Error(data?.message || text || `请求失败：${response.status}`);
    }
    return data ?? text;
  } catch (error) {
    const fallbackUrl = getLoopbackFallbackUrl(state.apiBaseUrl);
    if (allowFallback && fallbackUrl) {
      state.apiBaseUrl = fallbackUrl;
      localStorage.setItem("psychAgentApiBaseUrl", state.apiBaseUrl);
      hydrateSettings();
      syncConnectionLabel();
      return requestJson(path, options, false);
    }
    if (error instanceof TypeError) {
      throw new Error(`无法连接后端：${state.apiBaseUrl}。请确认后端已启动，并打开 ${state.apiBaseUrl}/test/ping 检查。`);
    }
    throw error;
  }
}

function getLoopbackFallbackUrl(apiBaseUrl) {
  if (apiBaseUrl.includes("localhost")) {
    return apiBaseUrl.replace("localhost", "127.0.0.1");
  }
  if (apiBaseUrl.includes("127.0.0.1")) {
    return apiBaseUrl.replace("127.0.0.1", "localhost");
  }
  return "";
}

function parseJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function bindNavigation() {
  $$(".nav-tab").forEach((button) => {
    button.addEventListener("click", () => {
      $$(".nav-tab").forEach((tab) => tab.classList.remove("active"));
      $$(".view").forEach((view) => view.classList.remove("active-view"));
      button.classList.add("active");
      $(`#${button.dataset.view}`).classList.add("active-view");
    });
  });

  $$(".mode-btn").forEach((button) => {
    button.addEventListener("click", () => {
      state.chatMode = button.dataset.mode;
      $$(".mode-btn").forEach((item) => item.classList.remove("active"));
      button.classList.add("active");
    });
  });
}

function bindAuth() {
  $("#loginBtn").addEventListener("click", async () => {
    const payload = readCredentials();
    if (!payload) return;
    try {
      const data = await api("/auth/login", {
        method: "POST",
        auth: false,
        body: JSON.stringify(payload),
      });
      state.token = data.token;
      state.user = { id: data.id, username: data.username, role: data.role };
      localStorage.setItem("psychAgentToken", state.token);
      localStorage.setItem("psychAgentUser", JSON.stringify(state.user));
      syncConnectionLabel();
      toast("登录成功");
      await refreshProfile();
    } catch (error) {
      toast(error.message);
    }
  });

  $("#registerBtn").addEventListener("click", async () => {
    const payload = readCredentials();
    if (!payload) return;
    try {
      await api("/auth/register", {
        method: "POST",
        auth: false,
        body: JSON.stringify({ ...payload, role: "USER" }),
      });
      toast("注册成功，可以登录了");
    } catch (error) {
      toast(error.message);
    }
  });
}

function readCredentials() {
  const username = $("#username").value.trim();
  const password = $("#password").value;
  if (!username || !password) {
    toast("请输入账号和密码");
    return null;
  }
  return { username, password };
}

function bindChat() {
  $("#chatForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const input = $("#messageInput");
    const message = input.value.trim();
    if (!message) return;
    addMessage("user", message);
    input.value = "";

    try {
      const path = state.chatMode === "agent" ? "/agent/chat" : "/message";
      const data = await api(path, {
        method: "POST",
        body: JSON.stringify({ message }),
      });
      addMessage("assistant", data.reply || data);
      if (data.userProfile) {
        fillProfile(data.userProfile);
      }
    } catch (error) {
      addMessage("meta", error.message);
    }
  });
}

function addMessage(role, content) {
  const item = document.createElement("div");
  item.className = `message ${role}`;
  item.textContent = content;
  $("#chatLog").appendChild(item);
  $("#chatLog").scrollTop = $("#chatLog").scrollHeight;
}

function bindProfile() {
  $("#refreshProfileBtn").addEventListener("click", refreshProfile);
  $("#profileForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      const data = await api("/profile", {
        method: "PUT",
        body: JSON.stringify(readProfileForm()),
      });
      fillProfile(data);
      toast("画像已保存");
    } catch (error) {
      toast(error.message);
    }
  });
}

async function refreshProfile() {
  try {
    const data = await api("/profile");
    fillProfile(data);
  } catch (error) {
    toast(error.message);
  }
}

function readProfileForm() {
  return {
    profileSummary: $("#profileSummary").value,
    concerns: $("#concerns").value,
    preferences: $("#preferences").value,
    copingStrategies: $("#copingStrategies").value,
    riskSignals: $("#riskSignals").value,
    supportGoals: $("#supportGoals").value,
  };
}

function fillProfile(profile) {
  $("#profileSummary").value = profile.profileSummary || "";
  $("#concerns").value = profile.concerns || "";
  $("#preferences").value = profile.preferences || "";
  $("#copingStrategies").value = profile.copingStrategies || "";
  $("#riskSignals").value = profile.riskSignals || "";
  $("#supportGoals").value = profile.supportGoals || "";
}

function bindReports() {
  $("#refreshReportsBtn").addEventListener("click", refreshReports);
}

async function refreshReports() {
  try {
    const dashboard = await api("/reports/dashboard?recentLimit=10&topRiskLimit=5");
    const reports = dashboard.reportSummaryResponse || [];
    const highCount = reports.filter((item) => item.risk === "high").length;
    $("#dashboardGrid").innerHTML = [
      metric("最近报告", reports.length),
      metric("高风险", highCount),
      metric("风险用户", dashboard.topRiskUserResponse?.length || 0),
    ].join("");
    $("#reportsList").innerHTML = reports.length
      ? reports.map(reportItem).join("")
      : empty("暂无报告");
  } catch (error) {
    toast(error.message);
  }
}

function metric(label, value) {
  return `<div class="metric"><span>${escapeHtml(label)}</span><strong>${escapeHtml(String(value))}</strong></div>`;
}

function reportItem(item) {
  return `
    <article class="list-item">
      <h3>${escapeHtml(item.emotion || "未知情绪")}</h3>
      <p>
        <span class="tag ${escapeHtml(item.risk || "")}">${escapeHtml(item.risk || "unknown")}</span>
        <span class="tag">${escapeHtml(formatTime(item.createdAt))}</span>
      </p>
      <p>${escapeHtml(item.message || "")}</p>
    </article>
  `;
}

function bindKnowledge() {
  $("#refreshKnowledgeBtn").addEventListener("click", loadKnowledge);
  $("#searchKnowledgeBtn").addEventListener("click", searchKnowledge);
  $("#knowledgeForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const payload = {
      category: $("#knowledgeCategory").value.trim(),
      source: $("#knowledgeSource").value.trim(),
      content: $("#knowledgeContent").value.trim(),
    };
    if (!payload.content) {
      toast("请输入知识内容");
      return;
    }
    try {
      await api("/knowledge/add", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      $("#knowledgeContent").value = "";
      toast("知识已添加");
      await loadKnowledge();
    } catch (error) {
      toast(error.message);
    }
  });
}

async function loadKnowledge() {
  try {
    const data = await api("/knowledge/all");
    renderKnowledge(data || []);
  } catch (error) {
    toast(error.message);
  }
}

async function searchKnowledge() {
  const query = $("#knowledgeQuery").value.trim();
  if (!query) {
    await loadKnowledge();
    return;
  }
  try {
    const data = await api(`/knowledge/search?query=${encodeURIComponent(query)}`);
    renderKnowledge(data || []);
  } catch (error) {
    toast(error.message);
  }
}

function renderKnowledge(items) {
  $("#knowledgeResults").innerHTML = items.length
    ? items.map(knowledgeItem).join("")
    : empty("暂无知识");
}

function knowledgeItem(item) {
  return `
    <article class="list-item">
      <h3>${escapeHtml(item.category || "未分类")}</h3>
      <p>
        <span class="tag">${escapeHtml(item.source || "unknown")}</span>
        ${item.score !== undefined ? `<span class="tag">${escapeHtml(Number(item.score).toFixed(2))}</span>` : ""}
      </p>
      <p>${escapeHtml(item.content || "")}</p>
    </article>
  `;
}

function bindSettings() {
  $("#saveConfigBtn").addEventListener("click", () => {
    state.apiBaseUrl = $("#apiBaseUrl").value.trim() || config.apiBaseUrl;
    localStorage.setItem("psychAgentApiBaseUrl", state.apiBaseUrl);
    syncConnectionLabel();
    toast("API 地址已保存");
  });
}

function hydrateSettings() {
  $("#apiBaseUrl").value = state.apiBaseUrl;
  $("#mysqlConfig").value = config.localServices?.mysql || "";
  $("#redisConfig").value = config.localServices?.redis || "";
  $("#ollamaConfig").value = config.localServices?.ollama || "";
  $("#chromaConfig").value = config.localServices?.chroma || "";
}

function syncConnectionLabel() {
  const label = state.user ? `${state.user.username} · ${state.apiBaseUrl}` : state.apiBaseUrl;
  $("#connectionLabel").textContent = label;
}

function empty(text) {
  return `<div class="list-item"><p>${escapeHtml(text)}</p></div>`;
}

function formatTime(value) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function toast(message) {
  const element = $("#toast");
  element.textContent = message;
  element.classList.add("visible");
  window.clearTimeout(toast.timer);
  toast.timer = window.setTimeout(() => element.classList.remove("visible"), 2600);
}

init();
