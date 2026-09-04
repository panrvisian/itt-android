# ITT 安卓应用

[English](README.en.md)

> ITT（Individual Time Trial，个人计时）是一款本地优先的安卓时间记录应用，用于记录、追踪和分析个人活动时长。

## 项目简介

ITT 按分组和事件组织活动，支持普通计时、手动补录、记录编辑、事件克隆、时间轴查看、统计分析、备注和桌面小组件。数据默认保存在设备本地，可通过 ZIP（CSV + 图片）或纯 CSV 备份和迁移。

## 功能特性

### 记录

- 点击事件开始计时，再次点击结束
- 进行中记录支持长按结束，并提供震动反馈
- 手动补录开始时间和结束时间
- 编辑记录时可使用快捷时间，也可以克隆记录
- 克隆时先选择分组，再选择分组内的事件；进行中的记录也可以克隆
- 跨天记录自动拆分

### 分组与事件

- 按分组组织事件，每个分组有独立颜色
- 「未分组」固定在底部
- 事件支持收藏
- 首页按事件记录频率排序；普通记录、补录记录和克隆记录都会计入，跨天记录只计算一次

### 首页

- 当前时间显示，可切换日期显示
- 进行中记录区
- 收藏和分组区

### 时间轴

- 按天查看记录色块
- 支持「比例」和「紧凑」两种视图
- 比例视图按实际时间位置和时长显示；紧凑视图按记录顺序排列，不对应开始时间
- 记录重叠时按重叠记录的宽度规则分列显示
- 色块文字优先在同一行显示名称和起止时间，备注固定显示在下方；宽度不足时自动换行或使用省略号
- 双指缩放以两指之间对应的时间为锚点，并支持双指上下移动
- 单指滚动使用正常的拖动和惯性效果；不识别旋转手势
- 支持前一天、今天、后一天以及跳转到指定日期
- 记录详情支持编辑、备注、克隆和删除

### 备注

- 每条记录可添加文字备注和最多 10 张图片
- 图片来源：相册多选或拍照
- 支持全屏编辑
- 未保存退出时自动保存草稿
- 草稿会按记录状态和时间自动清理

### 统计

- 支持天、周、月统计
- 可以查看当前统计范围之前或之后的其他天、周、月
- 天、周、月共用一个基准日期
- 支持合计和去重统计
- 分组时间占比饼图显示各分组的累计时长
- 点击分组后可查看该分组内各事件的时间占比，事件排行同步筛选；点击返回可恢复完整统计

### 桌面小组件

- 支持 1×1 快速计时小组件
- 支持 4×2 小组件，包含 7 个事件格和 1 个编辑格
- 点击事件格开始或结束计时
- 进行中的事件显示实心圆点
- 未分配的格子显示灰色加号，点击后先选择分组，再选择事件
- 小组件中的事件名称单行显示，宽度不足时使用省略号
- 每个小组件实例可以独立配置事件，允许重复配置同一事件

### 新手引导与系统界面

- 首次打开时显示遮罩式新手引导，可以跳过
- 可在设置中重新查看新手引导
- 支持边到边显示，适配底部手势导航横条
- 系统栏会根据当前主题自动调整图标颜色

## 技术栈

- 语言：Kotlin
- UI：Jetpack Compose（Material 3）
- 数据库：Room 2.6.1（数据库版本 2，含迁移）
- 偏好存储：DataStore Preferences
- 架构：ViewModel + Repository
- 构建：Gradle 8.7 / Android Gradle Plugin 8.5.2 / Kotlin 1.9.24
- 最低系统：Android 12（API 31）
- 目标 / 编译 SDK：34

## 应用信息

- 包名：`com.bigbrother.mobile`
- 当前版本：`2.11`
- versionCode：`16`

## 环境要求

- Windows、Android Studio 或 PowerShell
- JDK 17
- Android SDK Platform 34
- Android SDK Build Tools 34.0.0
- Android SDK Platform-Tools（包含 `adb`）
- 可联网下载 Gradle 依赖

## 构建和自动安装

手动构建请查看 [BUILD_APK.md](BUILD_APK.md)。

如果需要自动构建、安装、启动或无线 ADB，请查看 [AUTO_INSTALL.md](AUTO_INSTALL.md)。自动脚本会读取当前主机的 `JAVA_HOME`、`ANDROID_SDK_ROOT` 或 `ANDROID_HOME`，不依赖项目作者电脑的固定路径。

最小构建命令示例（请替换为本机实际路径）：

```powershell
$env:JAVA_HOME = 'C:\Path\To\jdk-17'
$env:ANDROID_SDK_ROOT = 'C:\Path\To\Android\Sdk'
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:Path"
.\gradlew.bat :app:assembleDebug --no-daemon
```

APK 输出路径：

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 项目结构

```text
android-mobile/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/bigbrother/mobile/
│       │   ├── data/        # Room、Repository、DataStore、CSV 编解码
│       │   ├── domain/      # 统计计算和时间工具
│       │   ├── ui/          # Compose 界面、ViewModel、主题
│       │   └── widget/      # 1×1 和 4×2 桌面小组件
│       └── res/             # 布局、图标和其他资源
├── auto_install/            # Windows 自动构建、安装和无线 ADB 脚本
├── gradle/wrapper/           # Gradle Wrapper
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## 数据与存储

- 数据保存在应用私有目录的 Room 数据库中
- 备注图片保存在 `filesDir/notes/<recordId>/`
- 草稿图片保存在 `filesDir/notes_draft/<recordId>/`
- 数据库只保存图片文件名

## 导入导出

- ZIP：包含 CSV 数据文件和图片文件夹，可完整备份和恢复备注图片
- 纯 CSV：兼容旧版本导出的文件，只包含文本数据
- CSV 解析兼容引号内换行、逗号和转义引号

## 版本说明

当前源码版本为 `2.11`（versionCode `16`）。后续功能更新保持版本号不变时，仍会记录在 `main` 分支提交历史中；正式发布版本以 Git 标签和 GitHub Release 为准。

## 说明

- 项目数据默认只保存在本机，不会自动上传到服务器
- 备份请使用应用内「导出」功能，并妥善保存导出的 ZIP 文件
