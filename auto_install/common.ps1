# ITT auto-install shared tooling helpers
#
# The scripts in this directory are intended to work after the repository is
# cloned to another Windows host. Tool paths are resolved from environment
# variables, local.properties, common installation locations, or PATH.

$ProjectDir = Split-Path -Parent $PSScriptRoot
$Gradlew    = Join-Path $ProjectDir 'gradlew.bat'
$ApkPath   = Join-Path $ProjectDir 'app\build\outputs\apk\debug\app-debug.apk'
$Package   = 'com.bigbrother.mobile'

function Normalize-ToolPath {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $null
    }

    $normalized = [Environment]::ExpandEnvironmentVariables($Value.Trim().Trim('"'))
    # local.properties commonly uses forward slashes and escaped drive colons.
    $normalized = $normalized -replace '\\:', ':'
    $normalized = $normalized.Replace('/', '\')

    if (-not [System.IO.Path]::IsPathRooted($normalized)) {
        $normalized = Join-Path $ProjectDir $normalized
    }

    try {
        $normalized = [System.IO.Path]::GetFullPath($normalized)
    } catch {
        # Keep the expanded value so the later existence check can provide a
        # useful error message.
    }

    if ($normalized.Length -gt 3) {
        $normalized = $normalized.TrimEnd('\')
    }
    return $normalized
}

function Get-LocalPropertiesSdkDir {
    $propertiesPath = Join-Path $ProjectDir 'local.properties'
    if (-not (Test-Path -LiteralPath $propertiesPath -PathType Leaf)) {
        return $null
    }

    $line = Get-Content -LiteralPath $propertiesPath -ErrorAction SilentlyContinue |
        Where-Object { $_ -match '^\s*sdk\.dir\s*=' } |
        Select-Object -First 1
    if ($line -and $line -match '^\s*sdk\.dir\s*=\s*(.*)$') {
        return Normalize-ToolPath $Matches[1]
    }
    return $null
}

function Resolve-SdkDir {
    $candidates = @(
        $env:ANDROID_SDK_ROOT,
        $env:ANDROID_HOME,
        [Environment]::GetEnvironmentVariable('ANDROID_SDK_ROOT', 'User'),
        [Environment]::GetEnvironmentVariable('ANDROID_HOME', 'User'),
        [Environment]::GetEnvironmentVariable('ANDROID_SDK_ROOT', 'Machine'),
        [Environment]::GetEnvironmentVariable('ANDROID_HOME', 'Machine'),
        (Get-LocalPropertiesSdkDir)
    )

    if ($env:LOCALAPPDATA) {
        $candidates += (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
    }
    if ($env:USERPROFILE) {
        $candidates += (Join-Path $env:USERPROFILE 'AppData\Local\Android\Sdk')
    }

    foreach ($candidate in $candidates) {
        $normalized = Normalize-ToolPath $candidate
        if ($normalized -and (Test-Path -LiteralPath $normalized -PathType Container)) {
            return $normalized
        }
    }
    return $null
}

function Resolve-AdbPath {
    param([string]$SdkDir)

    if ($SdkDir) {
        $candidate = Join-Path $SdkDir 'platform-tools\adb.exe'
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return $candidate
        }
    }

    $command = Get-Command adb.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) {
        $path = $command.Source
        if ([string]::IsNullOrWhiteSpace($path)) {
            $path = $command.Path
        }
        if (-not [string]::IsNullOrWhiteSpace($path)) {
            return $path
        }
    }
    return $null
}

function Test-JdkHome {
    param([string]$Candidate)
    if ([string]::IsNullOrWhiteSpace($Candidate)) {
        return $false
    }
    $javaPath = Join-Path $Candidate 'bin\java.exe'
    $releasePath = Join-Path $Candidate 'release'
    if (-not (Test-Path -LiteralPath $javaPath -PathType Leaf) -or
        -not (Test-Path -LiteralPath $releasePath -PathType Leaf)) {
        return $false
    }
    $versionLine = Get-Content -LiteralPath $releasePath -ErrorAction SilentlyContinue |
        Where-Object { $_ -match '^JAVA_VERSION=' } |
        Select-Object -First 1
    return ($versionLine -match '^JAVA_VERSION="21(?:\.|\")')
}

function Resolve-JdkDir {
    $candidates = @(
        $env:JAVA_HOME,
        [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User'),
        [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine')
    )

    foreach ($candidate in $candidates) {
        $normalized = Normalize-ToolPath $candidate
        if ($normalized -and (Test-JdkHome $normalized)) {
            return $normalized
        }
    }

    # If JAVA_HOME is not set, infer the JDK root from java.exe on PATH.
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($javaCommand) {
        $javaPath = $javaCommand.Source
        if ([string]::IsNullOrWhiteSpace($javaPath)) {
            $javaPath = $javaCommand.Path
        }
        if ($javaPath) {
            $javaBin = Split-Path -Parent $javaPath
            $javaHome = Split-Path -Parent $javaBin
            if (Test-JdkHome $javaHome) {
                return (Normalize-ToolPath $javaHome)
            }
        }
    }

    # Common Windows installation locations. This project targets JDK 21.
    $roots = @()
    if ($env:ProgramFiles) {
        $roots += (Join-Path $env:ProgramFiles 'Eclipse Adoptium')
        $roots += (Join-Path $env:ProgramFiles 'Java')
        $roots += (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr')
    }
    if (${env:ProgramFiles(x86)}) {
        $roots += (Join-Path ${env:ProgramFiles(x86)} 'Eclipse Adoptium')
        $roots += (Join-Path ${env:ProgramFiles(x86)} 'Java')
    }
    if ($env:LOCALAPPDATA) {
        $roots += (Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium')
    }

    foreach ($root in $roots) {
        $normalizedRoot = Normalize-ToolPath $root
        if (-not $normalizedRoot -or -not (Test-Path -LiteralPath $normalizedRoot -PathType Container)) {
            continue
        }
        if (Test-JdkHome $normalizedRoot) {
            return $normalizedRoot
        }

        $children = Get-ChildItem -LiteralPath $normalizedRoot -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending
        foreach ($child in $children) {
            if ($child.Name -match '(?i)(jdk|temurin|zulu|microsoft|corretto).*21' -and (Test-JdkHome $child.FullName)) {
                return (Normalize-ToolPath $child.FullName)
            }
        }
    }
    return $null
}

$SdkDir = Resolve-SdkDir
$Adb = Resolve-AdbPath $SdkDir
if (-not $SdkDir -and $Adb) {
    # Infer the SDK root when adb was found through PATH.
    $platformToolsDir = Split-Path -Parent $Adb
    $inferredSdkDir = Split-Path -Parent $platformToolsDir
    if (Test-Path -LiteralPath $inferredSdkDir -PathType Container) {
        $SdkDir = Normalize-ToolPath $inferredSdkDir
    }
}
$JdkDir = Resolve-JdkDir

# Give Gradle and Android tooling the same resolved SDK for this process.
if ($SdkDir) {
    $env:ANDROID_SDK_ROOT = $SdkDir
    $env:ANDROID_HOME = $SdkDir
}

function Initialize-JavaEnvironment {
    if (-not $JdkDir) {
        return $false
    }

    $javaBin = Join-Path $JdkDir 'bin'
    $env:JAVA_HOME = $JdkDir

    $alreadyInPath = $false
    foreach ($entry in ($env:Path -split ';')) {
        if ($entry.TrimEnd('\') -ieq $javaBin.TrimEnd('\')) {
            $alreadyInPath = $true
            break
        }
    }
    if (-not $alreadyInPath) {
        $env:Path = $javaBin + ';' + $env:Path
    }
    return $true
}

function Assert-JavaAvailable {
    if (Initialize-JavaEnvironment) {
        return $true
    }

    Write-Host 'ERROR: JDK 21 was not found.' -ForegroundColor Red
    Write-Host '  Set JAVA_HOME to the JDK directory containing bin\java.exe.' -ForegroundColor Yellow
    Write-Host '  Example: $env:JAVA_HOME = ''C:\Path\To\jdk-21''' -ForegroundColor Yellow
    return $false
}

function Assert-GradleAvailable {
    if (Test-Path -LiteralPath $Gradlew -PathType Leaf) {
        return $true
    }

    Write-Host ("ERROR: gradlew.bat not found -> " + $Gradlew) -ForegroundColor Red
    return $false
}

function Assert-AdbAvailable {
    if ($Adb -and (Test-Path -LiteralPath $Adb -PathType Leaf)) {
        return $true
    }

    Write-Host 'ERROR: adb.exe was not found.' -ForegroundColor Red
    Write-Host '  Install Android SDK Platform-Tools, then set ANDROID_SDK_ROOT or ANDROID_HOME.' -ForegroundColor Yellow
    Write-Host '  Alternatively, add the SDK platform-tools directory to PATH.' -ForegroundColor Yellow
    return $false
}

function Assert-BuildEnvironment {
    $ok = $true
    if (-not (Assert-JavaAvailable)) {
        $ok = $false
    }
    if (-not (Assert-GradleAvailable)) {
        $ok = $false
    }
    return $ok
}
