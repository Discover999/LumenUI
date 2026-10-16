<div align="center">

<img src="docs/icon.svg" width="96" alt="LumenUI" />

# LumenUI

**连接外部 mihomo（Clash.Meta）后端的原生 Android 客户端**，内置 zashboard 面板。

[![Android CI](https://github.com/Discover999/LumenUI/actions/workflows/android.yml/badge.svg)](https://github.com/Discover999/LumenUI/actions/workflows/android.yml)
[![minSdk](https://img.shields.io/badge/minSdk-26-blue)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

</div>

---

## ✨ 功能

| 能力 | 说明 |
| :--- | :--- |
| 内置面板 | 打包 zashboard 3.29.1，从 APK assets 提供，离线可用 |
| 多后端管理 | 增删改与切换；密钥经 Keystore 加密，从不进入 WebView URL |
| 连通性测试 | 48 个平台 / 7 个分类，并发探测，附出口 IP 与 IP 信誉 |
| DNS 泄露检测 | 走 bash.ws 公开测试，给出结论与解析器的国家 / 地区 |
| 原生顶栏 | 主页、刷新、出口 IP、后端切换与功能菜单 |
| 桌面快捷方式 | 长按图标直达「连通性测试」/「DNS 泄露检测」 |

---

## 🚀 构建

需要 JDK 17 与 Android SDK（`platforms;android-37.0`、`build-tools;36.1.0`）。

```bash
./gradlew :app:assembleDebug        # 调试包
./gradlew :app:testDebugUnitTest    # 单元测试
./gradlew :app:lintDebug            # Lint
```

发布包需要仓库外的 `keystore.properties`（已在 `.gitignore` 中），否则不签名。

---

## 📦 目录结构

```
app/src/main/
├─ assets/zashboard/   # 内置面板（zashboard 3.29.1）
├─ java/com/lumenui/app/
│  ├─ data/            # 后端、加密、探活、连通性与 DNS 探测
│  ├─ di/              # 手写依赖容器
│  ├─ web/             # AssetLoader、注入脚本、面板控制器
│  ├─ util/            # 日志
│  └─ ui/              # 界面、组件、主题
└─ res/                # 主题、图标、快捷方式、网络安全配置
```

---

## 🙏 致谢

| 项目 | 用途 | 许可 |
| :--- | --- | :---: |
| [zashboard](https://github.com/Zephyruso/zashboard) | 内置控制面板 | MIT |
| [Lucide](https://lucide.dev) | 界面图标 | ISC |
| [Simple Icons](https://simpleicons.org) | 品牌图标 | CC0-1.0 |
| [FlagKit](https://github.com/acarlsen/kmp-flagkit) | 国家 / 地区旗帜 | MIT |

以上项目的著作权均归各自作者所有。

---

## 📄 许可证

本项目基于 **[MIT 许可证](LICENSE)** 发布。
