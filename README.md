# Codex 安卓伴侣 App

这是一个第一阶段不依赖云服务器的 Codex 手机伴侣工程。它支持两种连接方式：

- 同一 Wi-Fi 局域网直连
- Tailscale / ZeroTier 虚拟局域网 IP 直连

整体结构：

- `apps/desktop-bridge`：运行在 Windows 电脑上的本地 Bridge，读取 Codex 会话并提供 HTTP/WebSocket 接口。
- `apps/android`：Kotlin + Jetpack Compose 安卓 App。
- `packages/protocol`：电脑端、安卓端、未来 Relay 共用的数据协议。

## 电脑端 Bridge

双击运行：

```text
启动电脑端页面.bat
```

它会启动 Bridge 并打开电脑端页面：

```text
http://127.0.0.1:4518/app
```

也可以手动运行：

```powershell
npm install
npm run dev:bridge
```

启动后终端会显示局域网地址和配对 token。安卓端可以手动输入地址，也可以后续接入二维码扫码。

默认端口是 `4518`，可用环境变量覆盖：

```powershell
$env:BRIDGE_PORT="4518"
$env:BRIDGE_HOST="0.0.0.0"
npm run dev:bridge
```

发送消息需要 Codex App Server 可启动。Bridge 会尝试运行：

```powershell
codex app-server
```

如果当前 Windows 环境阻止直接运行 `codex.exe`，Bridge 仍可读取历史会话，但发送消息会返回“App Server 不可用”。

## 安卓端

安卓工程位于 `apps/android`。当前环境没有 Gradle 和 Android SDK，建议用 Android Studio 打开该目录，并使用 JDK 17 编译运行。

App 第一版能力：

- 手动输入电脑 Bridge 地址
- 使用配对 token 配对
- 保存连接 token
- 查看会话列表
- 查看会话详情和工具/思考/错误事件
- 通过 Bridge 向 Codex 线程发送消息

## 安全边界

- Bridge 默认只适合局域网或 Tailscale / ZeroTier 私网。
- 不要把 Bridge 端口直接暴露到公网。
- 首次访问需要配对 token，后续接口需要设备 token。
