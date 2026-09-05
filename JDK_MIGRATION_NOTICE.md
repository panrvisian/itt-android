# JDK 21 环境说明

项目和 Gradle 构建需要 JDK 21。JDK 安装目录因主机而异，不要在脚本或文档中照搬其他电脑的绝对路径。

## 配置

在 PowerShell 中将 `JAVA_HOME` 设置为 JDK 根目录，目录内应存在 `bin\java.exe`：

```powershell
$env:JAVA_HOME = 'C:\Path\To\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

在 CMD 中：

```bat
set JAVA_HOME=C:\Path\To\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
```

## 验证

```powershell
java -version
```

应使用 Java 21。已经打开的终端不会自动继承后来修改的系统环境变量，修改后请重新打开终端，或在当前会话重新设置变量。

## 自动脚本

`auto_install\common.ps1` 会依次尝试读取当前会话、用户和系统级的 `JAVA_HOME`，校验它确实是 JDK 21，然后检查 PATH 和 Windows 常见的 JDK 21 安装目录。其他主机通常只需要正确设置 `JAVA_HOME`，无需修改自动脚本。
