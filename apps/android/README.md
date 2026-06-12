# Android App

This is the mobile client for Codex Companion. It connects to the desktop Bridge and brings the core web-console features into the Android app:

- Manual Bridge URL entry
- QR pairing payload scanning
- Device token storage
- Thread list with search
- Thread details with WebSocket refresh
- Event filters for messages, reasoning, tools, status, and errors
- Conversation statistics
- Codex App Server diagnostics
- Sending messages through the Bridge
- Interrupting the current Codex turn

## Run The Desktop Bridge First

From the repository root, either double-click:

```text
启动电脑端页面.bat
```

or run:

```powershell
npm.cmd install
npm.cmd run build
npm.cmd run start:bridge
```

The Bridge web page opens at:

```text
http://127.0.0.1:4518/app
```

Use the LAN address printed by the Bridge, for example `http://192.168.x.x:4518`, when connecting from a phone or emulator.

## Run The Android App

Requirements:

- Android Studio
- Android SDK 35
- JDK 17
- A phone or emulator running Android 7.0+

Steps:

1. Open `apps/android` in Android Studio.
2. Let Android Studio sync Gradle.
3. Select a device or emulator.
4. Click Run.
5. In the app, enter the Bridge LAN URL and pair token, or scan the Bridge QR payload.

For Tailscale or ZeroTier mode, enter the virtual-network IP with the same Bridge port, for example `http://100.x.x.x:4518`.
