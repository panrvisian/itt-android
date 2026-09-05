# ITT Android 构建说明

项目目录为仓库根目录。完整的自动构建、安装和无线 ADB 使用方法请查看 [AUTO_INSTALL.md](AUTO_INSTALL.md)。

## 环境要求

- JDK 21
- Android SDK Platform 34
- Android SDK Build Tools 34.0.0
- Android SDK Platform-Tools（包含 `adb`）
- 可联网下载 Gradle 依赖

Android Studio 可以直接打开项目并等待 Gradle 同步。命令行构建时，请使用本机实际安装路径配置环境变量；不要照搬其他主机的绝对路径。

## 配置环境变量（PowerShell）

以下路径仅为格式示例，请替换为本机路径：

```powershell
$env:JAVA_HOME = 'C:\Path\To\jdk-21'
$env:ANDROID_SDK_ROOT = 'C:\Path\To\Android\Sdk'
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:Path"
```

如果 `JAVA_HOME`、`ANDROID_SDK_ROOT` 或 `ANDROID_HOME` 已正确配置，可以跳过这一步。

## 命令行打包

在仓库根目录执行：

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon
```

生成路径：

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 版本信息

当前源码版本：`2.11`，versionCode：`16`。

## 常见问题

- **找不到 Java**：确认 `JAVA_HOME` 指向包含 `bin\java.exe` 的 JDK 21 根目录。
- **找不到 Android SDK 或 adb**：确认 `ANDROID_SDK_ROOT` / `ANDROID_HOME` 指向 SDK 根目录，并在 SDK Manager 中安装 Platform-Tools。
- **项目目录中的 `local.properties`**：这是本机配置文件，不提交到 Git；如果存在且有效，脚本会优先读取其中的 `sdk.dir`。
- **PowerShell 禁止运行脚本**：可使用 `auto_install\start-menu.bat`，或在当前用户范围调整执行策略后再运行 PowerShell 脚本。
