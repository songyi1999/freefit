# FreeFit

[![Build APK](https://github.com/songyi1999/freefit/actions/workflows/build.yml/badge.svg)](https://github.com/songyi1999/freefit/actions/workflows/build.yml)

免费、无账号、零服务器的本地健身 App（安卓原生 Kotlin + Compose）。产品与开发方案见 [PLAN.md](PLAN.md)。

## 下载安装

- 稳定版：到 [Releases](https://github.com/songyi1999/freefit/releases) 下载 `FreeFit-release.apk`（打 `v*` 标签自动发布）
- 尝鲜版：Actions 构建产物（每次 push 自动打包，需登录 GitHub 下载 artifact）

## 构建

- 用 **Android Studio** 打开项目根目录，等 Gradle Sync 完成后直接 Run。
- 或命令行（需 JDK 17 + Android SDK）：

```bash
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

## 当前状态（M1）

- 内置 10 个动作（关键帧姿态数据驱动骨架小人动画）
- 3 个训练计划模板，跟练播放器（语音倒计时 / 组间休息 / 跳过 / 暂停）
- 训练记录本地存储（Room），首页打卡天数与最近记录

## 目录结构

```
app/src/main/java/com/freefit/app/
├── MainActivity.kt          # 入口 + 导航
├── ui/Theme.kt              # Material3 主题
├── data/
│   ├── Models.kt            # 姿态/动作/计划数据模型
│   ├── ExerciseLibrary.kt   # 内置动作库（关键帧数据）
│   ├── Plans.kt             # 训练计划模板
│   └── db/Db.kt             # Room 训练记录
├── fitness/
│   ├── Poser.kt             # 关键帧插值 + 骨架小人渲染
│   ├── Tts.kt               # 语音倒计时
│   ├── PlayerController.kt  # 跟练状态机
│   └── PlayerScreen.kt      # 跟练播放器界面
└── screens/                 # 首页 / 动作库 / 记录
```
