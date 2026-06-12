const state = {
  status: null,
  token: localStorage.getItem("codexCompanion.token") || "",
  deviceId: localStorage.getItem("codexCompanion.deviceId") || "",
  threads: [],
  filteredThreads: [],
  selectedThread: null,
  events: [],
  eventFilter: "messages",
  socket: null,
  pairInfo: null,
  collapsedProjects: readCollapsedProjects()
};

const els = {
  bridgeSubtitle: document.querySelector("#bridgeSubtitle"),
  refreshButton: document.querySelector("#refreshButton"),
  searchInput: document.querySelector("#searchInput"),
  threadCount: document.querySelector("#threadCount"),
  activeCount: document.querySelector("#activeCount"),
  threadList: document.querySelector("#threadList"),
  threadTitle: document.querySelector("#threadTitle"),
  threadMeta: document.querySelector("#threadMeta"),
  emptyState: document.querySelector("#emptyState"),
  eventList: document.querySelector("#eventList"),
  taskStatus: document.querySelector("#taskStatus"),
  messageCount: document.querySelector("#messageCount"),
  toolCount: document.querySelector("#toolCount"),
  errorCount: document.querySelector("#errorCount"),
  quotaUsed: document.querySelector("#quotaUsed"),
  tokenUsage: document.querySelector("#tokenUsage"),
  bridgeAddress: document.querySelector("#bridgeAddress"),
  pairingStatus: document.querySelector("#pairingStatus"),
  emulatorAddress: document.querySelector("#emulatorAddress"),
  lanAddress: document.querySelector("#lanAddress"),
  publicAddress: document.querySelector("#publicAddress"),
  pairToken: document.querySelector("#pairToken"),
  copyResult: document.querySelector("#copyResult"),
  refreshPairButton: document.querySelector("#refreshPairButton"),
  copyEmulatorAddressButton: document.querySelector("#copyEmulatorAddressButton"),
  copyLanAddressButton: document.querySelector("#copyLanAddressButton"),
  copyPublicAddressButton: document.querySelector("#copyPublicAddressButton"),
  copyPairTokenButton: document.querySelector("#copyPairTokenButton"),
  appServerStatus: document.querySelector("#appServerStatus"),
  codexHomeCard: document.querySelector("#codexHomeCard"),
  codexHomeStatus: document.querySelector("#codexHomeStatus"),
  codexHomePath: document.querySelector("#codexHomePath"),
  codexHomeForm: document.querySelector("#codexHomeForm"),
  codexHomeInput: document.querySelector("#codexHomeInput"),
  saveCodexHomeButton: document.querySelector("#saveCodexHomeButton"),
  codexHomeResult: document.querySelector("#codexHomeResult"),
  codexHomeCandidates: document.querySelector("#codexHomeCandidates"),
  backendWarning: document.querySelector("#backendWarning"),
  appServerError: document.querySelector("#appServerError"),
  appServerEvents: document.querySelector("#appServerEvents"),
  messageInput: document.querySelector("#messageInput"),
  sendButton: document.querySelector("#sendButton"),
  interruptButton: document.querySelector("#interruptButton"),
  operationResult: document.querySelector("#operationResult"),
  disconnectButton: document.querySelector("#disconnectButton"),
  openAppButton: document.querySelector("#openAppButton"),
  filterButtons: [...document.querySelectorAll(".filter-chip")]
};

els.refreshButton.addEventListener("click", refreshThreads);
els.searchInput.addEventListener("input", applyFilter);
els.sendButton.addEventListener("click", sendMessage);
els.interruptButton.addEventListener("click", interruptThread);
els.disconnectButton.addEventListener("click", resetConnection);
els.openAppButton.addEventListener("click", () => window.open("/app", "_blank"));
els.refreshPairButton.addEventListener("click", refreshPairInfo);
els.codexHomeForm.addEventListener("submit", saveCodexHome);
els.copyEmulatorAddressButton.addEventListener("click", () => copyText(els.emulatorAddress.textContent, "已复制模拟器地址"));
els.copyLanAddressButton.addEventListener("click", () => copyText(els.lanAddress.textContent, "已复制真机地址"));
els.copyPublicAddressButton.addEventListener("click", () => copyText(els.publicAddress.textContent, "已复制公网/虚拟网地址"));
els.copyPairTokenButton.addEventListener("click", () => copyText(els.pairToken.textContent, "已复制配对码"));
els.messageInput.addEventListener("keydown", (event) => {
  if (event.ctrlKey && event.key === "Enter") sendMessage();
});
for (const button of els.filterButtons) {
  button.addEventListener("click", () => {
    if (button.hidden) return;
    state.eventFilter = button.dataset.filter || "all";
    syncFilterButtons();
    renderEvents();
  });
}

await boot();

async function boot() {
  await refreshStatus();
  if (!state.token) await autoPair();
  await refreshPairInfo();
  await refreshThreads();
  setInterval(refreshStatus, 5000);
  setInterval(refreshPairInfo, 30_000);
}

async function refreshStatus() {
  try {
    state.status = await api("/health", { auth: false });
    els.bridgeSubtitle.textContent = state.status.bridgeName;
    const lanAddress = buildBestAddress(state.status);
    els.bridgeAddress.textContent = lanAddress;
    els.lanAddress.textContent = lanAddress;
    const virtualAddr = state.status.publicUrl || state.status.virtualAddress;
    els.publicAddress.textContent = virtualAddr || "未配置";
    if (!virtualAddr) {
      els.publicAddress.classList.add("muted");
    } else {
      els.publicAddress.classList.remove("muted");
    }
    els.copyPublicAddressButton.disabled = !virtualAddr;
    els.emulatorAddress.textContent = `http://10.0.2.2:${state.status.port}`;
    els.appServerStatus.textContent = state.status.codexAppServer.available ? "可用" : "不可用";
    renderCodexHomeStatus(state.status.codexHome);
    const warning = extractWarning(state.status.codexAppServer.lastError);
    els.backendWarning.textContent = warning || "-";
    els.appServerError.textContent = warning ? "上方显示的是最近一次 Codex 后端警告。" : "";
    renderAppServerEvents(state.status.codexAppServer.recentEvents || []);
    els.sendButton.disabled = !state.selectedThread;
    els.interruptButton.disabled = !state.selectedThread;
  } catch (error) {
    els.bridgeSubtitle.textContent = "Bridge 离线";
    els.appServerStatus.textContent = "未知";
    renderCodexHomeStatus(null);
    els.appServerError.textContent = error.message;
  }
}

function renderCodexHomeStatus(codexHome) {
  if (!codexHome) {
    els.codexHomeCard.classList.add("warning");
    els.codexHomeStatus.textContent = "未知";
    els.codexHomePath.textContent = "-";
    els.codexHomeCandidates.replaceChildren();
    return;
  }

  els.codexHomeCard.classList.toggle("warning", !codexHome.detected);
  els.codexHomeStatus.textContent = codexHome.detected ? sourceLabel(codexHome.source) : "需要选择";
  els.codexHomePath.textContent = codexHome.path || "-";
  els.codexHomeInput.placeholder = codexHome.detected ? "可粘贴新的 .codex 路径后保存" : "粘贴 Codex 数据目录，例如 C:\\Users\\你的用户名\\.codex";
  if (!els.codexHomeInput.value) els.codexHomeInput.value = "";
  if (!codexHome.detected && !els.codexHomeResult.textContent) {
    els.codexHomeResult.textContent = "没有自动找到 Codex 数据目录，请从资源管理器复制 .codex 文件夹路径到这里。";
  }

  const candidates = codexHome.candidates || [];
  els.codexHomeCandidates.replaceChildren(
    ...candidates.map((candidate) => {
      const row = document.createElement("div");
      row.className = `diagnostic-item ${candidate.valid ? "" : "error"}`;
      row.innerHTML = `
        <span>${candidate.valid ? "可用" : candidate.exists ? "不可用" : "不存在"}</span>
        <strong>${escapeHtml(candidate.path || "-")}</strong>
        <p>${escapeHtml(candidate.reason || "已找到 Codex 数据。")}</p>
      `;
      return row;
    })
  );
}

async function saveCodexHome(event) {
  event.preventDefault();
  const codexHome = els.codexHomeInput.value.trim();
  if (!codexHome) {
    els.codexHomeResult.textContent = "请先粘贴 Codex 数据目录路径。";
    return;
  }
  els.saveCodexHomeButton.disabled = true;
  els.codexHomeResult.textContent = "正在检查目录...";
  try {
    const result = await api("/settings/codex-home", {
      method: "POST",
      body: { codexHome }
    });
    renderCodexHomeStatus(result.codexHome);
    els.codexHomeInput.value = "";
    els.codexHomeResult.textContent = "已保存，会话列表正在刷新。";
    await refreshStatus();
    await refreshThreads();
  } catch (error) {
    els.codexHomeResult.textContent = error.message;
  } finally {
    els.saveCodexHomeButton.disabled = false;
  }
}

async function autoPair() {
  const pairInfo = await api("/pair", { auth: false });
  const paired = await api("/pair", {
    auth: false,
    method: "POST",
    body: { pairToken: pairInfo.pairToken, deviceName: "电脑浏览器" }
  });
  state.token = paired.authToken;
  state.deviceId = paired.deviceId;
  localStorage.setItem("codexCompanion.token", state.token);
  localStorage.setItem("codexCompanion.deviceId", state.deviceId);
}

async function refreshPairInfo() {
  try {
    state.pairInfo = await api("/pair", { auth: false });
    els.pairToken.textContent = state.pairInfo.pairToken;
    els.pairingStatus.textContent = `有效期到 ${formatTime(state.pairInfo.expiresAt)}`;
    els.copyPairTokenButton.disabled = false;
  } catch (error) {
    els.pairingStatus.textContent = "配对码获取失败";
    els.pairToken.textContent = "-";
    els.copyPairTokenButton.disabled = true;
    showCopyResult(error.message);
  }
}

async function refreshThreads() {
  if (!state.token) return;
  try {
    state.threads = await api("/threads");
    applyFilter();
    if (!state.selectedThread && state.threads.length > 0) {
      await selectThread(state.threads[0].id);
    }
  } catch (error) {
    if (String(error.message).includes("AUTH_REQUIRED") || String(error.message).includes("401")) {
      resetConnection();
      await autoPair();
      await refreshThreads();
      return;
    }
    showOperation(error.message);
  }
}

function applyFilter() {
  const query = els.searchInput.value.trim().toLowerCase();
  state.filteredThreads = query
    ? state.threads.filter((thread) => `${thread.title} ${thread.preview} ${thread.cwd || ""}`.toLowerCase().includes(query))
    : [...state.threads];
  renderThreads();
}

function renderThreadsLegacy() {
  const active = state.threads.filter((thread) => thread.status === "active").length;
  els.threadCount.textContent = `${state.filteredThreads.length} 个会话`;
  els.activeCount.textContent = `${active} 个运行中`;
  els.threadList.replaceChildren(
    ...state.filteredThreads.map((thread) => {
      const item = document.createElement("button");
      item.className = `thread-item ${state.selectedThread?.id === thread.id ? "active" : ""}`;
      item.addEventListener("click", () => selectThread(thread.id));
      item.innerHTML = `
        <div class="thread-title">
          <span class="status-dot ${escapeHtml(thread.status)}"></span>
          <span>${escapeHtml(thread.title)}</span>
        </div>
        <div class="thread-preview">${escapeHtml(thread.preview || "")}</div>
        <div class="thread-meta">${escapeHtml(thread.model || "未知模型")} | ${formatTime(thread.updatedAt)}</div>
      `;
      return item;
    })
  );
}

async function selectThread(threadId) {
  closeSocket();
  const detail = await api(`/threads/${encodeURIComponent(threadId)}/events`);
  state.selectedThread = detail.thread;
  state.events = detail.events;
  renderSelectedThread();
  renderThreads();
  openSocket(threadId);
}

function renderSelectedThread() {
  const thread = state.selectedThread;
  els.emptyState.style.display = thread ? "none" : "block";
  els.eventList.style.display = thread ? "flex" : "none";

  if (!thread) {
    els.threadTitle.textContent = "选择一个会话";
    els.threadMeta.textContent = "历史记录、任务状态和对话操作都会显示在这里。";
    els.sendButton.disabled = true;
    els.interruptButton.disabled = true;
    return;
  }

  els.threadTitle.textContent = thread.title;
  els.threadMeta.textContent = `${thread.cwd || "未记录工作目录"} | ${thread.model || "未知模型"} | ${formatTime(thread.updatedAt)}`;
  els.sendButton.disabled = false;
  els.interruptButton.disabled = false;
  updateFilterVisibility();
  renderEvents();
  renderTaskStats();
}

function renderEvents() {
  const events = filteredEvents();
  const fragment = document.createDocumentFragment();
  if (!events.length) {
    const node = document.createElement("div");
    node.className = "inline-empty";
    node.textContent = emptyTextForFilter();
    fragment.appendChild(node);
  }
  for (const event of events) {
    const node = document.createElement("article");
    const collapsible = shouldCollapse(event);
    node.className = `event ${event.kind} ${isCompletedEvent(event) ? "completed" : ""} ${collapsible ? "collapsed" : ""}`;
    node.innerHTML = `
      <div class="event-head">
        <span class="event-title">${escapeHtml(event.title || labelForKind(event.kind))}</span>
        <span class="event-time">${formatTime(event.timestamp)}</span>
      </div>
      <pre class="event-text">${escapeHtml(event.text || "")}</pre>
    `;
    if (collapsible) {
      const toggle = document.createElement("button");
      toggle.className = "event-toggle";
      toggle.textContent = "展开";
      toggle.addEventListener("click", () => {
        node.classList.toggle("collapsed");
        toggle.textContent = node.classList.contains("collapsed") ? "展开" : "收起";
      });
      node.querySelector(".event-head").appendChild(toggle);
    }
    fragment.appendChild(node);
  }
  els.eventList.replaceChildren(fragment);
  els.eventList.lastElementChild?.scrollIntoView({ block: "nearest" });
}

function filteredEvents() {
  if (state.eventFilter === "all") return state.events;
  if (state.eventFilter === "messages") {
    return state.events.filter((event) => event.kind === "user_message" || event.kind === "assistant_message" || isCompletedEvent(event));
  }
  if (state.eventFilter === "tools") {
    return state.events.filter((event) => event.kind === "tool_call" || event.kind === "tool_result");
  }
  return state.events.filter((event) => event.kind === state.eventFilter);
}

function updateFilterVisibility() {
  const reasoningButton = els.filterButtons.find((button) => button.dataset.filter === "reasoning_summary");
  const hasReasoning = state.events.some((event) => event.kind === "reasoning_summary" && String(event.text || "").trim());
  if (reasoningButton) reasoningButton.hidden = !hasReasoning;
  if (!hasReasoning && state.eventFilter === "reasoning_summary") state.eventFilter = "messages";
  syncFilterButtons();
}

function syncFilterButtons() {
  for (const item of els.filterButtons) {
    item.classList.toggle("active", item.dataset.filter === state.eventFilter);
  }
}

function emptyTextForFilter() {
  return {
    all: "当前会话还没有记录。",
    messages: "当前会话还没有对话内容。",
    reasoning_summary: "当前会话没有可展示的思考摘要。",
    tools: "当前会话还没有工具调用。",
    status: "当前会话还没有状态记录。",
    error: "当前会话还没有错误。"
  }[state.eventFilter] || "当前筛选没有内容。";
}

function renderTaskStats() {
  const events = state.events;
  const lastStarted = findLastIndex(events, (event) => event.title === "任务开始" || event.title === "Task started");
  const lastComplete = findLastIndex(events, (event) => event.title === "任务完成" || event.title === "Task complete");
  const errors = events.filter((event) => event.kind === "error").length;
  const active = errors === 0 && lastStarted > lastComplete;
  els.taskStatus.textContent = errors > 0 ? "有错误" : active ? pendingLabel(events[lastStarted]) : "空闲";
  els.messageCount.textContent = events.filter((event) => event.kind === "user_message" || event.kind === "assistant_message").length;
  els.toolCount.textContent = events.filter((event) => event.kind === "tool_call" || event.kind === "tool_result").length;
  els.errorCount.textContent = errors;
  const quota = quotaSummary(events);
  els.quotaUsed.textContent = quota?.quota || "-";
  els.tokenUsage.textContent = quota?.tokens || "-";
}

function renderAppServerEvents(events) {
  const latest = [...events].slice(-6).reverse();
  if (!latest.length) {
    els.appServerEvents.replaceChildren();
    return;
  }
  els.appServerEvents.replaceChildren(
    ...latest.map((event) => {
      const row = document.createElement("div");
      row.className = `diagnostic-item ${escapeHtml(event.direction || "")}`;
      row.innerHTML = `
        <span>${escapeHtml(directionLabel(event.direction))}</span>
        <strong>${escapeHtml(event.method || "-")}</strong>
        <p>${escapeHtml(event.summary || "")}</p>
      `;
      return row;
    })
  );
}

function openSocket(threadId) {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const url = `${protocol}//${window.location.host}/threads/${encodeURIComponent(threadId)}/events?token=${encodeURIComponent(state.token)}`;
  state.socket = new WebSocket(url);
  state.socket.onmessage = (message) => {
    const envelope = JSON.parse(message.data);
    if (envelope.type === "snapshot") {
      state.selectedThread = envelope.thread;
      state.events = envelope.events || [];
      renderSelectedThread();
      refreshThreads();
    }
    if (envelope.type === "error") showOperation(envelope.message);
  };
  state.socket.onclose = () => {
    if (state.selectedThread?.id === threadId) showOperation("实时连接已断开，刷新后会重新连接。");
  };
}

async function sendMessage() {
  const text = els.messageInput.value.trim();
  if (!state.selectedThread || !text) return;
  els.sendButton.disabled = true;
  try {
    const result = await api(`/threads/${encodeURIComponent(state.selectedThread.id)}/send`, {
      method: "POST",
      body: { text }
    });
    showOperation(result.message);
    if (result.accepted) els.messageInput.value = "";
    await selectThread(state.selectedThread.id);
  } catch (error) {
    showOperation(error.message);
  } finally {
    els.sendButton.disabled = false;
  }
}

async function interruptThread() {
  if (!state.selectedThread) return;
  const result = await api(`/threads/${encodeURIComponent(state.selectedThread.id)}/interrupt`, {
    method: "POST",
    body: {}
  });
  showOperation(result.message);
}

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json" };
  if (options.auth !== false && state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(path, {
    method: options.method || "GET",
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : {};
  if (!response.ok) {
    const message = data.error?.message || `${response.status} ${response.statusText}`;
    throw new Error(`${data.error?.code || response.status}: ${message}`);
  }
  return data;
}

function resetConnection() {
  closeSocket();
  state.token = "";
  state.deviceId = "";
  localStorage.removeItem("codexCompanion.token");
  localStorage.removeItem("codexCompanion.deviceId");
  window.location.reload();
}

function closeSocket() {
  if (state.socket) state.socket.close(1000, "Switch thread");
  state.socket = null;
}

function showOperation(message) {
  els.operationResult.textContent = message || "";
}

async function copyText(value, successMessage) {
  const text = String(value || "").trim();
  if (!text || text === "-") {
    showCopyResult("没有可复制的内容");
    return;
  }
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
    } else {
      const input = document.createElement("textarea");
      input.value = text;
      input.setAttribute("readonly", "");
      input.style.position = "fixed";
      input.style.opacity = "0";
      document.body.appendChild(input);
      input.select();
      document.execCommand("copy");
      input.remove();
    }
    showCopyResult(successMessage);
  } catch {
    showCopyResult("复制失败，请手动选中复制");
  }
}

function showCopyResult(message) {
  els.copyResult.textContent = message;
  window.clearTimeout(showCopyResult.timer);
  showCopyResult.timer = window.setTimeout(() => {
    els.copyResult.textContent = "";
  }, 2400);
}

function buildBestAddress(status) {
  const address = status.addresses?.find((item) => item.startsWith("192.168.")) || status.addresses?.[0] || "127.0.0.1";
  return `http://${address}:${status.port}`;
}

function labelForKind(kind) {
  return {
    user_message: "你",
    assistant_message: "Codex",
    reasoning_summary: "思考",
    tool_call: "工具调用",
    tool_result: "工具结果",
    status: "状态",
    error: "错误"
  }[kind] || kind;
}

function formatTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value || "-";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

function shouldCollapse(event) {
  return (event.kind === "tool_call" || event.kind === "tool_result" || event.kind === "status") && String(event.text || "").length > 500;
}

function pendingLabel(startedEvent) {
  const startedAt = Date.parse(startedEvent?.timestamp || "");
  if (!Number.isFinite(startedAt)) return "运行中";
  const elapsedSeconds = Math.round((Date.now() - startedAt) / 1000);
  return elapsedSeconds > 45 ? `等待 ${elapsedSeconds} 秒` : "运行中";
}

function directionLabel(value) {
  return {
    stderr: "日志",
    notification: "通知",
    request: "请求",
    response: "响应",
    error: "错误"
  }[value] || "事件";
}

function extractWarning(value) {
  if (!value) return "";
  try {
    const parsed = JSON.parse(value);
    return parsed.fields?.message || value;
  } catch {
    return value;
  }
}

function sourceLabel(value) {
  return {
    environment: "环境变量",
    saved: "已保存",
    auto: "自动识别",
    default: "默认位置"
  }[value] || "已识别";
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function renderThreads() {
  const active = state.threads.filter((thread) => thread.status === "active").length;
  els.threadCount.textContent = `${state.filteredThreads.length} 个会话`;
  els.activeCount.textContent = `${active} 个运行中`;
  const nodes = [];
  for (const group of groupThreadsByProject(state.filteredThreads)) {
    const collapsed = state.collapsedProjects.has(group.project);
    const header = document.createElement("button");
    header.type = "button";
    header.className = `project-group-header ${collapsed ? "collapsed" : "expanded"}`;
    header.addEventListener("click", () => toggleProjectGroup(group.project));
    header.innerHTML = `
      <span>${escapeHtml(group.project)}</span>
      <strong>${group.threads.length} 个会话</strong>
    `;
    nodes.push(header);
    if (collapsed) continue;
    for (const thread of group.threads) {
      const item = document.createElement("button");
      item.className = `thread-item ${state.selectedThread?.id === thread.id ? "active" : ""}`;
      item.addEventListener("click", () => selectThread(thread.id));
      item.innerHTML = `
        <div class="thread-title">
          <span class="status-dot ${escapeHtml(thread.status)}"></span>
          <span>${escapeHtml(thread.title)}</span>
        </div>
        <div class="thread-preview">${escapeHtml(thread.preview || "")}</div>
        <div class="thread-meta">${escapeHtml(thread.model || "未知模型")} | ${formatTime(thread.updatedAt)}</div>
      `;
      nodes.push(item);
    }
  }
  els.threadList.replaceChildren(...nodes);
}

function readCollapsedProjects() {
  try {
    return new Set(JSON.parse(localStorage.getItem("codexCompanion.collapsedProjects") || "[]"));
  } catch {
    return new Set();
  }
}

function toggleProjectGroup(project) {
  if (state.collapsedProjects.has(project)) {
    state.collapsedProjects.delete(project);
  } else {
    state.collapsedProjects.add(project);
  }
  localStorage.setItem("codexCompanion.collapsedProjects", JSON.stringify([...state.collapsedProjects]));
  renderThreads();
}

function groupThreadsByProject(threads) {
  const groups = new Map();
  for (const thread of threads) {
    const project = projectName(thread);
    if (!groups.has(project)) groups.set(project, []);
    groups.get(project).push(thread);
  }
  return [...groups.entries()].map(([project, items]) => ({ project, threads: items }));
}

function projectName(thread) {
  const cwd = String(thread.cwd || "").trim();
  if (!cwd) return "未记录项目";
  const normalized = cwd.replace(/[\\/]+$/, "");
  return normalized.split(/[\\/]/).filter(Boolean).pop() || cwd;
}

function isCompletedEvent(event) {
  const title = String(event.title || "");
  const text = String(event.text || "");
  return event.kind === "status" && (
    title.includes("任务完成") ||
    /task complete/i.test(title) ||
    title.includes("已完成") ||
    text.includes("已完成") ||
    /completed/i.test(text)
  );
}

function quotaSummary(events) {
  const event = [...events].reverse().find((item) => item.metadata?.rate_limits || item.metadata?.info);
  if (!event) return null;
  const primary = event.metadata?.rate_limits?.primary?.used_percent;
  const secondary = event.metadata?.rate_limits?.secondary?.used_percent;
  const totalTokens = event.metadata?.info?.total_token_usage?.total_tokens;
  const quota = [
    Number.isFinite(primary) ? `5小时 ${formatPercent(primary)}%` : "",
    Number.isFinite(secondary) ? `7天 ${formatPercent(secondary)}%` : ""
  ].filter(Boolean).join(" / ");
  const tokens = Number.isFinite(totalTokens) ? compactNumber(totalTokens) : "";
  if (!quota && !tokens) return null;
  return { quota: quota || "-", tokens: tokens || "-" };
}

function formatPercent(value) {
  return Number.isInteger(value) ? String(value) : value.toFixed(1);
}

function compactNumber(value) {
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(1)}k`;
  return String(value);
}

function findLastIndex(items, predicate) {
  for (let index = items.length - 1; index >= 0; index -= 1) {
    if (predicate(items[index])) return index;
  }
  return -1;
}
