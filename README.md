# CodeCCX

CodeCCX 是一个让手机查看和继续操作本机 Codex 会话的伴侣项目。当前版本为 `1.0.0`，第一阶段不依赖云服务器，电脑和手机通过局域网或虚拟局域网直接连接。

## 当前能做什么

- 在 Windows 电脑上启动本地 Bridge 服务。
- 读取本机 Codex 会话列表和会话详情。
- 在浏览器页面查看会话、工具调用、思考过程、状态和错误事件。
- 通过 Android App 连接电脑端 Bridge。
- 使用配对 token 完成手机和电脑的授权。
- 保存设备 token，后续访问不需要每次重新配对。
- 通过 WebSocket 自动刷新会话详情。
- 首次启动时自动检测本机 Codex 数据目录；如果检测不到，可在电脑端页面手动保存路径。
- 尝试通过 Codex App Server 向指定线程发送消息或中断当前回复。
- 支持同一 Wi-Fi 局域网直连。
- 支持 Tailscale / ZeroTier 这类虚拟局域网 IP 直连。

## 项目结构

```text
CodeCCX/
  apps/
    desktop-bridge/      Windows 电脑端 Bridge 和本地网页
    android/             Android 手机 App
  packages/
    protocol/            电脑端和手机端共用的数据协议
  启动电脑端页面.bat       Windows 一键启动脚本
  package.json           Node 工作区配置
```

## 需要准备的环境

电脑端需要：

- Windows 10 或 Windows 11。
- Node.js `20` 或更高版本。
- npm，通常会随 Node.js 一起安装。
- Git，用于下载和更新项目。
- Codex Desktop 或 Codex CLI，并且当前用户目录下已经有 Codex 会话数据。

Android 端需要：

- Android Studio。
- JDK 17。
- Android SDK 35。
- Android 7.0 或更高版本的真机或模拟器。

可选网络环境：

- 手机和电脑在同一个 Wi-Fi。
- 或者手机和电脑都加入同一个 Tailscale / ZeroTier 网络。

## 第一次下载项目

```powershell
git clone https://github.com/cikeC-cui/CodeCCX.git
cd CodeCCX
npm install
```

如果已经下载过项目，后续更新代码时先运行：

```powershell
git pull --rebase
npm install
```

## 启动电脑端

最简单的方式是在项目根目录双击：

```text
启动电脑端页面.bat
```

脚本会自动安装依赖、构建项目、启动 Bridge，并打开电脑端页面：

```text
http://127.0.0.1:4518/app
```

也可以手动启动：

```powershell
npm install
npm run build
npm run start:bridge
```

开发时可以运行：

```powershell
npm run dev:bridge
```

启动成功后，终端会显示：

- 本机可访问地址，例如 `http://192.168.x.x:4518`。
- 一次性配对 token。
- 用于配对的二维码内容。

Bridge 会优先自动识别当前用户的 Codex 数据目录。默认会检查：

- 环境变量 `CODEX_HOME` 指定的位置。
- 已在电脑端页面保存过的位置。
- `%USERPROFILE%\.codex`。
- 常见的 OpenAI/Codex 本地目录。

如果没有自动识别到，电脑端页面右侧会显示“Codex 数据目录”提示。把资源管理器里的 `.codex` 文件夹路径复制进去并保存即可，保存后会自动刷新会话列表。

手机连接时不要使用 `127.0.0.1`，要使用电脑在局域网或虚拟局域网里的 IP 地址。

## 启动 Android App

1. 先启动电脑端 Bridge。
2. 用 Android Studio 打开 `apps/android`。
3. 等待 Gradle 同步完成。
4. 选择真机或模拟器。
5. 点击 Run 启动 App。
6. 在 App 中输入电脑端 Bridge 地址，例如 `http://192.168.x.x:4518`。
7. 输入电脑端终端里显示的配对 token，或使用二维码配对。

如果使用 Tailscale / ZeroTier，把地址换成电脑的虚拟局域网 IP，例如：

```text
http://100.x.x.x:4518
```

## 常用命令

```powershell
npm run check
```

检查 TypeScript 类型。

```powershell
npm run build
```

构建共用协议包和电脑端 Bridge。

```powershell
npm run start:bridge
```

运行已构建的电脑端 Bridge。

```powershell
npm run dev:bridge
```

以开发模式运行电脑端 Bridge。

## 生成一键运行 EXE

在项目根目录运行：

```powershell
npm run build:bridge:exe
```

生成完成后，电脑端 Bridge 可执行文件会输出到：

```text
apps/desktop-bridge/release/CodeCCX-Bridge.exe
```

这个 exe 会内置 Node 运行时和电脑端网页资源。首次运行时仍会自动检测 Codex 数据目录；如果检测不到，可以打开电脑端页面后在右侧填写 `.codex` 路径。

## 发布 GitHub Release

先在项目根目录生成 exe：

```powershell
npm run build:bridge:exe
```

然后按以下步骤在 GitHub 上创建 Release：

1. 打开 [Releases 页面](https://github.com/cikeC-cui/CodeCCX/releases)
2. 点击 **Draft a new release**
3. 填写信息：
   - **Tag**：用版本号，例如 `v1.0.0`（首次发布点 **Create new tag**）
   - **Release title**：`CodeCCX v1.0.0`
   - **Description**：写此版本的主要更新内容
4. 在 **Attach binaries** 区域，上传刚生成的文件：
   ```text
   apps/desktop-bridge/release/CodeCCX-Bridge.exe
   ```
5. 点击 **Publish release**

发布后，其他人就可以从 Release 页面直接下载 `CodeCCX-Bridge.exe`，双击运行，不需要安装 Node 或 clone 项目。

## 可配置项

电脑端 Bridge 支持用环境变量调整配置：

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

常见配置说明：

- `BRIDGE_PORT`：Bridge 监听端口，默认 `4518`。
- `BRIDGE_HOST`：Bridge 监听地址，默认 `0.0.0.0`。
- `BRIDGE_NAME`：配对时显示的电脑端名称。
- `BRIDGE_PUBLIC_URL`：未来接入公网或 Relay 时使用的公开地址。
- `CODEX_HOME`：Codex 本地数据目录。未配置时 Bridge 会自动检测，也可以在电脑端页面手动保存。
- `BRIDGE_DATA_DIR`：Bridge 保存已配对设备的目录。
- `CODEX_COMMAND`：启动 Codex App Server 的命令。
- `BRIDGE_DISABLE_APP_SERVER=1`：只查看历史会话，不尝试发送消息到 Codex。

## Codex App Server 说明

Bridge 会尝试启动：

```powershell
codex app-server
```

如果这个命令可用，手机端就可以把消息发送到已有 Codex 线程。若当前 Windows 权限或 Codex 安装方式阻止 Bridge 启动 `codex.exe`，历史会话仍然可以查看，但发送消息会显示 App Server 不可用。

## 常见问题

手机打不开电脑地址：

- 确认手机和电脑在同一个 Wi-Fi，或在同一个 Tailscale / ZeroTier 网络。
- 确认手机访问的是电脑 IP，不是 `127.0.0.1`。
- 确认 Windows 防火墙允许 Node.js 或当前端口访问。
- 确认终端里显示的端口和 App 输入的端口一致。

配对失败：

- 配对 token 有有效期，过期后刷新电脑端 `/pair` 或重启 Bridge。
- 确认手机输入的 Bridge 地址没有多余空格。
- 确认电脑端终端里的 token 和手机端输入一致。

能看历史，不能发送消息：

- 确认 `codex app-server` 能在终端里手动运行。
- 如果暂时只需要查看历史，可以设置 `BRIDGE_DISABLE_APP_SERVER=1`。

Android Studio 同步失败：

- 确认 JDK 是 17。
- 确认 Android SDK 35 已安装。
- 确认网络能下载 Gradle 和 Android 依赖。

## 安全边界

- Bridge 默认只适合局域网或 Tailscale / ZeroTier 私网使用。
- 不要把 Bridge 端口直接暴露到公网。
- 首次配对需要一次性 token。
- 配对成功后，接口访问需要设备 token。
- 设备 token 保存在电脑端 `BRIDGE_DATA_DIR` 下。
- 后续如果购买服务器，应新增 Relay transport，不直接暴露本机 Bridge。
