# CodeCCX 安装指南

## 第一步：下载组网工具

手机和电脑都下载 Tailscale：

- [Windows 下载](https://github.com/cikeC-cui/CodeCCX/raw/main/apps/desktop-bridge/release/tailscale-setup-1.98.4.exe)
- [Android 下载](https://github.com/cikeC-cui/CodeCCX/raw/main/apps/desktop-bridge/release/tailscale-android-universal-1.98.4.apk)

安装后分别登录同一个 Tailscale 账号。

> Tailscale 只用于手机和电脑之间的虚拟组网，不影响你正常的网络使用。

## 第二步：启动电脑端 Bridge

下载并双击运行：

[CodeCCX-Bridge.exe](https://github.com/cikeC-cui/CodeCCX/raw/main/apps/desktop-bridge/release/CodeCCX-Bridge.exe)

启动后会自动打开浏览器页面。终端里会显示二维码和配对码。

## 第三步：安装手机 App

下载并安装：

[CodeCCX-Android.apk](https://github.com/cikeC-cui/CodeCCX/raw/main/apps/desktop-bridge/release/CodeCCX-Android.apk)

## 第四步：组网连接

确保手机和电脑都启动了 Tailscale 且在线。

## 第五步：扫码配对

1. 打开手机 App，点击 **扫码** 按钮
2. 扫描电脑端页面或终端里显示的二维码
3. App 自动填入地址和配对码，点击连接即可

---

配对成功后，只要 Tailscale 在线，手机就能随时查看和继续操作电脑上的 Codex 会话。
