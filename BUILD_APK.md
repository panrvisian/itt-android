# Big Brother Mobile 构建说明

项目目录：`android-mobile`

## Android Studio
1. 用 Android Studio 打开 `android-mobile`。
2. 等待 Gradle 同步完成。
3. 连接 Android 12+ 手机或启动模拟器。
4. 点击 Run。

## 命令行打包
需要本机已安装 JDK 17 和 Android SDK。

```powershell
cd D:\Administrator\Documents\Big-Brother\android-mobile
.\gradlew.bat assembleDebug
```

生成路径：

```text
android-mobile\app\build\outputs\apk\debug\app-debug.apk
```
