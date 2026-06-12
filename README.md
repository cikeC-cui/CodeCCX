# CodeCCX

[English](README.md) | [简体中文](README.zh-CN.md)

CodeCCX is a local-first Android companion for OpenAI Codex. It lets you view, monitor, and continue your Codex sessions from your phone while the actual Codex data stays on your own Windows computer.

## Why CodeCCX

Codex is powerful on desktop, but many real workflows are not always in front of the computer:

- You start a long Codex task and want to check progress from your phone.
- You want to continue a session while away from the desk.
- You want a simple mobile monitor for reasoning, tool calls, errors, and final replies.
- You do not want to expose your Codex credentials to a third-party remote UI.

CodeCCX solves this by running a small Bridge on your Windows computer. Your Android phone connects to that Bridge over LAN or a virtual private network such as Tailscale / ZeroTier.

## Security First

CodeCCX is designed around a simple rule: your Codex data should stay on your machine.

- No cloud relay is required in the current version.
- No Codex token is uploaded to a third-party service.
- The Bridge reads local Codex session files from your computer.
- Pairing uses a short-lived token and QR code.
- Paired devices use their own device token.
- LAN / Tailscale / ZeroTier is recommended.
- Do not expose the Bridge port directly to the public internet.

## Current Features

- Windows Desktop Bridge with local web console.
- Android App for viewing and continuing Codex sessions.
- LAN pairing with token or QR code.
- Virtual-LAN pairing for Tailscale / ZeroTier addresses.
- Session list, session detail, and event filters.
- Message, reasoning, tool call, status, and error views.
- WebSocket refresh for active sessions.
- Codex App Server integration for sending messages to an existing thread.
- Interrupt current Codex turn from phone or web.
- Automatic Codex home detection with manual fallback.
- Release-ready EXE and APK artifacts.

## Preview

Screenshots and demo video will be added here.

Suggested demo flow:

1. Start `CodeCCX-Bridge.exe` on Windows.
2. Scan the LAN or virtual-LAN QR code from Android.
3. Open a Codex session on phone.
4. Send a message from Android.
5. Watch the reply complete in real time.

## Quick Start

### Option A: Download Release

Download the latest release assets:

- `CodeCCX-Bridge.exe` for Windows.
- `CodeCCX-Android.apk` for Android.

Then:

1. Run `CodeCCX-Bridge.exe` on your Windows computer.
2. Open the web console at `http://127.0.0.1:4518/app`.
3. Install `CodeCCX-Android.apk` on your phone.
4. Scan the pairing QR code from the web console or terminal.

Phone and computer must be on the same Wi-Fi, or both must join the same Tailscale / ZeroTier network.

### Option B: Run From Source

```powershell
git clone https://github.com/cikeC-cui/CodeCCX.git
cd CodeCCX
npm install
npm run build
npm run start:bridge
```

The Bridge web console opens at:

```text
http://127.0.0.1:4518/app
```

When connecting from your phone, do not use `127.0.0.1`. Use your computer LAN IP or virtual-LAN IP, for example:

```text
http://192.168.1.15:4518
http://100.x.x.x:4518
```

## Android Setup

Requirements:

- Android Studio.
- Android SDK 35.
- JDK 17 or newer.
- Android 7.0 or newer phone/emulator.

Steps:

1. Start the Desktop Bridge first.
2. Open `apps/android` in Android Studio.
3. Wait for Gradle sync.
4. Select a phone or emulator.
5. Run the app.
6. Pair with the Bridge URL and token, or scan the QR code.

## Project Structure

```text
CodeCCX/
  apps/
    desktop-bridge/      Windows Bridge, local web console, release artifacts
    android/             Android companion app
  packages/
    protocol/            Shared API and WebSocket protocol types
  启动电脑端页面.bat       Windows one-click launcher
  package.json           npm workspace configuration
```

## Common Commands

Check TypeScript:

```powershell
npm run check
```

Build shared protocol and desktop Bridge:

```powershell
npm run build
```

Start the built Bridge:

```powershell
npm run start:bridge
```

Run Bridge in development mode:

```powershell
npm run dev:bridge
```

Build the Windows EXE:

```powershell
npm run build:bridge:exe
```

The EXE is generated at:

```text
apps/desktop-bridge/release/CodeCCX-Bridge.exe
```

## Configuration

The Desktop Bridge can be configured with environment variables:

```powershell
$env:BRIDGE_PORT="4518"
$env:BRIDGE_HOST="0.0.0.0"
$env:BRIDGE_NAME="My Codex Bridge"
$env:BRIDGE_PUBLIC_URL="https://example.com"
$env:CODEX_HOME="$env:USERPROFILE\.codex"
$env:BRIDGE_DATA_DIR="$env:USERPROFILE\.codex-companion"
$env:CODEX_COMMAND="codex"
$env:BRIDGE_DISABLE_APP_SERVER="1"
npm run start:bridge
```

Options:

- `BRIDGE_PORT`: Bridge port. Default: `4518`.
- `BRIDGE_HOST`: Bridge host. Default: `0.0.0.0`.
- `BRIDGE_NAME`: Name shown during pairing.
- `BRIDGE_PUBLIC_URL`: Reserved for future public/relay transport.
- `CODEX_HOME`: Codex data directory.
- `BRIDGE_DATA_DIR`: Paired device data directory.
- `CODEX_COMMAND`: Command used to start Codex App Server.
- `BRIDGE_DISABLE_APP_SERVER=1`: Read-only mode. View sessions without sending messages.

## Codex Data Detection

Bridge tries to find Codex data automatically from:

- `CODEX_HOME`.
- Previously saved path in the web console.
- `%USERPROFILE%\.codex`.
- Common OpenAI / Codex local directories.

If detection fails, open the web console and save the correct `.codex` folder path manually.

## Codex App Server

To send messages from phone/web back into an existing Codex thread, Bridge tries to start:

```powershell
codex app-server
```

If App Server is unavailable, CodeCCX can still read and display local session history. Sending messages and interrupting turns require App Server support.

## Release Checklist

Before publishing a GitHub Release:

1. Build the Bridge:

   ```powershell
   npm run build:bridge:exe
   ```

2. Build or copy the latest Android APK to:

   ```text
   apps/desktop-bridge/release/CodeCCX-Android.apk
   ```

3. Upload these assets to GitHub Releases:

   ```text
   apps/desktop-bridge/release/CodeCCX-Bridge.exe
   apps/desktop-bridge/release/CodeCCX-Android.apk
   ```

4. Write release notes with:

   - New features.
   - Fixed bugs.
   - Upgrade notes.
   - Known issues.

## FAQ

### Phone cannot open the Bridge URL

- Make sure the phone and computer are on the same Wi-Fi, or the same Tailscale / ZeroTier network.
- Use the computer IP address, not `127.0.0.1`.
- Check that Windows Firewall allows access to the Bridge port.
- Confirm the port shown by the terminal matches the port entered in the app.

### Pairing fails

- Refresh the pairing page or restart Bridge to get a new token.
- Make sure the token has not expired.
- Remove extra spaces from the Bridge URL.
- Prefer QR pairing when possible.

### Sessions are visible but sending does not work

- Run `codex app-server` manually in a terminal to confirm it works.
- Make sure `BRIDGE_DISABLE_APP_SERVER` is not set.
- If you only need monitoring, read-only mode is enough.

### Android Studio sync fails

- Use JDK 17 or newer.
- Install Android SDK 35.
- Make sure Gradle and Android dependencies can be downloaded.

## Roadmap

- Push notifications when Codex completes a turn.
- Better mobile background refresh.
- PWA mode for iOS and browser-only mobile access.
- Multi-computer management.
- Optional HTTPS for local Bridge.
- Device permission levels: read-only / send messages / interrupt.
- More AI coding agent integrations.
- Optional relay transport with end-to-end encryption.

## Status

CodeCCX is early-stage software. It is useful today for local monitoring and Android companion workflows, but the Codex App Server integration may change as Codex evolves.

Contributions, testing feedback, and security reviews are welcome.
