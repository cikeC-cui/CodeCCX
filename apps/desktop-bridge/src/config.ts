import { homedir } from "node:os";
import { existsSync } from "node:fs";
import { join } from "node:path";

export type BridgeConfig = {
  name: string;
  host: string;
  port: number;
  publicUrl?: string;
  codexHome: string;
  dataDir: string;
  enableAppServer: boolean;
  codexCommand: string;
};

export function loadConfig(): BridgeConfig {
  const dataDir = process.env.BRIDGE_DATA_DIR ?? join(homedir(), ".codex-companion");
  return {
    name: process.env.BRIDGE_NAME ?? `${process.env.COMPUTERNAME ?? "Windows"} Codex Bridge`,
    host: process.env.BRIDGE_HOST ?? "0.0.0.0",
    port: parseInt(process.env.BRIDGE_PORT ?? "4518", 10),
    publicUrl: normalizePublicUrl(process.env.BRIDGE_PUBLIC_URL),
    codexHome: process.env.CODEX_HOME ?? join(homedir(), ".codex"),
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
  const candidates = [
    localAppData ? join(localAppData, "OpenAI", "Codex", "bin", "07133f975a59dbd9", "codex.exe") : "",
    localAppData ? join(localAppData, "OpenAI", "Codex", "bin", "codex.exe") : ""
  ].filter(Boolean);
  return candidates.find((candidate) => existsSync(candidate)) ?? "codex";
}
