# FreeFit · 自由炼

[![Build APK](https://github.com/songyi1999/freefit/actions/workflows/build.yml/badge.svg)](https://github.com/songyi1999/freefit/actions/workflows/build.yml)

**自由炼（FreeFit）** —— 免费、无账号、零服务器的本地健身 App（安卓原生 Kotlin + Compose）。

> 名字含义：自由 = Free（永久免费、数据自由），炼 = Fit（自律锻造）。呼应口号「自律给我自由」。
> 中文名「自由炼」为中文系统显示名，英文系统显示 FreeFit，跟随系统语言自动切换。

产品与开发方案见 [PLAN.md](PLAN.md)。

## 功能

- **跟练训练**：16 个动作（含哑铃系列），骨架小人关键帧动画领练，语音倒计时 + 提示音 + 组间休息
- **AI 实时纠错**：MediaPipe 端侧姿态识别，全部动作自动计数 + 深度/体线纠错，支持全屏模式与跟练右下角小窗
- **智能定制计划**：问卷式向导（性别/年龄/器械/伤病/目标）生成个性化计划，动作可替换/移除/添加
- **户外跑步**：GPS 轨迹（高德底图）+ 距离配速 + 战绩卡片分享
- **应用内自更新（OTA）**：自动检查 GitHub Release，下载安装一条龙
- **中英双语**：界面/数据/语音/通知/分享卡片全量跟随系统语言

一切数据仅存本机（Room/SharedPreferences），无任何服务器，永久免费无广告。

## 下载安装

- 稳定版：到 [Releases](https://github.com/songyi1999/freefit/releases) 下载 `FreeFit-release.apk`（打 `v*` 标签自动发布）
- 已装用户可在 App 内直接检查更新（首页底部）

## 构建

- 用 **Android Studio** 打开项目根目录，等 Gradle Sync 完成后直接 Run。
- 或命令行（需 JDK 17 + Android SDK）：

```bash
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

## 目录结构

```
app/src/main/java/com/freefit/app/
├── MainActivity.kt          # 入口 + 导航
├── L10n.kt / FreeFitApp.kt  # 双语与全局初始化
├── ui/Theme.kt              # Material3 主题
├── data/
│   ├── Models.kt            # 姿态/动作/计划数据模型
│   ├── ExerciseLibrary.kt   # 内置动作库（关键帧数据）
│   ├── Plans.kt             # 内置训练计划
│   ├── PlanStore.kt         # 用户资料 + 定制计划 + 生成器
│   └── db/Db.kt             # Room 训练记录
├── fitness/
│   ├── Poser.kt             # 关键帧插值 + 骨架小人渲染
│   ├── Tts.kt / SoundCues.kt# 语音与提示音
│   ├── PlayerController.kt  # 跟练状态机
│   ├── PlayerScreen.kt      # 跟练播放器（含 AI 小窗）
│   └── camera/              # MediaPipe 姿态识别（全屏/小窗/规则引擎）
├── run/                     # 跑步（GPS/地图/轨迹/分享卡片）
├── update/                  # OTA 自更新
└── screens/                 # 首页 / 动作库 / 计划 / 记录 / 向导
```
