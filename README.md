# ITT 安卓应用

[English](README.en.md)

> ITT（Individual Time Trial，个人计时）安卓应用：用于记录、追踪和分析个人活动时长。

## 项目简介

ITT 是一款面向个人的计时记录应用，支持按分组/事件组织活动，记录每一次开始与结束，提供首页、时间轴、备注、统计和设置五个主页面。应用数据全部保存在本地，支持通过 ZIP（CSV + 图片）或纯 CSV 导入导出，便于备份与迁移。

## 功能特性

### 记录

- 点击事件开始计时，再次点击结束
- 进行中卡片长按 0.5 秒结束计时，带震动反馈
- 手动补录（开始时间 / 结束时间）
- 编辑记录弹窗支持「上一条记录结束 + 1 分钟」快捷时间
- 跨天记录自动拆分

### 分组与事件

- 按分组组织事件，每个分组有独立颜色
- 「未分组」固定在底部，不可排序
- 事件支持收藏（星标仅在时间轴色块显示）

### 首页

- 当前时间显示（可开关日期）
- 进行中记录区
- 收藏与分组区

### 时间轴

- 按天查看记录色块
- 双指缩放，锚点为屏幕中心对应的时间
- 前一天 / 今天 / 后一天 / 跳转到指定日期
- 记录备注按色块高度动态显示行数，超出部分省略

### 备注

- 每条记录可添加文字备注和最多 10 张图片
- 图片来源：相册多选 / 拍照
- 全屏编辑模式
- 未保存退出自动保存草稿
- 草稿清理规则：进行中记录的草稿永久保留；已结束记录超过 1 天删除；记录已删除的草稿删除

### 统计

- 汇总时长统计（合计 / 去重可选）
- 支持配置学期、每周起始日等

### 设置

- 主题：跟随系统 / 浅色 / 深色
- 字号：小 / 中 / 大 / 特大 / 跟随系统
- 壁纸背景：默认 / 图片 / 纯色
- 震动反馈开关
- 导入导出：ZIP（CSV + 图片文件夹）或纯 CSV

## 技术栈

- 语言：Kotlin
- UI：Jetpack Compose（Material 3）
- 数据库：Room 2.6.1（版本 2，含迁移）
- 偏好存储：DataStore Preferences
- 架构：ViewModel + Repository
- 构建：Gradle 8.7 / Android Gradle Plugin 8.5.2 / Kotlin 1.9.24
- 最低系统：Android 12（API 31）
- 目标 / 编译 SDK：34

## 应用信息

- 包名：`com.bigbrother.mobile`
- 当前版本：2.8（versionCode 13）

## 环境要求

- JDK 17
- Android SDK Platform 34、Build Tools 34.0.0
- 可联网下载 Gradle 依赖

## 构建方法

Windows PowerShell 命令行打包：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.20+8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug --no-daemon
```

APK 输出路径：

```text
app\build\outputs\apk\debug\app-debug.apk
```

交付命名示例：`ITT-v2.8-build-yyyyMMdd-HHmm-debug.apk`

## 项目结构

```text
android-mobile/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/bigbrother/mobile/
│       │   ├── data/        # Room 数据库、Repository、DataStore、CSV 编解码
│       │   ├── domain/      # 统计计算、时间工具
│       │   └── ui/          # Compose 界面、ViewModel、主题
│       └── res/             # 资源
├── gradle/wrapper/          # Gradle Wrapper
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## 数据与存储

- 数据保存在应用私有目录的 Room 数据库（records / groups / events / note_images 表）
- 备注图片保存在 `filesDir/notes/<recordId>/`，草稿图片保存在 `filesDir/notes_draft/<recordId>/`
- 数据库只保存图片文件名

## 导入导出

- ZIP：包含 CSV 数据文件和图片文件夹，可完整备份 / 恢复备注图片
- 纯 CSV：兼容旧版本导出的文件，只包含文本数据
- CSV 解析兼容引号内换行、逗号、转义引号

## 版本历史

### v2.8（2026-08-15）

- 新增备注功能：记录文字备注与图片（相册 / 拍照），全屏编辑，草稿自动保存与清理
- 时间轴色块显示备注，行数随色块高度动态计算
- 导入导出升级为 ZIP（CSV + 图片）

更早版本的变更记录见 Git 标签与提交历史。

## 说明

- 本项目为个人使用应用，数据默认仅保存在本机
- 备份请使用应用内「导出」功能，并妥善保存导出的 ZIP 文件
