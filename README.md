<div align="center">

# LumenUI

**连接外部 mihomo（Clash.Meta）后端的 Android 纯客户端**

不内置内核、不跑代理，只把 [zashboard](https://github.com/Zephyruso/zashboard) 面板打包进 APK，
并提供原生顶栏、多后端管理与连通性测试。

[![Android CI](https://github.com/Discover999/LumenUI/actions/workflows/android.yml/badge.svg)](https://github.com/Discover999/LumenUI/actions/workflows/android.yml)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com)
[![minSdk](https://img.shields.io/badge/minSdk-26-blue)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

</div>

> 需自备后端：在 mihomo 中开放 external-controller（默认 `:9090`）。

## 功能

- **内置离线面板**：zashboard 随 APK 分发（`assets/zashboard`），启动即用，不联网加载前端资源。
- **多后端管理**：增删改查与一键切换；`/version` 探活与延迟；Secret 用 Android Keystore 加密。
- **面板双向同步**：可读取 / 写回面板自身的后端列表（`setup/api-list`）。
- **连通性测试**：40+ 平台的流媒体 / 地区解锁检测（Cloudflare `trace` + 各平台专用探测），默认 17 项，可自选。
- **原生外壳**：毛玻璃顶栏与悬浮弹层（Jetpack Compose）；深色 / 浅色、边到边、跟随系统主题。

**顶栏**（从左到右）：状态灯 + 后端名（点击切换）、🏠 返回面板、🌐 IP 检测（`ip.skk.moe`）、刷新、☰ 菜单（连通性测试 / 在浏览器打开 / 关于）。

## 构建

需要 JDK 17 与 Android SDK（`platforms;android-36`、`build-tools;36.1.0`），在 `local.properties` 里配置 `sdk.dir=<SDK 路径>`（不提交）。

```bash
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

release 未配置签名，`assembleRelease` 产出未签名 APK，需自行签名。

## 技术栈

Kotlin + Jetpack Compose (Material 3) · WebView + `WebViewAssetLoader` · OkHttp + kotlinx.serialization · SharedPreferences + Keystore (AES/GCM) · Gradle 8.13 + AGP 8.11 · minSdk 26 / compileSdk 36

## 目录结构

```
app/src/main/
├─ assets/zashboard/   # 内置面板
├─ java/com/lumenui/app/
│  ├─ data/            # 后端模型与仓库、Keystore 加密、探活、连通性探测
│  ├─ di/              # 手写依赖容器
│  ├─ web/             # AssetLoader、面板 URL、面板控制器
│  └─ ui/              # 界面、组件、主题
└─ res/                # 主题、图标、网络安全配置
```

## 实现说明

- 面板挂在虚拟域名 `panel.internal`，由 `WebViewAssetLoader` 本地拦截并从 APK assets 返回，不经过网络。
- 面板使用 **http 源**（`setHttpAllowed(true)`）：与 http / ws 后端同源，避免混合内容拦截，也不触发 zashboard 的「https 面板连 http 后端」提示。
- 该源为非安全上下文，`navigator.clipboard` 不可用，复制走 `document.execCommand` 回退。

## 致谢 / 许可证

内置面板 [zashboard](https://github.com/Zephyruso/zashboard)；图标 [Lucide](https://lucide.dev) / [Simple Icons](https://simpleicons.org) / [FlagKit](https://github.com/linb/FlagKit)。本项目以 [MIT](LICENSE) 发布。
