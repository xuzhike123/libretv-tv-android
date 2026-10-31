# LibreTV TV

参照 SmartTube（SmartTubeNext）界面语言设计的 LibreTV 电视端应用，采用 Kotlin + Compose for TV 实现，可用遥控器（方向键 / OK / 返回）完整操作。

## 当前状态

- 界面与导航：左侧导航栏 + 内容行 + 详情页 + 播放页，全部基于 Compose for TV 的 `TvLazyRow` / `TvLazyColumn`。
- 数据：内置占位演示数据（片名、来源、延迟、EPG 均为虚构），**未接入真实检索接口**。
- 播放：`PlayerConfig.DEMO_STREAM_URI` 为空时只显示占位提示，填入 HLS(m3u8) 地址即可起播。
- 设置：偏好项仅保存在内存中，重启后恢复默认。

## 页面结构

| 入口 | 内容 |
| --- | --- |
| 首页 | 继续观看、豆瓣电影 / 剧集推荐、新番放送、热播榜单、收藏 |
| 搜索 | 遥控器屏上键盘 + 搜索历史 + 热门词 |
| 收藏 / 观看历史 | 内容墙列表 |
| 直播 IPTV | 分组 + 频道宫格，显示当前 / 下一个节目（EPG） |
| 片源管理 | 源健康度、延迟、阶梯停用状态 |
| 设置 | 主题配色、界面缩放、卡片标题样式、倍速、缓冲、AFR 等 |
| 详情 / 播放 | 选集、换源测速；播放进度条与单排控制条 |

## 遥控器按键

| 按键 | 作用 |
| --- | --- |
| 方向键 | 移动焦点 |
| OK / 确定 | 进入 / 播放暂停 |
| 返回 / Esc | 返回上一页 |
| 播放页 ← → | 快退 / 快进 15 秒 |

## 依赖版本

均取自公开仓库元数据（2026-10-30 核对）：

- AGP `9.5.0`、Kotlin `2.4.21`、Gradle `9.8.2`
- Compose BOM `2026.09.00`
- `androidx.tv:tv-foundation:1.0.0`、`androidx.tv:tv-material:1.1.0`
- Media3 `1.11.1`（exoplayer / hls / ui）
- compileSdk `36`、minSdk `26`

## 构建

```bash
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

## 事实依据

- LibreTV 的聚合搜索、跨源同名聚合、本机 IndexedDB 秒级续播、换源测速、源健康度阶梯停用等能力，来自项目公开仓库文档（github.com/LibreSpark/LibreTV 及其 Next.js 迁移分支的 README / Wiki）。
- 直播 IPTV（M3U 订阅、XMLTV 节目单）仅出现在社区分支的说明中，上游仓库文档未列出，因此作为可卸载模块呈现。
- SmartTube 的左侧导航栏、卡片网格、单排控制条、播放设置面板、三套配色与 0.8x–1.4x 界面缩放，来自 smarttube.app 与 SmartTube Next 的公开介绍。
