<div align="center">

<img src="docs/app-icon.svg" width="96" height="96" alt="123 图标" />

# 123

**原生 Android 视频客户端** · Jetpack Compose · Material 3 · Material You

![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose%20BOM-2026.09.00-4285F4?logo=jetpackcompose&logoColor=white)
![AGP](https://img.shields.io/badge/AGP-9.4.1-3DDC84?logo=androidstudio&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-26-555555)
![targetSdk](https://img.shields.io/badge/targetSdk-36-555555)
![Version](https://img.shields.io/badge/release-v0.2.5-FF5B79)

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

## 🗺️ 功能清单与开发计划

> `✅` 表示已开发完成，`- [ ]` 表示待开发；后续新功能完成后可将 `- [ ]` 替换为 `✅` 归档。

### ✅ 已开发功能

#### 内容浏览与发现
- ✅ **首页信息流**：精选轮播、继续观看、热门推荐与最新上架瀑布流，支持长按卡片预览与下拉刷新
- ✅ **多维分栏索引**：支持栏目 / 分类 / 创作者 / 出品方 / 合集浏览，内置条件筛选与首末页 / 指定页码跳转
- ✅ **关键词搜索**：支持关键词与编号检索、历史搜索词持久化、单条删除与一键清空
- ✅ **详情页体验**：自适应顶栏字号、长按复制编号、点击简介直接展开/收起、关联版本快捷跳转、相关推荐与链接分享

#### 视频播放与手势交互
- ✅ **Media3 HLS 播放**：支持 `m3u8` 流媒体解析与播放、选集独立抽屉、按集断点续播、片尾自动连播、解析中排队自动起播及播完居中重播
- ✅ **画质与倍速控制**：独立倍速调节面板、清晰度轨道切换（「自动」档实时显示当前分辨率，如 `480p` / `720p` / `1080p`）
- ✅ **全屏手势操作**：长按 2 倍速、双击左右半屏 ±10 秒、双击中间播放/暂停、横滑精细拖进度（支持精细 / 适中 / 较快三档灵敏度）
- ✅ **亮度与音量手势**：全屏左右半区竖滑调节亮度与系统音量（同步物理音量键、对侧竖条浮层、防误触边缘死区），右下角按钮点击一键静音/取消静音
- ✅ **播放辅助能力**：画中画（PiP）小窗、横竖屏切换、三档画面比例（适应 / 填充 / 裁切填满）、播放锁屏与防熄屏常亮
- ✅ **播放状态监控**：播放详情统计面板（编码 / 分辨率 / 帧率 / 码率 / 缓冲健康），支持全屏左上角常驻显示缓冲健康秒数开关

#### 本地数据与存储管理
- ✅ **观看历史**：按今天 / 昨天 / 更早分组，支持全部 / 未看完 / 已看完筛选、多选批量删除与撤销提示
- ✅ **收藏与收藏夹**：支持新建 / 重命名 / 删除自定义收藏夹、详情页原地切换收藏夹或取消收藏、收藏夹内按标题与编号搜索
- ✅ **创作者关注**：本地关注创作者并聚合最新作品，支持一键刷新、取消关注与撤销提示
- ✅ **观看统计视图**：累计观看时长、近 7 天柱状图、连续观看天数、12 周活跃热力图、完成度与分类偏好分析
- ✅ **缓存与备份**：ExoPlayer 分片缓存（默认 512 MB LRU 淘汰，清理同步维护索引）、分类存储空间管理与 JSON 数据导出 / 导入合并备份

#### 外观、隐私与工程化
- ✅ **Material You 外观**：动态取色（Android 12+）、浅色 / 深色 / 跟随系统切换、悬浮胶囊底栏开关、单双列切换、平板双栏适配（≥ 840dp）与全局触感反馈
- ✅ **隐私与安全**：生物识别应用锁（支持自动重新上锁超时、关闭前强制鉴权与系统凭据兜底）、防截屏 / 最近任务隐私遮罩、全屏翻转退出
- ✅ **网络诊断**：DNS / TCP / TLS / TTFB 分段耗时探测（兼容代理场景 DNS 读数）、站点状态外部打开、公网 IP 与运行指标统计
- ✅ **版本与发布流水线**：应用内 GitHub Release 检查更新与更新日志弹窗；GitHub Actions 自动校验、签名发布及 Telegram 发布/失败通知

### 🚧 待开发功能

#### 📌 近期开发
- [ ] **缓冲大小调节**：支持在设置中自定义视频播放缓冲大小

#### 🔭 远期开发
- [ ] **多源站点管理**：支持在应用内新增、切换与测试自定义远程站点配置源

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
