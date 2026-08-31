# =====================================================================
#  Big Brother Mobile - one-command deploy script
#  Steps: build debug APK -> install to device -> launch app
#
#  Usage (PowerShell):
#     .\deploy.ps1              # build + install + launch
#     .\deploy.ps1 -SkipBuild   # skip build, install existing APK
#     .\deploy.ps1 -InstallOnly # install only, do not auto-launch
#
#  Requires: USB connection (debugging on), or paired WiFi wireless debug.
# =====================================================================

param(
    [switch]$SkipBuild,     # skip gradlew assembleDebug
    [switch]$InstallOnly    # do not auto-launch after install
)

$ErrorActionPreference = 'Stop'

# ---------- paths ----------
$ProjectDir = Split-Path -Parent $PSScriptRoot
$SdkDir     = 'D:\Administrator\Documents\Big-Brother\.local\android-sdk'
$MachineJavaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine')
$JdkDir = if (
    -not [string]::IsNullOrWhiteSpace($MachineJavaHome) -and
    (Test-Path (Join-Path $MachineJavaHome 'bin\java.exe'))
) {
    $MachineJavaHome.TrimEnd('\')
} else {
    'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
}
$Adb        = Join-Path $SdkDir 'platform-tools\adb.exe'
$Gradlew    = Join-Path $ProjectDir 'gradlew.bat'
$ApkPath    = Join-Path $ProjectDir 'app\build\outputs\apk\debug\app-debug.apk'
$Package    = 'com.bigbrother.mobile'

# Ensure JDK is discoverable by gradlew (explicit, environment-independent).
if (Test-Path (Join-Path $JdkDir 'bin\java.exe')) {
    $env:JAVA_HOME = $JdkDir
    $env:Path      = (Join-Path $JdkDir 'bin') + ';' + $env:Path
} else {
    Write-Host ("WARNING: JDK not found at " + $JdkDir) -ForegroundColor Yellow
}

# ---------- sanity checks ----------
if (-not (Test-Path $Adb)) {
    Write-Host ("ERROR: adb not found -> " + $Adb) -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $Gradlew)) {
    Write-Host ("ERROR: gradlew.bat not found -> " + $Gradlew) -ForegroundColor Red
    exit 1
}

# ---------- 1. build ----------
if ($SkipBuild) {
    Write-Host "[skip] build step" -ForegroundColor DarkGray
} else {
    Write-Host "[1/4] building debug APK ..." -ForegroundColor Cyan
    Push-Location $ProjectDir
    try {
        & $Gradlew assembleDebug
        if ($LASTEXITCODE -ne 0) {
            Write-Host ("build failed (exit code " + $LASTEXITCODE + ")") -ForegroundColor Red
            exit $LASTEXITCODE
        }
    } finally {
        Pop-Location
    }
    Write-Host "      build done" -ForegroundColor Green
}

if (-not (Test-Path $ApkPath)) {
    Write-Host ("ERROR: APK not found -> " + $ApkPath) -ForegroundColor Red
    exit 1
}

# ---------- 2. pick target device ----------
Write-Host "[2/4] detecting device ..." -ForegroundColor Cyan
$devices = & $Adb devices
$deviceLines = $devices | Where-Object { $_ -match '^(\S+)\s+device$' }
if (-not $deviceLines) {
    Write-Host "ERROR: no authorized device detected." -ForegroundColor Red
    Write-Host "  USB : plug in cable and allow USB debugging." -ForegroundColor Yellow
    Write-Host "  WiFi: adb pair, then adb connect IP:PORT" -ForegroundColor Yellow
    exit 1
}

# Collect device serials; prefer USB (non IP:port) over WiFi.
$serials = foreach ($l in $deviceLines) { ($l -split '\s+')[0] }
$usbSerial   = $serials | Where-Object { $_ -notmatch '^\d+\.\d+\.\d+\.\d+' } | Select-Object -First 1
$wifiSerial  = $serials | Where-Object { $_ -match '^\d+\.\d+\.\d+\.\d+' } | Select-Object -First 1
$Target = if ($usbSerial) { $usbSerial } else { $wifiSerial }
Write-Host ("      target device: " + $Target) -ForegroundColor DarkGray

# ---------- 3. install ----------
Write-Host "[3/4] installing APK ..." -ForegroundColor Cyan
& $Adb -s $Target install -r $ApkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host ("install failed (exit code " + $LASTEXITCODE + ")") -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "      install OK" -ForegroundColor Green

# ---------- 4. launch ----------
if ($InstallOnly) {
    Write-Host "[skip] launch step" -ForegroundColor DarkGray
} else {
    Write-Host "[4/4] launching app ..." -ForegroundColor Cyan
    & $Adb -s $Target shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null
    Write-Host "      app launched" -ForegroundColor Green
}

Write-Host ""
Write-Host "deploy complete!" -ForegroundColor Green