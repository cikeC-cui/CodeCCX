# Desktop Bridge

The Bridge runs on the Windows computer that already has Codex Desktop / Codex CLI installed.

## Run

From the repository root, double-click:

```text
启动电脑端页面.bat
```

Or run manually:

```powershell
npm install
npm run dev:bridge
```

The terminal prints:

- LAN / virtual LAN URLs, for example `http://192.168.1.15:4518`
- A one-time pairing token
- A QR code with the same pairing payload

Use the LAN address when the phone and computer are on the same Wi-Fi. Use the Tailscale / ZeroTier address when both devices are joined to the same virtual network.

## API

- `GET /health`
- `GET /pair`
- `POST /pair`
- `GET /threads`
- `GET /threads/:threadId/events`
- `WS /threads/:threadId/events?token=...`
- `POST /threads/:threadId/send`

All endpoints except `/health` and `/pair` require:

```text
Authorization: Bearer <device-token>
```

## Current Codex Integration

Historical viewing reads local Codex JSONL files from:

```text
%USERPROFILE%\.codex\sessions
%USERPROFILE%\.codex\session_index.jsonl
```

Sending messages tries to use:

```powershell
codex app-server
```

On this machine, direct CLI startup currently returns `spawn EPERM`, so the Bridge exposes the send endpoint but reports App Server unavailable until Codex CLI can be launched by the Bridge process.
