<div align="center">

<img src="docs/app-icon.svg" width="96" height="96" alt="123 图标" />

# 123

**原生 Android 视频客户端** · Jetpack Compose · Material 3 · Material You

![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose%20BOM-2026.09.00-4285F4?logo=jetpackcompose&logoColor=white)
![AGP](https://img.shields.io/badge/AGP-9.4.1-3DDC84?logo=androidstudio&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-26-555555)
![targetSdk](https://img.shields.io/badge/targetSdk-36-555555)
![Version](https://img.shields.io/badge/release-v0.1.2-FF5B79)

</div>

> 🤖 **AI 全程开发**：架构、编码、调试与文档均在 AI 编程助手协作下完成。
>
> 📺 **定位说明**：基于 Jetpack Compose 与 Media3 构建的通用网络视频播放客户端示例，仅加载公开媒体流配置，不生产、不存储、不上传任何视频文件。

---

## ✨ 功能

| 🔗 | 模块 | 说明 |
| :---: | :--- | :--- |
| 🎬 | 内容浏览 | 首页精选轮播与信息流；栏目 / 分类 / 创作者 / 出品方 / 合集多维索引；关键词搜索与历史词 |
| ▶️ | 视频播放 | Media3 · ExoPlayer 播放 m3u8；选集、断点续播、片尾连播；画中画与横竖屏切换 |
| 👆 | 手势操作 | 长按 2 倍速、双击左右半屏 ±10 秒、横滑拖进度、双击中间播放/暂停；画面比例三档 |
| 💾 | 离线缓冲 | ExoPlayer 分片缓存 512 MB（LRU 淘汰），二次观看免重复缓冲 |
| 📚 | 本地数据 | 观看历史、收藏与自定义收藏夹、创作者关注、观看统计，全部存于本机 Room |
| 🎨 | 外观 | Material You 动态取色；网格 / 单列切换；平板双栏（≥ 840dp）；全局触感反馈 |
| 🛡️ | 隐私 | 生物识别应用锁、最近任务界面隐私遮罩、全屏翻转退出 |
| 🛰️ | 网络诊断 | DNS / TCP / TLS / TTFB 分段耗时，一键生成环境报告；全站共享 OkHttp 连接池 |

---

## 🧱 技术栈

| 层 | 选型 |
| :--- | :--- |
| UI | Jetpack Compose（BOM 2026.09.00）· Material 3 · Material Symbols · Haze 模糊底栏 |
| 架构 | 单 Activity + MVVM（ViewModel + StateFlow）· Navigation Compose 2.9.8 |
| 存储 | Room 2.8.5 · DataStore Preferences · kotlinx.serialization |
| 网络 | OkHttp 5.5 · Jsoup 1.23 · 无头 WebView 媒体流解析 |
| 播放 | Media3 / ExoPlayer 1.11.1（HLS + SimpleCache）· Coil 2.7 |

---

## 🚀 构建

```bash
./gradlew installDebug        # 安装 Debug 到已连接设备
./gradlew testDebugUnitTest   # 运行单元测试
./gradlew assembleRelease     # Release 构建（R8 混淆 + 资源压缩）
```

| 项 | 要求 |
| :--- | :--- |
| JDK | **17** |
| SDK | compileSdk 37 · targetSdk 36 · minSdk 26（Android 8.0） |

> 💡 发布前请按 [发布检查清单](docs/p5-release-checklist.md) 完成真机冒烟与回滚验证。

---

## 📦 项目结构

```text
app/src/main/java/com/av123/video/
├── MainActivity.kt / VideoHubApp.kt
├── data/
│   ├── model/         # 领域模型
│   ├── source/        # 站点适配器、Jsoup 解析、远程站点配置
│   ├── net/           # OkHttp、统一错误、网络探针
│   ├── repository/    # 首页 / 详情 / 历史 / 收藏 / 关注 / 统计 / 备份
│   ├── db/            # Room 实体、DAO、迁移
│   ├── playback/      # ExoPlayer 分片缓存
│   ├── prefs/         # DataStore 设置
│   └── diagnostics/   # 诊断指标
└── ui/
    ├── screens/       # 首页 / 栏目 / 搜索 / 详情 / 历史 / 收藏 / 关注 / 统计 / 存储 / 诊断 / 我的
    ├── components/    # 卡片、播放器、通用组件
    ├── navigation/    # 导航图与底栏
    ├── icons/         # Material Symbols 图标集
    ├── lock/          # 生物识别应用锁
    ├── theme/         # 配色、字体、形状、动效
    └── util/          # 窗口尺寸、时间格式、触感
```

单元测试位于 `app/src/test/`，与主源码包结构一一对应。

---

## 🔐 权限

| 权限 | 用途 |
| :--- | :--- |
| `INTERNET` | 媒体列表加载、封面显示与视频流播放 |
| `ACCESS_NETWORK_STATE` | 全屏顶栏显示当前网络类型（Wi-Fi / 流量） |
| `USE_BIOMETRIC` | 应用锁生物识别验证，设备凭据由系统弹窗兜底 |

不申请存储、位置、通讯录等权限；历史与收藏仅存本机，迁移请使用「数据与备份 → 导出 JSON」。

---

## 📄 免责声明

1. 本项目仅用于 Jetpack Compose、Media3 与现代 Android 架构的**个人学习与技术研究**。
2. 应用内展示的示例媒体信息与播放流均来自外部公开站点或自定义配置，相关版权归原站点及权利人所有。
3. 请在合法合规的前提下使用与扩展本项目，因自行修改配置或接入第三方数据源产生的一切后果由使用者自行承担。

<div align="center">

**123** · 仅用于技术交流

</div>
