# auto_install 使用与主机适配

`auto_install` 是项目附带的 Windows PowerShell 自动部署工具，支持构建 Debug APK、安装到 Android 设备、启动应用、查看设备和无线 ADB 连接。

## 适用范围

- Windows PowerShell 5.1 或 PowerShell 7
- 本目录中的脚本使用相对项目目录，不要求仓库放在固定位置
- `auto_install` 目前面向 Windows；macOS/Linux 可以直接使用 Gradle Wrapper 构建，但不能使用 `.bat` 菜单

## 环境要求

1. JDK 21，并确保 JDK 根目录内存在 `bin\java.exe`
2. Android SDK Platform 37.0
3. Android SDK Build Tools 37.0.0
4. Android SDK Platform-Tools（包含 `adb.exe`）
5. 手机开启开发者选项和 USB 调试，或已完成无线调试配对
6. 首次构建时可以访问网络，以下载 Gradle 依赖

SDK Manager 安装项：

```powershell
$env:ANDROID_SDK_ROOT = 'C:\Path\To\Android\Sdk'
& "$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin\sdkmanager.bat" `
  --sdk_root="$env:ANDROID_SDK_ROOT" `
  'platforms;android-37.0' 'build-tools;37.0.0' 'platform-tools'
```

如果使用 Android Studio，则在 SDK Manager 中安装同名的 Platform 37.0、Build Tools 37.0.0 和 Platform-Tools。

## 在另一台主机上的配置

推荐先在当前 PowerShell 会话设置环境变量。下面的路径只是格式示例，请替换为本机实际路径：

```powershell
$env:JAVA_HOME = 'C:\Path\To\jdk-21'
$env:ANDROID_SDK_ROOT = 'C:\Path\To\Android\Sdk'
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:Path"
```

验证：

```powershell
java -version
adb version
```

`java -version` 应使用 JDK 21。`adb version` 能正常输出版本信息即可。

### 脚本的路径查找顺序

脚本通过 `auto_install\common.ps1` 统一查找工具，不再依赖项目作者主机的绝对路径。

Android SDK 查找顺序：

1. 当前 PowerShell 会话的 `ANDROID_SDK_ROOT`
2. 当前会话的 `ANDROID_HOME`
3. 用户或系统环境变量中的上述变量
4. 项目本地的 `local.properties` 中有效的 `sdk.dir`
5. Windows 常见的 Android SDK 目录
6. `PATH` 中的 `adb.exe`

JDK 查找顺序：

1. 当前会话的 `JAVA_HOME`
2. 用户或系统环境变量中的 `JAVA_HOME`
3. `PATH` 中的 `java.exe`
4. Windows 常见的 JDK 21 安装目录

如果 SDK 或 JDK 安装在非标准目录，设置环境变量即可，不需要修改脚本。项目中的 `local.properties` 仅属于本机配置文件，不应提交到 Git。

## 启动方式

在仓库根目录执行：

```powershell
Set-Location .\auto_install
.\menu.ps1
```

也可以双击：

```text
auto_install\start-menu.bat
```

## 菜单功能

- `1`：构建、安装并启动
- `2`：安装已有 APK，不重新构建
- `3`：只构建 APK
- `4`：通过 USB 切换并连接无线 ADB
- `5`：断开无线 ADB
- `6`：列出已连接设备
- `7`：查看已安装应用的版本
- `0`：退出

## 命令行方式

在 `auto_install` 目录执行：

```powershell
.\deploy.ps1
```

构建、安装并启动应用。

```powershell
.\deploy.ps1 -InstallOnly
```

构建并安装，但不启动应用。

```powershell
.\deploy.ps1 -SkipBuild
```

跳过构建，安装已有 APK 并启动应用。

```powershell
.\deploy.ps1 -SkipBuild -InstallOnly
```

跳过构建，只安装已有 APK，不启动应用。

APK 默认位置：

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 设备连接

### USB

1. 手机开启 USB 调试。
2. 用 USB 连接电脑并在手机上允许调试授权。
3. 执行菜单中的构建部署，或执行 `deploy.ps1`。

### 无线 ADB

使用 USB 连接手机时：

```powershell
.\wifi-adb.ps1
```

如果脚本无法自动识别手机 IP：

```powershell
.\wifi-adb.ps1 -Ip 192.168.1.100
```

手机重启后，TCP 调试模式通常会失效，需要重新连接 USB 并执行一次脚本。电脑和手机应连接到同一局域网。

## 常见问题

### `adb.exe was not found`

确认已安装 Android SDK Platform-Tools，并设置：

```powershell
$env:ANDROID_SDK_ROOT = 'C:\Path\To\Android\Sdk'
$env:Path = "$env:ANDROID_SDK_ROOT\platform-tools;$env:Path"
```

### `JDK 21 was not found`

确认 `JAVA_HOME` 指向 JDK 根目录，而不是 `bin` 目录：

```powershell
$env:JAVA_HOME = 'C:\Path\To\jdk-21'
```

### 没有检测到设备

执行：

```powershell
adb devices
```

如果显示 `unauthorized`，解锁手机并接受授权；如果没有设备，检查 USB 线、USB 调试开关或无线连接状态。

### PowerShell 禁止执行脚本

可以双击 `start-menu.bat`，或者在当前用户范围执行：

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

## 协作注意事项

- 不要把本机的 `local.properties` 提交到仓库
- 不要把 JDK、Android SDK 的本机绝对路径写入项目脚本
- 不要提交 APK、密钥或设备授权信息
- 代码修改建议在独立分支完成，再通过 Pull Request 合并到 `main`
