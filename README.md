<div align="center">

<img src="docs/icon.svg" width="96" alt="LumenUI" />

# LumenUI

**[zashboard](https://github.com/Zephyruso/zashboard) 的 APK 简单预览** —— 连接外部 mihomo（Clash.Meta）后端，提供原生顶栏、多后端管理与连通性测试。

[![Android CI](https://github.com/Discover999/LumenUI/actions/workflows/android.yml/badge.svg)](https://github.com/Discover999/LumenUI/actions/workflows/android.yml)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com)
[![minSdk](https://img.shields.io/badge/minSdk-26-blue)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

</div>

## 📦 目录结构

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

---

## 🙏 致谢

| 项目 | 用途 | 许可 |
| :--- | --- | :---: |
| <img src="https://cdn.simpleicons.org/github/8b949e" width="14" alt="GitHub"> [zashboard](https://github.com/Zephyruso/zashboard) | 内置控制面板 | MIT |
| <img src="https://cdn.simpleicons.org/github/8b949e" width="14" alt="GitHub"> [Lucide](https://lucide.dev) | 界面图标（`icons-lucide-cmp`） | ISC |
| <img src="https://cdn.simpleicons.org/github/8b949e" width="14" alt="GitHub"> [Simple Icons](https://simpleicons.org) | 品牌图标（`simple-icons`） | CC0-1.0 |
| <img src="https://cdn.simpleicons.org/github/8b949e" width="14" alt="GitHub"> [FlagKit](https://github.com/acarlsen/kmp-flagkit) | 国家 / 地区旗帜（`dev.carlsen.flagkit`） | MIT |

- 图标封装库（`icons-lucide-cmp` / `simple-icons`）自身均为 MIT。
- 内置面板自带的第三方组件（Earth 贴图、DB-IP 数据等，CC BY 4.0）见 [`assets/zashboard/THIRD_PARTY_NOTICES.md`](app/src/main/assets/zashboard/THIRD_PARTY_NOTICES.md)。
- 以上项目的著作权均归各自作者所有。

---

## 📄 许可证

本项目基于 **[MIT 许可证](LICENSE)** 发布。
