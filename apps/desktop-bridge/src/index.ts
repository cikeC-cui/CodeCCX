import express, { type Request, type Response, type NextFunction } from "express";
import cors from "cors";
import http from "node:http";
import { join } from "node:path";
import { dirname } from "node:path";
import { fileURLToPath } from "node:url";
import chokidar from "chokidar";
import type { FSWatcher } from "chokidar";
import qrcode from "qrcode-terminal";
import { WebSocketServer } from "ws";
import type {
  ApiError,
  BridgeStatus,
  DeleteThreadResponse,
  PairingInfo,
  PairRequest,
  RenameThreadRequest,
  SendMessageRequest,
  SocketEnvelope
} from "@codex-companion/protocol";
import { AuthStore, createPairToken, type DeviceRecord } from "./auth.js";
import { loadConfig } from "./config.js";
import { CodexAppServerClient } from "./codexAppServer.js";
import { CodexLogStore } from "./codexLogStore.js";
import { getPrivateAddresses } from "./network.js";

const config = loadConfig();
const addresses = getPrivateAddresses();
let pairing = createPairToken();

const authStore = new AuthStore(join(config.dataDir, "devices.json"));
const logStore = new CodexLogStore(config.codexHome);
const appServer = new CodexAppServerClient(config.codexCommand, config.enableAppServer, config.codexHome);
const __dirname = dirname(fileURLToPath(import.meta.url));
const publicDir = join(__dirname, "../public");

await authStore.load();
void appServer.start();

const app = express();
app.use(cors());
app.use(express.json({ limit: "1mb" }));
app.use(express.static(publicDir));

app.get("/health", (_req, res) => {
  res.json(statusPayload());
});

app.get("/pair", (_req, res) => {
  ensurePairingFresh();
  res.json(pairingPayload());
});

app.post("/pair", async (req: Request<unknown, unknown, PairRequest>, res) => {
  if (!req.body?.pairToken || req.body.pairToken !== pairing.pairToken || pairing.expiresAt.getTime() < Date.now()) {
    writeError(res, 401, "PAIRING_TOKEN_INVALID", "Pairing token is invalid or expired.");
    return;
  }
  const device = await authStore.createDevice(req.body.deviceName);
  pairing = createPairToken();
  res.json({ ...device, bridgeName: config.name });
});

app.get("/devices", requireAuth, (req, res) => {
  res.json(authStore.listDevices().map(({ tokenHash: _tokenHash, ...device }) => device));
});

app.delete("/devices/:deviceId", requireAuth, async (req, res) => {
  const revoked = await authStore.revokeDevice(String(req.params.deviceId));
  res.json({ revoked });
});

app.get("/threads", requireAuth, async (_req, res, next) => {
  try {
    res.json(await logStore.listThreads());
  } catch (error) {
    next(error);
  }
});

app.get("/threads/:threadId/events", requireAuth, async (req, res, next) => {
  try {
    res.json(await logStore.getThreadEvents(String(req.params.threadId)));
  } catch (error) {
    next(error);
  }
});

app.post("/threads/:threadId/send", requireAuth, async (req: Request<{ threadId: string }, unknown, SendMessageRequest>, res) => {
  const text = req.body?.text?.trim();
  if (!text) {
    writeError(res, 400, "MESSAGE_EMPTY", "Message text is required.");
    return;
  }
  res.json(await appServer.sendMessage(String(req.params.threadId), text));
});

app.post(
  "/threads/:threadId/rename",
  requireAuth,
  async (req: Request<{ threadId: string }, unknown, RenameThreadRequest>, res, next) => {
    try {
      const title = req.body?.title?.trim();
      if (!title) {
        writeError(res, 400, "THREAD_TITLE_EMPTY", "Thread title is required.");
        return;
      }
      res.json({ thread: await logStore.renameThread(String(req.params.threadId), title) });
    } catch (error) {
      next(error);
    }
  }
);

app.post("/threads/:threadId/interrupt", requireAuth, async (req: Request<{ threadId: string }>, res) => {
  res.json(await appServer.interrupt(String(req.params.threadId)));
});

app.delete("/threads/:threadId", requireAuth, async (req: Request<{ threadId: string }>, res, next) => {
  try {
    const threadId = String(req.params.threadId);
    const deleted = await logStore.deleteThread(threadId);
    if (!deleted) {
      writeError(res, 404, "THREAD_NOT_FOUND", "Thread was not found.");
      return;
    }
    const response: DeleteThreadResponse = { deleted, threadId };
    res.json(response);
  } catch (error) {
    next(error);
  }
});

app.get("/app", (_req, res) => {
  res.sendFile(join(publicDir, "index.html"));
});

app.use((error: unknown, _req: Request, res: Response, _next: NextFunction) => {
  const message = error instanceof Error ? error.message : String(error);
  writeError(res, 500, "BRIDGE_ERROR", message);
});

const server = http.createServer(app);
const wss = new WebSocketServer({ noServer: true });

server.on("upgrade", async (request, socket, head) => {
  const url = new URL(request.url ?? "/", `http://${request.headers.host}`);
  if (!url.pathname.startsWith("/threads/") || !url.pathname.endsWith("/events")) {
    socket.destroy();
    return;
  }
  const token = url.searchParams.get("token") ?? bearerToken(request.headers.authorization);
  const device = await authStore.verifyToken(token ?? undefined);
  if (!device) {
    socket.destroy();
    return;
  }
  wss.handleUpgrade(request, socket, head, (ws) => {
    wss.emit("connection", ws, request, device);
  });
});

wss.on("connection", async (ws, request) => {
  const url = new URL(request.url ?? "/", `http://${request.headers.host}`);
  const threadId = String(url.pathname.split("/")[2]);
  let watcher: FSWatcher | null = null;

  const send = (envelope: SocketEnvelope) => {
    if (ws.readyState === ws.OPEN) ws.send(JSON.stringify(envelope));
  };

  try {
    const snapshot = await logStore.getThreadEvents(threadId);
    send({ type: "snapshot", thread: snapshot.thread, events: snapshot.events });
    const threadPath = await logStore.findThreadPath(threadId);
    if (threadPath) {
      watcher = chokidar.watch(threadPath, { ignoreInitial: true, awaitWriteFinish: { stabilityThreshold: 300, pollInterval: 100 } });
      watcher.on("change", async () => {
        try {
          const latest = await logStore.getThreadEvents(threadId);
          send({ type: "snapshot", thread: latest.thread, events: latest.events });
        } catch (error) {
          send({ type: "error", message: error instanceof Error ? error.message : String(error) });
        }
      });
    }
  } catch (error) {
    send({ type: "error", message: error instanceof Error ? error.message : String(error) });
  }

  ws.on("close", () => {
    void watcher?.close();
  });
});

server.listen(config.port, config.host, () => {
  printStartup();
});

async function requireAuth(req: Request, res: Response, next: NextFunction): Promise<void> {
  const token = bearerToken(req.header("authorization"));
  const device = await authStore.verifyToken(token);
  if (!device) {
    writeError(res, 401, "AUTH_REQUIRED", "A valid paired device token is required.");
    return;
  }
  res.locals.device = device;
  next();
}

function statusPayload(): BridgeStatus {
  const transports: BridgeStatus["transports"] = config.publicUrl ? ["direct_lan", "virtual_lan", "relay"] : ["direct_lan", "virtual_lan"];
  return {
    bridgeName: config.name,
    version: "1.0.0",
    host: config.host,
    port: config.port,
    addresses,
    publicUrl: config.publicUrl,
    transports,
    codexAppServer: {
      enabled: config.enableAppServer,
      available: appServer.available,
      lastError: appServer.lastError,
      ...appServer.diagnostics
    }
  };
}

function pairingPayload(): PairingInfo {
  ensurePairingFresh();
  const transports: PairingInfo["transports"] = config.publicUrl ? ["direct_lan", "virtual_lan", "relay"] : ["direct_lan", "virtual_lan"];
  return {
    bridgeName: config.name,
    pairToken: pairing.pairToken,
    expiresAt: pairing.expiresAt.toISOString(),
    addresses,
    port: config.port,
    publicUrl: config.publicUrl,
    transports,
    qrPayload: JSON.stringify({
      type: "codex-companion-pairing",
      bridgeName: config.name,
      addresses,
      port: config.port,
      publicUrl: config.publicUrl,
      pairToken: pairing.pairToken,
      expiresAt: pairing.expiresAt.toISOString(),
      transports
    })
  };
}

function ensurePairingFresh(): void {
  if (pairing.expiresAt.getTime() <= Date.now()) {
    pairing = createPairToken();
  }
}

function bearerToken(header: string | undefined): string | undefined {
  if (!header) return undefined;
  const match = header.match(/^Bearer\s+(.+)$/i);
  return match?.[1];
}

function writeError(res: Response, status: number, code: string, message: string): void {
  const body: ApiError = { error: { code, message } };
  res.status(status).json(body);
}

function printStartup(): void {
  const payload = pairingPayload();
  console.log(`\n${config.name} is running.`);
  console.log(`Codex home: ${config.codexHome}`);
  console.log(`Bridge data: ${config.dataDir}`);
  console.log(`Pairing token expires at: ${payload.expiresAt}`);
  console.log("\nConnect from Android with one of these addresses:");
  for (const address of addresses) console.log(`  http://${address}:${config.port}`);
  if (addresses.length === 0) console.log(`  http://<computer-ip>:${config.port}`);
  if (config.publicUrl) console.log(`  ${config.publicUrl}`);
  console.log(`\nPair token: ${payload.pairToken}\n`);
  qrcode.generate(payload.qrPayload, { small: true });
}
