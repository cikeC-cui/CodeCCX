import { createReadStream, existsSync } from "node:fs";
import { readdir, readFile, rm, stat, writeFile } from "node:fs/promises";
import { basename, join } from "node:path";
import { createInterface } from "node:readline/promises";
import type { ConversationEvent, ThreadEventsResponse, ThreadSummary } from "@codex-companion/protocol";

type IndexLine = {
  id: string;
  thread_name?: string;
  updated_at?: string;
};

type ThreadFile = {
  id: string;
  path: string;
  updatedAt: string;
};

export class CodexLogStore {
  constructor(private readonly codexHome: string) {}

  async listThreads(): Promise<ThreadSummary[]> {
    const indexed = await this.readIndex();
    const files = await this.scanThreadFiles();
    const fileById = new Map(files.map((file) => [file.id, file]));
    const ids = new Set([...indexed.map((item) => item.id), ...files.map((file) => file.id)]);
    const threads: ThreadSummary[] = [];

    for (const id of ids) {
      const indexItem = indexed.find((item) => item.id === id);
      const threadFile = fileById.get(id);
      const summary = await this.summarizeThread(id, threadFile?.path, indexItem);
      threads.push(summary);
    }

    return threads.sort((a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt));
  }

  async getThreadEvents(threadId: string): Promise<ThreadEventsResponse> {
    const threadFile = await this.findThreadFile(threadId);
    if (!threadFile) {
      throw new Error(`Thread ${threadId} was not found in local Codex history.`);
    }
    const indexItem = (await this.readIndex()).find((item) => item.id === threadId);
    const thread = await this.summarizeThread(threadId, threadFile.path, indexItem);
    const events = await this.parseEvents(threadFile.path, threadId);
    return { thread, events };
  }

  async findThreadPath(threadId: string): Promise<string | null> {
    return (await this.findThreadFile(threadId))?.path ?? null;
  }

  async renameThread(threadId: string, title: string): Promise<ThreadSummary> {
    const cleanTitle = firstLine(title).slice(0, 80);
    if (!cleanTitle) throw new Error("Thread title is required.");
    const threadFile = await this.findThreadFile(threadId);
    const indexed = await this.readIndex();
    if (!threadFile && !indexed.some((item) => item.id === threadId)) {
      throw new Error(`Thread ${threadId} was not found in local Codex history.`);
    }

    const nextIndex = indexed.map((item) =>
      item.id === threadId ? { ...item, thread_name: cleanTitle, updated_at: item.updated_at ?? new Date().toISOString() } : item
    );
    if (!nextIndex.some((item) => item.id === threadId)) {
      nextIndex.push({ id: threadId, thread_name: cleanTitle, updated_at: new Date().toISOString() });
    }
    await this.writeIndex(nextIndex);
    return this.summarizeThread(threadId, threadFile?.path, nextIndex.find((item) => item.id === threadId));
  }

  async deleteThread(threadId: string): Promise<boolean> {
    const indexed = await this.readIndex();
    const threadFile = await this.findThreadFile(threadId);
    const wasIndexed = indexed.some((item) => item.id === threadId);
    if (!threadFile && !wasIndexed) return false;

    await this.writeIndex(indexed.filter((item) => item.id !== threadId));
    if (threadFile) await rm(threadFile.path, { force: true });
    return true;
  }

  private async summarizeThread(id: string, path?: string, indexItem?: IndexLine): Promise<ThreadSummary> {
    const indexTitle = firstLine(indexItem?.thread_name);
    const fallbackTitle = indexTitle || "Untitled Codex thread";
    const fallbackUpdated = indexItem?.updated_at || new Date(0).toISOString();
    if (!path) {
      return {
        id,
        title: fallbackTitle,
        preview: fallbackTitle,
        updatedAt: fallbackUpdated,
        archived: false,
        status: "notLoaded"
      };
    }

    const events = await this.parseEvents(path, id);
    const userPreview = events.find((event) => event.kind === "user_message" && !isNoiseMessage(event.text))?.text;
    const assistantPreview = [...events].reverse().find((event) => event.kind === "assistant_message")?.text;
    const fileStat = await stat(path);
    const title = indexTitle || firstLine(userPreview) || fallbackTitle;
    const status = getThreadStatus(events);
    return {
      id,
      title,
      preview: firstLine(assistantPreview || userPreview || fallbackTitle),
      updatedAt: fileStat.mtime.toISOString(),
      cwd: events.find((event) => event.kind === "status" && event.metadata?.cwd)?.metadata?.cwd as string | undefined,
      model: events.find((event) => event.metadata?.model)?.metadata?.model as string | undefined,
      archived: false,
      status
    };
  }

  private async parseEvents(path: string, threadId: string, limit?: number): Promise<ConversationEvent[]> {
    const stream = createReadStream(path, { encoding: "utf8" });
    const rl = createInterface({ input: stream, crlfDelay: Infinity });
    const events: ConversationEvent[] = [];
    let lineNumber = 0;

    for await (const line of rl) {
      lineNumber += 1;
      if (!line.trim()) continue;
      try {
        const raw = JSON.parse(line) as {
          timestamp?: string;
          type?: string;
          payload?: Record<string, unknown>;
        };
        const normalized = normalizeEvent(raw, threadId, lineNumber);
        if (normalized) events.push(normalized);
      } catch {
        events.push({
          id: `${threadId}:${lineNumber}`,
          threadId,
          timestamp: new Date().toISOString(),
          kind: "error",
          title: "解析失败",
          text: `无法解析第 ${lineNumber} 行 Codex 记录。`
        });
      }
      if (limit && events.length >= limit) break;
    }

    return events;
  }

  private async readIndex(): Promise<IndexLine[]> {
    const path = join(this.codexHome, "session_index.jsonl");
    if (!existsSync(path)) return [];
    const raw = await readFile(path, "utf8");
    return raw
      .split(/\r?\n/)
      .filter(Boolean)
      .map((line) => {
        try {
          return JSON.parse(line) as IndexLine;
        } catch {
          return null;
        }
      })
      .filter((item): item is IndexLine => Boolean(item?.id));
  }

  private async writeIndex(items: IndexLine[]): Promise<void> {
    const path = join(this.codexHome, "session_index.jsonl");
    const lines = items.map((item) => JSON.stringify(item));
    await writeFile(path, `${lines.join("\n")}\n`, "utf8");
  }

  private async findThreadFile(threadId: string): Promise<ThreadFile | null> {
    const files = await this.scanThreadFiles();
    return files.find((file) => file.id === threadId) ?? null;
  }

  private async scanThreadFiles(): Promise<ThreadFile[]> {
    const sessionsDir = join(this.codexHome, "sessions");
    if (!existsSync(sessionsDir)) return [];
    const files: ThreadFile[] = [];
    await walk(sessionsDir, async (path) => {
      if (!path.endsWith(".jsonl")) return;
      const id = extractThreadId(path);
      if (!id) return;
      const fileStat = await stat(path);
      files.push({ id, path, updatedAt: fileStat.mtime.toISOString() });
    });
    return files.sort((a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt));
  }
}

async function walk(dir: string, onFile: (path: string) => Promise<void>): Promise<void> {
  const entries = await readdir(dir, { withFileTypes: true });
  await Promise.all(
    entries.map(async (entry) => {
      const path = join(dir, entry.name);
      if (entry.isDirectory()) await walk(path, onFile);
      else if (entry.isFile()) await onFile(path);
    })
  );
}

function extractThreadId(path: string): string | null {
  const match = basename(path).match(/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/i);
  return match?.[1] ?? null;
}

function normalizeEvent(
  raw: { timestamp?: string; type?: string; payload?: Record<string, unknown> },
  threadId: string,
  lineNumber: number
): ConversationEvent | null {
  const timestamp = raw.timestamp ?? new Date().toISOString();
  const payload = raw.payload ?? {};
  const payloadType = asString(payload.type);
  const base = {
    id: `${threadId}:${lineNumber}`,
    threadId,
    timestamp
  };

  if (raw.type === "session_meta") {
    return {
      ...base,
      kind: "status",
      title: "会话",
      text: "Codex 会话已启动",
      metadata: pickMetadata(payload, ["cwd", "model_provider", "cli_version", "source"])
    };
  }

  if (raw.type === "turn_context") {
    return {
      ...base,
      kind: "status",
      title: "回合上下文",
      text: "Codex 回合上下文已更新",
      metadata: pickMetadata(payload, ["cwd", "model", "timezone", "approval_policy", "sandbox_policy"])
    };
  }

  if (raw.type === "event_msg" && payloadType === "user_message") {
    return {
      ...base,
      kind: "user_message",
      title: "你",
      text: asString(payload.message)
    };
  }

  if (raw.type === "event_msg" && payloadType === "agent_message") {
    return {
      ...base,
      kind: "assistant_message",
      title: "Codex",
      text: asString(payload.message),
      metadata: pickMetadata(payload, ["phase", "memory_citation"])
    };
  }

  if (raw.type === "response_item" && payloadType === "message") {
    return null;
  }

  if (raw.type === "event_msg" && payloadType === "agent_reasoning") {
    const text = asString(payload.text);
    if (!text) return null;
    return {
      ...base,
      kind: "reasoning_summary",
      title: "思考摘要",
      text
    };
  }

  if (raw.type === "response_item" && payloadType === "reasoning") {
    return null;
  }

  if (raw.type === "response_item" && payloadType === "function_call") {
    return {
      ...base,
      kind: "tool_call",
      title: asString(payload.name) || "工具调用",
      text: trimLong(asString(payload.arguments), 4000),
      metadata: pickMetadata(payload, ["call_id", "name"])
    };
  }

  if (raw.type === "response_item" && payloadType === "function_call_output") {
    return {
      ...base,
      kind: "tool_result",
      title: "工具结果",
      text: trimLong(asString(payload.output), 4000),
      metadata: pickMetadata(payload, ["call_id"])
    };
  }

  if (raw.type === "event_msg" && payloadType === "task_started") {
    return {
      ...base,
      kind: "status",
      title: "任务开始",
      text: "Codex 开始处理",
      metadata: pickMetadata(payload, ["turn_id", "model_context_window", "collaboration_mode_kind"])
    };
  }

  if (raw.type === "event_msg" && payloadType === "task_complete") {
    const lastAgentMessage = asString(payload.last_agent_message);
    const metadata = pickMetadata(payload, ["turn_id", "duration_ms", "time_to_first_token_ms"]);
    if (!lastAgentMessage) {
      return {
        ...base,
        kind: "status",
        title: "任务完成",
        text: "本轮已结束，没有新的助手文本回复。",
        metadata: { ...metadata, missingAssistantOutput: true }
      };
    }
    return {
      ...base,
      kind: "status",
      title: "任务完成",
      text: lastAgentMessage,
      metadata
    };
  }

  if (raw.type === "event_msg" && payloadType === "token_count") {
    return {
      ...base,
      kind: "status",
      title: "Token 用量",
      text: "Token 用量已更新",
      metadata: payload
    };
  }

  return null;
}

function textFromContentItem(item: unknown): string {
  if (!item || typeof item !== "object") return asString(item);
  const record = item as Record<string, unknown>;
  return asString(record.text) || asString(record.content);
}

function pickMetadata(source: Record<string, unknown>, keys: string[]): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const key of keys) {
    if (source[key] !== undefined) result[key] = source[key];
  }
  return result;
}

function asString(value: unknown): string {
  if (typeof value === "string") return value;
  if (value === undefined || value === null) return "";
  return JSON.stringify(value);
}

function firstLine(value?: string): string {
  return (value ?? "").split(/\r?\n/)[0]?.trim().slice(0, 120) ?? "";
}

function trimLong(value: string, max: number): string {
  if (value.length <= max) return value;
  return `${value.slice(0, max)}\n...[truncated]`;
}

function isNoiseMessage(value?: string): boolean {
  const text = (value ?? "").trim();
  return text.startsWith("<environment_context>") || text.startsWith("<developer_context>");
}

function getThreadStatus(events: ConversationEvent[]): ThreadSummary["status"] {
  if (events.some((event) => event.kind === "error")) return "systemError";
  const lastTaskStarted = findLastIndex(events, (event) => event.title === "任务开始" || event.title === "Task started");
  const lastTaskComplete = findLastIndex(events, (event) => event.title === "任务完成" || event.title === "Task complete");
  return lastTaskStarted > lastTaskComplete ? "active" : "idle";
}

function findLastIndex<T>(items: T[], predicate: (item: T) => boolean): number {
  for (let index = items.length - 1; index >= 0; index -= 1) {
    if (predicate(items[index])) return index;
  }
  return -1;
}
