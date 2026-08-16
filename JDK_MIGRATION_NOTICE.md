# JDK 迁移通知（重要，请先读）

> 生成时间：2026-08-15。给所有在本机运行、需要用到 Java/Gradle 的 agent 和脚本。

## 发生了什么

系统 JDK 已迁移到标准路径，**旧目录已删除**：

- ❌ 旧路径（已删除，**不要再引用**）：
  `D:\Administrator\Documents\Big-Brother\.local\jdk17-extract\jdk-17.0.20+8`
- ✅ 新路径（Temurin 17.0.20+8，与原 JDK 内容完全一致，492 个文件校验一致）：
  `C:\Program Files\Eclipse Adoptium\jdk-17.0.20+8`
- ✅ 机器级环境变量已更新：
  - `JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.20+8`
  - `PATH` 已前置加入 `%JAVA_HOME%\bin`
- ✅ 由旧路径启动的 Gradle daemon 和 Kotlin daemon 已停止
- 📦 备份：`D:\Administrator\Documents\Big-Brother\.local\jdk17.zip`（如需重装可解压，解压后仍建议放到标准路径）
- ✏️ `deploy.ps1`、`menu.ps1` 中的 `$JdkDir` 已更新为新路径，无需再改

## 你需要做的调整

1. **刷新环境变量**：你当前会话里的 `JAVA_HOME` / `PATH` 仍是迁移前的旧值（指向已删除的路径）。新启动的进程会自动继承机器级新值；**已经在运行的会话**请手动刷新：
   - PowerShell：`$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20+8'`（或 `refreshenv`）
   - CMD：`set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20+8`
2. **禁止引用旧路径**：任何指向 `...\.local\jdk17-extract\...` 的脚本、配置、命令都会失败。
3. **Gradle 构建**：直接运行 `gradlew.bat` 即可——新会话中 `JAVA_HOME` 已指向新 JDK，会自动启动新 daemon，无需额外设置。若你的脚本显式设置了 `JAVA_HOME` 或 `org.gradle.java.home`，请改用新路径。
4. **验证**：
   - `java -version` 应输出 `openjdk version "17.0.20"`（Temurin 17.0.20+8）
   - `echo %JAVA_HOME%`（或 `$env:JAVA_HOME`）应指向 `C:\Program Files\Eclipse Adoptium\jdk-17.0.20+8`
