# CodeCCX

[English](README.md) | [简体中文](README.zh-CN.md)

CodeCCX 是一个本地优先的 OpenAI Codex Android 手机伴侣。它让你在手机上查看、监控并继续电脑上的 Codex 会话，同时让真实的 Codex 数据留在你自己的 Windows 电脑上。

## 为什么需要 CodeCCX

Codex 在电脑端很强，但真实开发流程并不总是坐在电脑前：

- 你启动了一个较长的 Codex 任务，想用手机看进度。
- 你离开电脑后，仍想继续某个 Codex 会话。
- 你想在手机上查看思考过程、工具调用、错误和最终回复。
- 你不想把 Codex 凭据交给第三方远程 UI。

CodeCCX 的做法是在 Windows 电脑上运行一个轻量 Bridge。Android 手机通过局域网，或 Tailscale / ZeroTier 这类虚拟局域网连接到这台电脑。

## 安全优先

CodeCCX 的设计原则很简单：你的 Codex 数据应该留在你的机器上。

- 当前版本不需要云中转。
- 不会把 Codex token 上传到第三方服务。
- Bridge 读取的是电脑本地 Codex 会话文件。
- 配对使用短有效期 token 和二维码。
- 配对成功后的设备使用独立 device token。
- 推荐使用局域网、Tailscale 或 ZeroTier。
- 不建议把 Bridge 端口直接暴露到公网。

## 当前功能

- Windows Desktop Bridge 和本地网页控制台。
- Android App 查看和继续 Codex 会话。
- 支持 token 或二维码进行局域网配对。
- 支持 Tailscale / ZeroTier 虚拟局域网地址配对。
- 会话列表、会话详情和事件筛选。
- 消息、思考、工具调用、状态和错误视图。
- WebSocket 实时刷新活跃会话。
- 通过 Codex App Server 向已有线程发送消息。
- 支持从手机或网页中断当前 Codex 回合。
- 自动检测 Codex home，检测失败时可手动配置。
- 提供可发布的 EXE 和 APK 文件。

## 预览

截图和演示视频后续会添加到这里。

建议演示流程：

1. 在 Windows 上启动 `CodeCCX-Bridge.exe`。
2. Android 扫描局域网或虚拟局域网二维码。
3. 在手机上打开 Codex 会话。
4. 从 Android 发送一条消息。
5. 实时看到 Codex 回复完成。

## 快速开始

### 方式 A：下载 Release

下载最新 Release 文件：

- `CodeCCX-Bridge.exe`：Windows 电脑端。
- `CodeCCX-Android.apk`：Android 手机端。

然后：

1. 在 Windows 电脑上运行 `CodeCCX-Bridge.exe`。
2. 打开网页控制台：`http://127.0.0.1:4518/app`。
3. 在 Android 手机上安装 `CodeCCX-Android.apk`。
4. 使用网页控制台或终端里的二维码完成配对。

手机和电脑需要在同一个 Wi-Fi，或同时加入同一个 Tailscale / ZeroTier 网络。

### 方式 B：从源码运行

```powershell
git clone https://github.com/cikeC-cui/CodeCCX.git
cd CodeCCX
npm install
npm run build
npm run start:bridge
```

Bridge 网页控制台地址：

```text
http://127.0.0.1:4518/app
```

手机连接时不要使用 `127.0.0.1`。请使用电脑的局域网 IP 或虚拟局域网 IP，例如：

```text
http://192.168.1.15:4518
http://100.x.x.x:4518
```

## Android 设置

要求：

- Android Studio。
- Android SDK 35。
- JDK 17 或更高版本。
- Android 7.0 或更高版本的手机/模拟器。

步骤：

1. 先启动 Desktop Bridge。
2. 用 Android Studio 打开 `apps/android`。
3. 等待 Gradle 同步完成。
4. 选择手机或模拟器。
5. 运行 App。
6. 输入 Bridge 地址和配对 token，或直接扫描二维码配对。

## 项目结构

```text
CodeCCX/
  apps/
    desktop-bridge/      Windows Bridge、本地网页控制台、发布文件
    android/             Android 手机伴侣 App
  packages/
    protocol/            共享 API 和 WebSocket 协议类型
  启动电脑端页面.bat       Windows 一键启动脚本
  package.json           npm workspace 配置
```

## 常用命令

检查 TypeScript：

```powershell
npm run check
```

构建共享协议包和电脑端 Bridge：

```powershell
npm run build
```

启动已构建的 Bridge：

```powershell
npm run start:bridge
```

开发模式运行 Bridge：

```powershell
npm run dev:bridge
```

构建 Windows EXE：

```powershell
npm run build:bridge:exe
```

生成位置：

```text
apps/desktop-bridge/release/CodeCCX-Bridge.exe
```

## 配置项

Desktop Bridge 支持通过环境变量配置：

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

说明：

- `BRIDGE_PORT`：Bridge 端口，默认 `4518`。
- `BRIDGE_HOST`：Bridge 监听地址，默认 `0.0.0.0`。
- `BRIDGE_NAME`：配对时显示的电脑端名称。
- `BRIDGE_PUBLIC_URL`：预留给未来公网/Relay 传输。
- `CODEX_HOME`：Codex 数据目录。
- `BRIDGE_DATA_DIR`：已配对设备数据目录。
- `CODEX_COMMAND`：用于启动 Codex App Server 的命令。
- `BRIDGE_DISABLE_APP_SERVER=1`：只读模式，只查看会话，不发送消息。

## Codex 数据目录检测

Bridge 会自动尝试从以下位置寻找 Codex 数据：

- `CODEX_HOME`。
- 网页控制台中之前保存过的路径。
- `%USERPROFILE%\.codex`。
- 常见 OpenAI / Codex 本地目录。

如果自动检测失败，请打开网页控制台并手动保存正确的 `.codex` 文件夹路径。

## Codex App Server

为了从手机或网页继续已有 Codex 线程，Bridge 会尝试启动：

```powershell
codex app-server
```

如果 App Server 不可用，CodeCCX 仍然可以读取和展示本地会话历史。发送消息和中断回合需要 App Server 支持。

## Release 发布清单

发布 GitHub Release 前：

1. 构建 Bridge：

   ```powershell
   npm run build:bridge:exe
   ```

2. 构建或复制最新 Android APK 到：

   ```text
   apps/desktop-bridge/release/CodeCCX-Android.apk
   ```

3. 上传这些文件到 GitHub Releases：

   ```text
   apps/desktop-bridge/release/CodeCCX-Bridge.exe
   apps/desktop-bridge/release/CodeCCX-Android.apk
   ```

4. Release notes 建议包含：

   - 新增功能。
   - 修复问题。
   - 升级说明。
   - 已知问题。

## 常见问题

### 手机打不开 Bridge 地址

- 确认手机和电脑在同一个 Wi-Fi，或同一个 Tailscale / ZeroTier 网络。
- 使用电脑 IP 地址，不要使用 `127.0.0.1`。
- 检查 Windows 防火墙是否允许访问 Bridge 端口。
- 确认终端显示的端口和 App 输入的端口一致。

### 配对失败

- 刷新配对页面或重启 Bridge 获取新的 token。
- 确认 token 没有过期。
- 删除 Bridge URL 中多余的空格。
- 优先使用二维码配对。

### 能看到会话，但不能发送消息

- 在终端手动运行 `codex app-server`，确认它可用。
- 确认没有设置 `BRIDGE_DISABLE_APP_SERVER`。
- 如果只需要监控，只读模式已经足够。

### Android Studio 同步失败

- 使用 JDK 17 或更高版本。
- 安装 Android SDK 35。
- 确认网络可以下载 Gradle 和 Android 依赖。

## Roadmap

- Codex 回合完成时发送手机通知。
- 更好的移动端后台刷新。
- PWA 模式，支持 iOS 和浏览器移动访问。
- 多电脑管理。
- 本地 Bridge 可选 HTTPS。
- 设备权限等级：只读 / 可发送消息 / 可中断。
- 支持更多 AI Coding Agent。
- 可选端到端加密 Relay 传输。

## 项目状态

CodeCCX 仍处于早期阶段。它现在已经可以用于本地监控和 Android 伴侣工作流，但 Codex App Server 集成可能会随着 Codex 演进而变化。

欢迎贡献、测试反馈和安全审查。
