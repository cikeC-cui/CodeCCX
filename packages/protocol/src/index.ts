export type TransportKind = "direct_lan" | "virtual_lan" | "relay";

export type BridgeStatus = {
  bridgeName: string;
  version: string;
  host: string;
  port: number;
  addresses: string[];
  publicUrl?: string;
  virtualAddress?: string;
  transports: TransportKind[];
  codexHome: CodexHomeStatus;
  codexAppServer: {
    enabled: boolean;
    available: boolean;
    lastError?: string;
    pendingRequests?: number;
    initialized?: boolean;
    recentEvents?: AppServerDiagnosticEvent[];
  };
};

export type CodexHomeSource = "environment" | "saved" | "auto" | "default";

export type CodexHomeCandidate = {
  path: string;
  exists: boolean;
  valid: boolean;
  reason?: string;
};

export type CodexHomeStatus = {
  path: string;
  detected: boolean;
  source: CodexHomeSource;
  reason?: string;
  candidates: CodexHomeCandidate[];
};

export type AppServerDiagnosticEvent = {
  at: string;
  direction: "stderr" | "notification" | "request" | "response" | "error";
  method?: string;
  id?: string | number;
  summary: string;
};

export type PairingInfo = {
  bridgeName: string;
  pairToken: string;
  expiresAt: string;
  addresses: string[];
  port: number;
  publicUrl?: string;
  virtualAddress?: string;
  transports: TransportKind[];
  qrPayload: string;
};

export type PairRequest = {
  pairToken: string;
  deviceName: string;
};

export type PairResponse = {
  deviceId: string;
  authToken: string;
  bridgeName: string;
};

export type ThreadSummary = {
  id: string;
  title: string;
  preview: string;
  updatedAt: string;
  createdAt?: string;
  cwd?: string;
  model?: string;
  archived: boolean;
  status: "notLoaded" | "idle" | "active" | "systemError" | "unknown";
};

export type ConversationEventKind =
  | "user_message"
  | "assistant_message"
  | "reasoning_summary"
  | "tool_call"
  | "tool_result"
  | "status"
  | "error"
  | "raw";

export type ConversationEvent = {
  id: string;
  threadId: string;
  timestamp: string;
  kind: ConversationEventKind;
  title?: string;
  text?: string;
  metadata?: Record<string, unknown>;
};

export type ThreadEventsResponse = {
  thread: ThreadSummary;
  events: ConversationEvent[];
};

export type SendMessageRequest = {
  text: string;
};

export type RenameThreadRequest = {
  title: string;
};

export type RenameThreadResponse = {
  thread: ThreadSummary;
};

export type DeleteThreadResponse = {
  deleted: boolean;
  threadId: string;
};

export type UpdateCodexHomeRequest = {
  codexHome: string;
};

export type UpdateCodexHomeResponse = {
  codexHome: CodexHomeStatus;
};

export type SendMessageResponse = {
  accepted: boolean;
  turnId?: string;
  message: string;
};

export type ApiError = {
  error: {
    code: string;
    message: string;
  };
};

export type SocketEnvelope =
  | { type: "snapshot"; thread: ThreadSummary; events: ConversationEvent[] }
  | { type: "event"; event: ConversationEvent }
  | { type: "status"; status: string }
  | { type: "error"; message: string };
