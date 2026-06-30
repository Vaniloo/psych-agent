const config = window.PSYCH_AGENT_CONFIG || {};
const state = {
  apiBaseUrl: localStorage.getItem("psychAgentApiBaseUrl") || config.apiBaseUrl || "http://localhost:8080",
  token: localStorage.getItem("psychAgentToken") || "",
  user: JSON.parse(localStorage.getItem("psychAgentUser") || "null"),
  chatMode: "agent",
  currentSessionId: null,
  profiles: [],
  adminMemories: [],
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

function init() {
  normalizeStoredSession();
  bindNavigation();
  bindAuth();
  bindChat();
  bindRoleCards();
  bindProfile();
  bindReports();
  bindHelp();
  bindKnowledge();
  bindSettings();
  bindToolDebug();
  syncConnectionLabel();
  syncAuthUi();
  applyRoleVisibility();
  hydrateSettings();
  addMessage("assistant", "你好，我是 Psych Agent。登录后可以开始对话、查看画像和报告。");
  if (state.token) {
    loadConversations();
    loadRoleCards();
    if (isAdmin()) {
      refreshProfile();
      loadTools();
    }
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
    if (response.status === 401 && options.headers?.Authorization) {
      clearSession(false);
    }
    if (!response.ok || data?.success === false) {
      throw new Error(data?.message || text || `请求失败：${response.status}`);
    }
    return data ?? text;
  } catch (error) {
    const fallbackUrl = getLoopbackFallbackUrl(state.apiBaseUrl);
    if (error instanceof TypeError && allowFallback && fallbackUrl) {
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
      $("#password").value = "";
      syncConnectionLabel();
      syncAuthUi();
      applyRoleVisibility();
      toast("登录成功");
      await loadConversations();
      await loadRoleCards();
      if (isAdmin()) {
        await refreshProfile();
        await loadTools();
      }
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
      $("#password").value = "";
      toast("注册成功，可以登录了");
    } catch (error) {
      toast(error.message);
    }
  });

  $("#logoutBtn").addEventListener("click", () => {
    clearSession(true);
  });
}

function normalizeStoredSession() {
  if (state.token && state.user) return;
  state.token = "";
  state.user = null;
  localStorage.removeItem("psychAgentToken");
  localStorage.removeItem("psychAgentUser");
}

function clearSession(showToast) {
  const hadSession = Boolean(state.token || state.user);
  state.token = "";
  state.user = null;
  state.currentSessionId = null;
  localStorage.removeItem("psychAgentToken");
  localStorage.removeItem("psychAgentUser");
  $("#username").value = "";
  $("#password").value = "";
  $("#conversationList").innerHTML = "";
  $("#chatLog").innerHTML = "";
  addMessage("assistant", "你好，我是 Psych Agent。登录后可以开始对话、查看画像和报告。");
  syncConnectionLabel();
  syncAuthUi();
  applyRoleVisibility();
  if (showToast && hadSession) {
    toast("已退出登录");
  }
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
  $("#newChatBtn").addEventListener("click", startNewConversation);
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
        body: JSON.stringify({ message, sessionId: state.chatMode === "agent" ? state.currentSessionId : null }),
      });
      if (data.sessionId) {
        state.currentSessionId = data.sessionId;
      }
      addMessage("assistant", data.reply || data);
      if (data.usedTool && data.toolName) {
        addMessage("meta", `已调用工具：${data.toolName}`);
      }
      if (data.crisis) {
        addCrisisCard(data.resources || []);
      }
      await loadConversations();
    } catch (error) {
      addMessage("meta", error.message);
    }
  });
}

function bindRoleCards() {
  $("#refreshRoleCardsBtn").addEventListener("click", loadRoleCards);
  $("#roleCardForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await api("/role-cards", {
        method: "POST",
        body: JSON.stringify({
          name: $("#roleCardName").value.trim(),
          description: $("#roleCardDescription").value.trim(),
          tone: $("#roleCardTone").value,
          responseStyle: $("#roleCardResponseStyle").value,
          customInstructions: $("#roleCardInstructions").value.trim(),
          forbiddenExpressions: $("#roleCardForbidden").value.trim(),
        }),
      });
      event.target.reset();
      await loadRoleCards();
      toast("角色卡已创建");
    } catch (error) {
      toast(error.message);
    }
  });
}

async function loadRoleCards() {
  if (!state.token) return;
  try {
    const cards = await api("/role-cards");
    $("#roleCardList").innerHTML = cards.map((card) => `
      <article class="role-card-item ${card.active ? "active" : ""}">
        <h3>${escapeHtml(card.name)}</h3>
        <p>${escapeHtml(card.description)}</p>
        <p><span class="tag">${escapeHtml(card.tone)}</span><span class="tag">${escapeHtml(card.responseStyle)}</span></p>
        <button data-role-card-id="${card.id}" type="button" ${card.active ? "disabled" : ""}>${card.active ? "当前使用" : "启用"}</button>
      </article>
    `).join("");
    $$('[data-role-card-id]').forEach((button) => {
      button.addEventListener("click", () => activateRoleCard(Number(button.dataset.roleCardId)));
    });
  } catch (error) {
    $("#roleCardList").innerHTML = empty(error.message);
  }
}

async function activateRoleCard(roleCardId) {
  try {
    await api(`/role-cards/${roleCardId}/activate`, { method: "POST" });
    await loadRoleCards();
    toast("角色卡已启用");
  } catch (error) {
    toast(error.message);
  }
}

function startNewConversation() {
  state.currentSessionId = null;
  $("#chatLog").innerHTML = "";
  addMessage("assistant", "新的对话已开始。");
  renderConversationList();
}

function addMessage(role, content) {
  const item = document.createElement("div");
  item.className = `message ${role}`;
  item.textContent = content;
  $("#chatLog").appendChild(item);
  $("#chatLog").scrollTop = $("#chatLog").scrollHeight;
}

function addCrisisCard(resources) {
  const card = document.createElement("div");
  card.className = "message crisis-card";
  card.innerHTML = `
    <strong>请优先确保当前安全</strong>
    <div class="crisis-actions">
      ${resources.map((resource) => `<a class="tag ${resource.urgent ? "high" : ""}" href="tel:${escapeHtml(resource.contact)}">${escapeHtml(resource.name)} ${escapeHtml(resource.contact)}</a>`).join("")}
    </div>
  `;
  $("#chatLog").appendChild(card);
}

function bindProfile() {
  $("#refreshProfileBtn").addEventListener("click", refreshProfile);
  $("#profileUserSelect").addEventListener("change", () => {
    const memory = state.adminMemories.find((item) => item.username === $("#profileUserSelect").value);
    if (memory) {
      fillAdminMemory(memory);
    }
  });
  $("#profileForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const username = $("#profileUserSelect").value;
    if (!username) {
      toast("请选择用户");
      return;
    }
    try {
      const data = await api(`/profile/${encodeURIComponent(username)}`, {
        method: "PUT",
        body: JSON.stringify(readProfileForm()),
      });
      state.profiles = state.profiles.map((profile) => profile.username === data.username ? data : profile);
      state.adminMemories = state.adminMemories.map((memory) =>
        memory.username === data.username ? { ...memory, profile: data } : memory
      );
      fillProfile(data);
      toast("画像已保存");
    } catch (error) {
      toast(error.message);
    }
  });
}

async function refreshProfile() {
  if (!isAdmin()) return;
  try {
    state.adminMemories = await api("/admin/memories");
    state.profiles = state.adminMemories.map((memory) => memory.profile);
    renderProfileUsers();
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
  if (profile.username) {
    $("#profileUserSelect").value = profile.username;
  }
  $("#profileSummary").value = profile.profileSummary || "";
  $("#concerns").value = profile.concerns || "";
  $("#preferences").value = profile.preferences || "";
  $("#copingStrategies").value = profile.copingStrategies || "";
  $("#riskSignals").value = profile.riskSignals || "";
  $("#supportGoals").value = profile.supportGoals || "";
}

function fillAdminMemory(memory) {
  fillProfile(memory.profile || {});
  $("#memorySummary").value = memory.summary || "";
  $("#longTermMemory").value = memory.longTermMemory || "";
  renderAdminSessions(memory);
  $("#adminMessageList").innerHTML = empty("选择一个会话查看消息");
}

function renderProfileUsers() {
  $("#profileUserSelect").innerHTML = state.adminMemories
    .map((memory) => `<option value="${escapeHtml(memory.username)}">${escapeHtml(memory.username)}</option>`)
    .join("");
  if (state.adminMemories.length > 0) {
    fillAdminMemory(state.adminMemories[0]);
  }
}

function renderAdminSessions(memory) {
  const sessions = memory.sessions || [];
  $("#adminSessionList").innerHTML = sessions.length
    ? sessions.map((session) => `
      <button class="conversation-item" data-admin-session-id="${session.id}" data-admin-username="${escapeHtml(memory.username)}" type="button">
        ${escapeHtml(session.title || "未命名对话")}
        <span>${escapeHtml(formatTime(session.updatedAt))}</span>
      </button>
      <div class="list-item"><p>${escapeHtml(session.summary || "暂无会话摘要")}</p></div>
    `).join("")
    : empty("暂无会话");
  $$("[data-admin-session-id]").forEach((button) => {
    button.addEventListener("click", () => loadAdminMessages(button.dataset.adminUsername, Number(button.dataset.adminSessionId)));
  });
}

async function loadAdminMessages(username, sessionId) {
  try {
    const messages = await api(`/admin/memories/${encodeURIComponent(username)}/sessions/${sessionId}/messages`);
    $("#adminMessageList").innerHTML = messages.length
      ? messages.map((message) => `
        <article class="list-item">
          <h3>${escapeHtml(message.role === "assistant" ? "助手" : "用户")}</h3>
          <p>${escapeHtml(formatTime(message.createdAt))}</p>
          <p>${escapeHtml(message.content || "")}</p>
        </article>
      `).join("")
      : empty("暂无消息");
  } catch (error) {
    toast(error.message);
  }
}

async function loadConversations() {
  if (!state.token) return;
  try {
    state.conversations = await api("/conversations");
    renderConversationList();
  } catch (error) {
    toast(error.message);
  }
}

function renderConversationList() {
  const conversations = state.conversations || [];
  $("#conversationList").innerHTML = conversations.length
    ? conversations.map((session) => `
      <button class="conversation-item ${session.id === state.currentSessionId ? "active" : ""}" data-session-id="${session.id}" type="button">
        ${escapeHtml(session.title || "未命名对话")}
        <span>${escapeHtml(formatTime(session.updatedAt))}</span>
      </button>
    `).join("")
    : `<div class="list-item"><p>暂无历史</p></div>`;
  $$(".conversation-item").forEach((button) => {
    button.addEventListener("click", () => continueConversation(Number(button.dataset.sessionId)));
  });
}

async function continueConversation(sessionId) {
  try {
    const messages = await api(`/conversations/${sessionId}/messages`);
    state.currentSessionId = sessionId;
    $("#chatLog").innerHTML = "";
    messages.forEach((message) => addMessage(message.role === "assistant" ? "assistant" : "user", message.content));
    renderConversationList();
  } catch (error) {
    toast(error.message);
  }
}

function bindReports() {
  $("#refreshReportsBtn").addEventListener("click", refreshReports);
}

function bindHelp() {
  $("#refreshHelpBtn").addEventListener("click", loadHelpResources);
  loadHelpResources();
}

async function loadHelpResources() {
  try {
    const resources = await api("/help/resources", { auth: false });
    $("#helpResources").innerHTML = resources.map((resource) => `
      <article class="help-resource ${resource.urgent ? "urgent" : ""}">
        <h3>${escapeHtml(resource.name)}</h3>
        <span class="help-contact">${escapeHtml(resource.contact)}</span>
        <p>${escapeHtml(resource.description)}</p>
      </article>
    `).join("");
  } catch (error) {
    $("#helpResources").innerHTML = empty(error.message);
  }
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
    renderRiskDistribution(dashboard.riskDistribution || []);
    renderEmotionTrend(dashboard.emotionTrend || []);
    $("#reportsList").innerHTML = reports.length
      ? reports.map(reportItem).join("")
      : empty("暂无报告");
  } catch (error) {
    toast(error.message);
  }
}

function renderRiskDistribution(items) {
  const max = Math.max(1, ...items.map((item) => item.count));
  $("#riskDistribution").innerHTML = items.length
    ? items.map((item) => `
      <div class="bar-row">
        <span class="tag ${escapeHtml(item.risk)}">${escapeHtml(item.risk)}</span>
        <div class="bar-track"><div class="bar-fill" style="width:${Math.round(item.count / max * 100)}%"></div></div>
        <strong>${item.count}</strong>
      </div>
    `).join("")
    : empty("暂无风险数据");
}

function renderEmotionTrend(items) {
  $("#emotionTrend").innerHTML = items.length
    ? items.map((item) => `
      <div class="trend-point">
        <span>${escapeHtml(item.date)}</span>
        <span>${escapeHtml(item.emotion)}</span>
        <span class="tag">${item.count} 次</span>
      </div>
    `).join("")
    : empty("暂无趋势数据");
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
  if (!isAdmin()) return;
  try {
    const data = await api("/knowledge/all");
    renderKnowledge(data || []);
  } catch (error) {
    toast(error.message);
  }
}

async function searchKnowledge() {
  if (!isAdmin()) return;
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
        ${item.relevanceScore !== undefined && item.relevanceScore !== null ? `<span class="tag">相关度 ${escapeHtml(Number(item.relevanceScore).toFixed(2))}</span>` : ""}
        ${item.matchReason ? `<span class="tag">${escapeHtml(item.matchReason)}</span>` : ""}
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

function bindToolDebug() {
  $("#refreshToolsBtn").addEventListener("click", loadTools);
  $("#toolSelect").addEventListener("change", renderSelectedToolSchema);
  $("#toolDebugForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      const args = JSON.parse($("#toolArguments").value || "{}");
      const result = await api("/tools/call", {
        method: "POST",
        body: JSON.stringify({ tool: $("#toolSelect").value, arguments: args }),
      });
      $("#toolDebugOutput").textContent = JSON.stringify(result, null, 2);
    } catch (error) {
      $("#toolDebugOutput").textContent = error.message;
    }
  });
}

async function loadTools() {
  if (!isAdmin()) return;
  try {
    state.tools = await api("/tools");
    $("#toolSelect").innerHTML = state.tools.map((tool) => `<option value="${escapeHtml(tool.name)}">${escapeHtml(tool.name)}</option>`).join("");
    renderSelectedToolSchema();
  } catch (error) {
    $("#toolDebugOutput").textContent = error.message;
  }
}

function renderSelectedToolSchema() {
  const tool = (state.tools || []).find((item) => item.name === $("#toolSelect").value);
  $("#toolDebugOutput").textContent = tool ? JSON.stringify(tool, null, 2) : "暂无工具";
}

function applyRoleVisibility() {
  const admin = isAdmin();
  $$(".admin-only").forEach((element) => element.classList.toggle("hidden", !admin));
  if (!admin && $(".nav-tab.active")?.classList.contains("admin-only")) {
    $(".nav-tab[data-view='chatView']").click();
  }
}

function isAdmin() {
  return state.user?.role === "ADMIN";
}

function hydrateSettings() {
  $("#apiBaseUrl").value = state.apiBaseUrl;
  $("#mysqlConfig").value = config.localServices?.mysql || "";
  $("#redisConfig").value = config.localServices?.redis || "";
  $("#ollamaConfig").value = config.localServices?.ollama || "";
  $("#chromaConfig").value = config.localServices?.chroma || "";
}

function syncConnectionLabel() {
  const label = state.token && state.user
    ? `${state.user.username} · ${state.apiBaseUrl}`
    : state.apiBaseUrl;
  $("#connectionLabel").textContent = label;
}

function syncAuthUi() {
  const loggedIn = Boolean(state.token && state.user);
  $("#authFields").classList.toggle("hidden", loggedIn);
  $("#sessionPanel").classList.toggle("hidden", !loggedIn);
  $("#sessionUsername").textContent = loggedIn ? state.user.username : "";
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
