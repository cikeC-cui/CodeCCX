import { homedir } from "node:os";
import { existsSync, mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import type { CodexHomeCandidate, CodexHomeStatus } from "@codex-companion/protocol";

export type BridgeConfig = {
  name: string;
  host: string;
  port: number;
  publicUrl?: string;
  codexHome: string;
  codexHomeStatus: CodexHomeStatus;
  dataDir: string;
  enableAppServer: boolean;
  codexCommand: string;
};

type BridgeSettings = {
  codexHome?: string;
};

export function loadConfig(): BridgeConfig {
  const dataDir = process.env.BRIDGE_DATA_DIR ?? join(homedir(), ".codex-companion");
  const settings = readBridgeSettings(dataDir);
  const codexHomeStatus = resolveCodexHome(settings.codexHome);
  return {
    name: process.env.BRIDGE_NAME ?? `${process.env.COMPUTERNAME ?? "Windows"} Codex Bridge`,
    host: process.env.BRIDGE_HOST ?? "0.0.0.0",
    port: parseInt(process.env.BRIDGE_PORT ?? "4518", 10),
    publicUrl: normalizePublicUrl(process.env.BRIDGE_PUBLIC_URL),
    codexHome: codexHomeStatus.path,
    codexHomeStatus,
    dataDir,
    enableAppServer: process.env.BRIDGE_DISABLE_APP_SERVER !== "1",
    codexCommand: process.env.CODEX_COMMAND ?? findLocalCodexCommand()
  };
}

function normalizePublicUrl(value: string | undefined): string | undefined {
  const trimmed = value?.trim().replace(/\/+$/, "");
  return trimmed || undefined;
}

function findLocalCodexCommand(): string {
  const localAppData = process.env.LOCALAPPDATA;
  const binDir = localAppData ? join(localAppData, "OpenAI", "Codex", "bin") : "";
  const versionedCommands = binDir && existsSync(binDir)
    ? readdirSync(binDir, { withFileTypes: true })
        .filter((entry) => entry.isDirectory())
        .map((entry) => join(binDir, entry.name, "codex.exe"))
    : [];
  const candidates = [
    localAppData ? join(localAppData, "OpenAI", "Codex", "bin", "codex.exe") : "",
    ...versionedCommands
  ].filter(Boolean);
  return candidates.find((candidate) => existsSync(candidate)) ?? "codex";
}

export function inspectCodexHome(rawPath: string): CodexHomeCandidate {
  const candidatePath = normalizePath(rawPath);
  if (!candidatePath) {
    return { path: "", exists: false, valid: false, reason: "Path is required." };
  }
  if (!existsSync(candidatePath)) {
    return { path: candidatePath, exists: false, valid: false, reason: "Directory does not exist." };
  }
  try {
    if (!statSync(candidatePath).isDirectory()) {
      return { path: candidatePath, exists: true, valid: false, reason: "Path is not a directory." };
    }
  } catch (error) {
    return { path: candidatePath, exists: true, valid: false, reason: error instanceof Error ? error.message : String(error) };
  }

  const hasSessions = existsSync(join(candidatePath, "sessions"));
  const hasIndex = existsSync(join(candidatePath, "session_index.jsonl"));
  const hasAuth = existsSync(join(candidatePath, "auth.json"));
  if (!hasSessions && !hasIndex && !hasAuth) {
    return {
      path: candidatePath,
      exists: true,
      valid: false,
      reason: "No Codex sessions, session index, or auth.json were found in this directory."
    };
  }
  return { path: candidatePath, exists: true, valid: true };
}

export function saveCodexHome(dataDir: string, codexHome: string): void {
  const settingsPath = settingsFile(dataDir);
  mkdirSync(dirname(settingsPath), { recursive: true });
  writeFileSync(settingsPath, `${JSON.stringify({ ...readBridgeSettings(dataDir), codexHome: normalizePath(codexHome) }, null, 2)}\n`, "utf8");
}

function resolveCodexHome(savedCodexHome?: string): CodexHomeStatus {
  const explicit = process.env.CODEX_HOME;
  const rawCandidates: Array<{ path: string | undefined; source: CodexHomeStatus["source"] }> = [
    { path: explicit, source: "environment" },
    { path: savedCodexHome, source: "saved" },
    { path: join(homedir(), ".codex"), source: "auto" },
    { path: process.env.LOCALAPPDATA ? join(process.env.LOCALAPPDATA, "OpenAI", "Codex") : undefined, source: "auto" },
    { path: process.env.LOCALAPPDATA ? join(process.env.LOCALAPPDATA, "OpenAI", "Codex", ".codex") : undefined, source: "auto" },
    { path: process.env.APPDATA ? join(process.env.APPDATA, "OpenAI", "Codex") : undefined, source: "auto" },
    { path: process.env.APPDATA ? join(process.env.APPDATA, "OpenAI", "Codex", ".codex") : undefined, source: "auto" }
  ];
  const candidatesByPath = new Map<string, { candidate: CodexHomeCandidate; source: CodexHomeStatus["source"] }>();

  for (const raw of rawCandidates) {
    const candidatePath = normalizePath(raw.path ?? "");
    if (!candidatePath || candidatesByPath.has(candidatePath)) continue;
    candidatesByPath.set(candidatePath, { candidate: inspectCodexHome(candidatePath), source: raw.source });
  }

  const candidates = [...candidatesByPath.values()];
  const detected = candidates.find((item) => item.candidate.valid);
  if (detected) {
    return {
      path: detected.candidate.path,
      detected: true,
      source: detected.source,
      candidates: candidates.map((item) => item.candidate)
    };
  }

  const fallback = normalizePath(explicit || savedCodexHome || join(homedir(), ".codex"));
  return {
    path: fallback,
    detected: false,
    source: explicit ? "environment" : savedCodexHome ? "saved" : "default",
    reason: "Codex data directory was not detected automatically.",
    candidates: candidates.map((item) => item.candidate)
  };
}

function readBridgeSettings(dataDir: string): BridgeSettings {
  const path = settingsFile(dataDir);
  if (!existsSync(path)) return {};
  try {
    return JSON.parse(readFileSync(path, "utf8")) as BridgeSettings;
  } catch {
    return {};
  }
}

function settingsFile(dataDir: string): string {
  return join(dataDir, "settings.json");
}

function normalizePath(value: string): string {
  const trimmed = value.trim().replace(/^["']|["']$/g, "");
  return trimmed ? resolve(trimmed) : "";
}
