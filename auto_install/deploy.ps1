# =====================================================================
#  Big Brother Mobile - one-command deploy script
#  Steps: build debug APK -> install to device -> launch app
#
#  Usage (PowerShell):
#     .\deploy.ps1                         # build + install + launch
#     .\deploy.ps1 -InstallOnly             # build + install, no launch
#     .\deploy.ps1 -SkipBuild               # install existing APK + launch
#     .\deploy.ps1 -SkipBuild -InstallOnly  # install existing APK, no launch
#
#  Requires: USB connection (debugging on), or paired WiFi wireless debug.
#  Tool paths are resolved by auto_install\common.ps1.
# =====================================================================

param(
    [switch]$SkipBuild,
    [switch]$InstallOnly
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

if (-not (Assert-AdbAvailable)) {
    exit 1
}
if (-not $SkipBuild -and -not (Assert-BuildEnvironment)) {
    exit 1
}

# ---------- 1. build ----------
if ($SkipBuild) {
    Write-Host '[skip] build step' -ForegroundColor DarkGray
} else {
    Write-Host '[1/4] building debug APK ...' -ForegroundColor Cyan
    Push-Location $ProjectDir
    try {
        & $Gradlew assembleDebug
        if ($LASTEXITCODE -ne 0) {
            Write-Host ("build failed (exit code " + $LASTEXITCODE + ')') -ForegroundColor Red
            exit $LASTEXITCODE
        }
    } finally {
        Pop-Location
    }
    Write-Host '      build done' -ForegroundColor Green
}

if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) {
    Write-Host ("ERROR: APK not found -> " + $ApkPath) -ForegroundColor Red
    exit 1
}

# ---------- 2. pick target device ----------
$stepPrefix = if ($SkipBuild) { '[1/3]' } else { '[2/4]' }
Write-Host ("$stepPrefix detecting device ...") -ForegroundColor Cyan
$devices = & $Adb devices
$deviceLines = $devices | Where-Object { $_ -match '^(\S+)\s+device$' }
if (-not $deviceLines) {
    Write-Host 'ERROR: no authorized device detected.' -ForegroundColor Red
    Write-Host '  USB : plug in cable and allow USB debugging.' -ForegroundColor Yellow
    Write-Host '  WiFi: adb pair, then adb connect IP:PORT' -ForegroundColor Yellow
    exit 1
}

# Collect device serials; prefer USB (non IP:port) over WiFi.
$serials = foreach ($line in $deviceLines) { ($line -split '\s+')[0] }
$usbSerial = $serials | Where-Object { $_ -notmatch '^\d+\.\d+\.\d+\.\d+' } | Select-Object -First 1
$wifiSerial = $serials | Where-Object { $_ -match '^\d+\.\d+\.\d+\.\d+' } | Select-Object -First 1
$Target = if ($usbSerial) { $usbSerial } else { $wifiSerial }
Write-Host ("      target device: " + $Target) -ForegroundColor DarkGray

# ---------- 3. install ----------
$stepPrefix = if ($SkipBuild) { '[2/3]' } else { '[3/4]' }
Write-Host ("$stepPrefix installing APK ...") -ForegroundColor Cyan
& $Adb -s $Target install -r $ApkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host ("install failed (exit code " + $LASTEXITCODE + ')') -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host '      install OK' -ForegroundColor Green

# ---------- 4. launch ----------
if ($InstallOnly) {
    Write-Host '[skip] launch step' -ForegroundColor DarkGray
} else {
    $stepPrefix = if ($SkipBuild) { '[3/3]' } else { '[4/4]' }
    Write-Host ("$stepPrefix launching app ...") -ForegroundColor Cyan
    & $Adb -s $Target shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null
    Write-Host '      app launched' -ForegroundColor Green
}

Write-Host ''
Write-Host 'deploy complete!' -ForegroundColor Green
