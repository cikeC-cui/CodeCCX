import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import { createInterface, type Interface } from "node:readline";
import { randomUUID } from "node:crypto";
import { readFile } from "node:fs/promises";
import { join } from "node:path";
import type { AppServerDiagnosticEvent, SendMessageResponse } from "@codex-companion/protocol";

type PendingRequest = {
  resolve: (value: unknown) => void;
  reject: (error: Error) => void;
};

type JsonRpcMessage = {
  id?: string | number;
  method?: string;
  params?: unknown;
  result?: unknown;
  error?: { message?: string };
};

export class CodexAppServerClient {
  private child: ChildProcessWithoutNullStreams | null = null;
  private lines: Interface | null = null;
  private pending = new Map<string | number, PendingRequest>();
  private lastErrorText: string | undefined;
  private initialized = false;
  private recentEvents: AppServerDiagnosticEvent[] = [];
  private activeTurnByThread = new Map<string, string>();

  constructor(
    private readonly command: string,
    private readonly enabled: boolean,
    private readonly codexHome: string
  ) {}

  get available(): boolean {
    return Boolean(this.child && !this.child.killed);
  }

  get lastError(): string | undefined {
    return this.lastErrorText;
  }

  get diagnostics(): Pick<
    NonNullable<import("@codex-companion/protocol").BridgeStatus["codexAppServer"]>,
    "pendingRequests" | "initialized" | "recentEvents"
  > {
    return {
      pendingRequests: this.pending.size,
      initialized: this.initialized,
      recentEvents: [...this.recentEvents]
    };
  }

  async start(): Promise<void> {
    if (!this.enabled || this.child) return;
    try {
      this.child = spawn(this.command, ["app-server"], {
        stdio: ["pipe", "pipe", "pipe"],
        windowsHide: true
      });
      this.lines = createInterface({ input: this.child.stdout });
      this.lines.on("line", (line) => this.handleLine(line));
      this.child.stderr.on("data", (chunk: Buffer) => {
        const text = chunk.toString("utf8").trim();
        if (isImportantBackendWarning(text)) this.lastErrorText = text;
        this.recordEvent("stderr", text);
      });
      this.child.on("exit", (code) => {
        this.lastErrorText = `Codex App Server exited with code ${code ?? "unknown"}`;
        this.recordEvent("error", this.lastErrorText);
        this.child = null;
        this.initialized = false;
        for (const pending of this.pending.values()) pending.reject(new Error(this.lastErrorText));
        this.pending.clear();
      });
    } catch (error) {
      this.lastErrorText = error instanceof Error ? error.message : String(error);
      this.recordEvent("error", this.lastErrorText);
      this.child = null;
    }
  }

  async sendMessage(threadId: string, text: string): Promise<SendMessageResponse> {
    if (!this.enabled) {
      return { accepted: false, message: "Codex App Server 已禁用。" };
    }
    await this.start();
    if (!this.child) {
      return {
        accepted: false,
        message: this.lastErrorText || "Codex App Server 不可用。"
      };
    }

    const turnId = randomUUID();

    try {
      await this.ensureInitialized();
      await this.request("thread/resume", { threadId });
      const clientUserMessageId = randomUUID();
      const response = await this.request("turn/start", {
        threadId,
        clientUserMessageId,
        input: [{ type: "text", text, text_elements: [] }]
      });
      const responseTurnId = readNestedString(response, ["turn", "id"]) ?? turnId;
      this.activeTurnByThread.set(threadId, responseTurnId);
      return { accepted: true, turnId: responseTurnId, message: "消息已发送到 Codex。" };
    } catch (error) {
      this.lastErrorText = error instanceof Error ? error.message : String(error);
      return {
        accepted: false,
        turnId,
        message: `无法通过 Codex App Server 发送消息：${this.lastErrorText}`
      };
    }
  }

  async interrupt(threadId: string): Promise<SendMessageResponse> {
    if (!this.enabled) {
      return { accepted: false, message: "Codex App Server 已禁用。" };
    }
    await this.start();
    if (!this.child) {
      return { accepted: false, message: this.lastErrorText || "Codex App Server 不可用。" };
    }
    try {
      await this.ensureInitialized();
      const turnId = this.activeTurnByThread.get(threadId);
      if (!turnId) {
        return {
          accepted: false,
          message: "当前会话没有正在运行的 Codex 回复，无法中断。"
        };
      }
      await this.request("turn/interrupt", { turnId });
      this.activeTurnByThread.delete(threadId);
      return { accepted: true, message: "已发送中断请求。" };
    } catch (error) {
      this.lastErrorText = error instanceof Error ? error.message : String(error);
      return { accepted: false, message: `无法中断 Codex：${this.lastErrorText}` };
    }
  }

  private request(method: string, params: unknown, timeoutMs = 30000): Promise<unknown> {
    if (!this.child) return Promise.reject(new Error("Codex App Server process is not running."));
    const id = randomUUID();
    const payload = JSON.stringify({ jsonrpc: "2.0", id, method, params });
    this.recordEvent("request", `client request: ${method}`, method, id);
    this.child.stdin.write(`${payload}\n`);
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pending.delete(id);
        reject(new Error(`Timed out waiting for ${method}`));
      }, timeoutMs);
      this.pending.set(id, {
        resolve: (value) => {
          clearTimeout(timeout);
          resolve(value);
        },
        reject: (error) => {
          clearTimeout(timeout);
          reject(error);
        }
      });
    });
  }

  private handleLine(line: string): void {
    if (!line.trim()) return;
    try {
      const message = JSON.parse(line) as JsonRpcMessage;
      if (message.method) {
        if (message.id !== undefined) void this.handleServerRequest(message);
        else this.handleServerNotification(message);
        return;
      }
      if (message.id === undefined) return;
      const pending = this.pending.get(message.id);
      if (!pending) return;
      this.pending.delete(message.id);
      if (message.error) {
        const error = new Error(message.error.message ?? "Codex App Server returned an error.");
        this.recordEvent("error", error.message, undefined, message.id);
        pending.reject(error);
      } else {
        this.recordEvent("response", `response for ${String(message.id).slice(0, 8)}`, undefined, message.id);
        pending.resolve(message.result);
      }
    } catch {
      this.lastErrorText = `Unexpected App Server output: ${line.slice(0, 200)}`;
      this.recordEvent("error", this.lastErrorText);
    }
  }

  private async ensureInitialized(): Promise<void> {
    if (this.initialized) return;
    await this.request("initialize", {
      clientInfo: { name: "codex-companion-bridge", title: "Codex Companion Bridge", version: "1.0.0" },
      capabilities: { experimentalApi: true, requestAttestation: false }
    });
    this.initialized = true;
  }

  private handleServerNotification(message: JsonRpcMessage): void {
    this.trackTurnState(message);
    const summary = summarizeAppServerMessage(message);
    if (
      message.method === "error" ||
      ((message.method === "warning" || message.method === "configWarning") && isImportantBackendWarning(summary))
    ) {
      this.lastErrorText = summary;
    }
    this.recordEvent("notification", summary, message.method);
  }

  private trackTurnState(message: JsonRpcMessage): void {
    if (message.method !== "turn/started" && message.method !== "turn/completed") return;
    if (!message.params || typeof message.params !== "object") return;
    const record = message.params as Record<string, unknown>;
    const threadId = asString(record.threadId);
    const turnId = readNestedString(record, ["turn", "id"]) || asString(record.turnId);
    if (!threadId || !turnId) return;
    if (message.method === "turn/started") {
      this.activeTurnByThread.set(threadId, turnId);
      return;
    }
    this.activeTurnByThread.delete(threadId);
  }

  private async handleServerRequest(message: JsonRpcMessage): Promise<void> {
    this.recordEvent("request", `server request: ${message.method}`, message.method, message.id);
    try {
      const result = await this.resultForServerRequest(message);
      this.respond(message.id!, result);
      this.recordEvent("response", `handled server request: ${message.method}`, message.method, message.id);
    } catch (error) {
      const text = error instanceof Error ? error.message : String(error);
      this.lastErrorText = text;
      this.respondError(message.id!, -32000, text);
      this.recordEvent("error", `${message.method}: ${text}`, message.method, message.id);
    }
  }

  private async resultForServerRequest(message: JsonRpcMessage): Promise<unknown> {
    switch (message.method) {
      case "account/chatgptAuthTokens/refresh":
        return this.readChatGptAuthTokens();
      case "item/commandExecution/requestApproval":
        return { decision: "decline" };
      case "item/fileChange/requestApproval":
        return { decision: "decline" };
      case "applyPatchApproval":
      case "execCommandApproval":
        return { decision: "denied" };
      case "item/tool/requestUserInput":
        return { answers: {} };
      case "mcpServer/elicitation/request":
        return { action: "decline", content: null, _meta: null };
      case "item/permissions/requestApproval":
        return { permissions: {}, scope: "turn", strictAutoReview: true };
      case "item/tool/call":
        return {
          success: false,
          contentItems: [{ type: "inputText", text: "Codex Companion Bridge cannot run this client-side tool yet." }]
        };
      default:
        throw new Error(`Unsupported App Server request: ${message.method ?? "unknown"}`);
    }
  }

  private async readChatGptAuthTokens(): Promise<unknown> {
    const authPath = join(this.codexHome, "auth.json");
    const raw = await readFile(authPath, "utf8");
    const auth = JSON.parse(raw) as {
      tokens?: { access_token?: string; id_token?: string };
    };
    const accessToken = auth.tokens?.access_token;
    if (!accessToken) throw new Error("Codex auth.json does not contain an access token.");
    const claims = decodeJwtPayload(accessToken);
    const authClaims = claims["https://api.openai.com/auth"] as Record<string, unknown> | undefined;
    const chatgptAccountId = asString(authClaims?.chatgpt_account_id);
    if (!chatgptAccountId) throw new Error("Codex auth token does not include a ChatGPT account id.");
    return {
      accessToken,
      chatgptAccountId,
      chatgptPlanType: asString(authClaims?.chatgpt_plan_type) || null
    };
  }

  private respond(id: string | number, result: unknown): void {
    if (!this.child) return;
    this.child.stdin.write(`${JSON.stringify({ jsonrpc: "2.0", id, result })}\n`);
  }

  private respondError(id: string | number, code: number, message: string): void {
    if (!this.child) return;
    this.child.stdin.write(`${JSON.stringify({ jsonrpc: "2.0", id, error: { code, message } })}\n`);
  }

  private recordEvent(
    direction: AppServerDiagnosticEvent["direction"],
    summary: string,
    method?: string,
    id?: string | number
  ): void {
    this.recentEvents.push({
      at: new Date().toISOString(),
      direction,
      method,
      id,
      summary: redactSensitive(summary).slice(0, 500)
    });
    if (this.recentEvents.length > 30) this.recentEvents.splice(0, this.recentEvents.length - 30);
  }
}

function readNestedString(value: unknown, path: string[]): string | null {
  let current = value;
  for (const key of path) {
    if (!current || typeof current !== "object") return null;
    current = (current as Record<string, unknown>)[key];
  }
  return typeof current === "string" ? current : null;
}

function summarizeAppServerMessage(message: JsonRpcMessage): string {
  const params = message.params;
  if (!params || typeof params !== "object") return message.method ?? "App Server event";
  const record = params as Record<string, unknown>;
  const threadId = asString(record.threadId);
  if (message.method === "item/agentMessage/delta") {
    return `assistant delta${threadId ? ` for ${threadId}` : ""}: ${asString(record.delta).slice(0, 120)}`;
  }
  if (message.method === "turn/completed") {
    const turn = record.turn as Record<string, unknown> | undefined;
    return `turn completed${threadId ? ` for ${threadId}` : ""}: ${asString(turn?.status) || "unknown"}`;
  }
  if (message.method === "turn/started") return `turn started${threadId ? ` for ${threadId}` : ""}`;
  if (message.method === "error" || message.method === "warning" || message.method === "configWarning") {
    return asString(record.message) || JSON.stringify(record);
  }
  return `${message.method}${threadId ? ` for ${threadId}` : ""}`;
}

function decodeJwtPayload(token: string): Record<string, unknown> {
  const payload = token.split(".")[1];
  if (!payload) return {};
  const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
  return JSON.parse(Buffer.from(padded, "base64").toString("utf8")) as Record<string, unknown>;
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function redactSensitive(value: string): string {
  return value
    .replace(/Bearer\s+[A-Za-z0-9._-]+/gi, "Bearer [redacted]")
    .replace(/eyJ[A-Za-z0-9._-]+/g, "[jwt-redacted]")
    .replace(/rt_[A-Za-z0-9._-]+/g, "[refresh-token-redacted]");
}

function isImportantBackendWarning(value: string): boolean {
  return /error|failed|timed out|timeout|disconnected|falling back|auth|token|rate limit|unauthorized/i.test(value);
}
