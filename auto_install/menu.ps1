# =====================================================================
#  Big Brother Mobile - Interactive Deploy Menu
#
#  All-in-one interactive menu: build, install, launch, wireless debug,
#  device inspection, and more.
#
#  Run:
#     powershell -ExecutionPolicy Bypass -File .\menu.ps1
#  or, in PowerShell:
#     .\menu.ps1
# =====================================================================

$ErrorActionPreference = 'Stop'

# ---------- paths ----------
$ProjectDir = Split-Path -Parent $PSScriptRoot
$SdkDir     = 'D:\Administrator\Documents\Big-Brother\.local\android-sdk'
$JdkDir     = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20+8'
$Adb        = Join-Path $SdkDir 'platform-tools\adb.exe'
$Gradlew    = Join-Path $ProjectDir 'gradlew.bat'
$ApkPath    = Join-Path $ProjectDir 'app\build\outputs\apk\debug\app-debug.apk'
$Package    = 'com.bigbrother.mobile'

# ---------- helpers ----------
function Ensure-Jdk {
    if (Test-Path (Join-Path $JdkDir 'bin\java.exe')) {
        $env:JAVA_HOME = $JdkDir
        $env:Path      = (Join-Path $JdkDir 'bin') + ';' + $env:Path
        return $true
    }
    Write-Host ("WARNING: JDK not found at " + $JdkDir) -ForegroundColor Yellow
    return $false
}

function Get-TargetSerial {
    # returns a single device serial; prefers USB over WiFi. $null if none.
    $devices = & $Adb devices
    $serials = foreach ($l in $devices) {
        if ($l -match '^(\S+)\s+device$') { ($l -split '\s+')[0] }
    }
    $usb  = $serials | Where-Object { $_ -notmatch '^\d+\.\d+\.\d+\.\d+' } | Select-Object -First 1
    $wifi = $serials | Where-Object { $_ -match '^\d+\.\d+\.\d+\.\d+' } | Select-Object -First 1
    if ($usb)  { return $usb }
    if ($wifi) { return $wifi }
    return $null
}

function Show-Header {
    Clear-Host
    Write-Host ""
    Write-Host "  ============================================" -ForegroundColor Cyan
    Write-Host "   Big Brother Mobile - Deploy Menu"           -ForegroundColor Cyan
    Write-Host "  ============================================" -ForegroundColor Cyan
    Write-Host ""
}

function Press-AnyKey {
    Write-Host ""
    Write-Host "  Press Enter to return to menu..." -ForegroundColor DarkGray
    Read-Host | Out-Null
}

function Do-Build {
    Write-Host "  Building debug APK ..." -ForegroundColor Cyan
    Write-Host ""
    Ensure-Jdk | Out-Null
    Push-Location $ProjectDir
    try {
        & $Gradlew assembleDebug
        if ($LASTEXITCODE -ne 0) {
            Write-Host ("  Build FAILED (exit " + $LASTEXITCODE + ")") -ForegroundColor Red
        } else {
            Write-Host ""
            Write-Host "  Build OK -> " -NoNewline -ForegroundColor Green
            Write-Host $ApkPath -ForegroundColor White
        }
    } finally {
        Pop-Location
    }
    Press-AnyKey
}

function Do-Deploy {
    Ensure-Jdk | Out-Null
    Write-Host "  [1/3] building APK ..." -ForegroundColor Cyan
    Push-Location $ProjectDir
    & $Gradlew assembleDebug
    $rc = $LASTEXITCODE
    Pop-Location
    if ($rc -ne 0) {
        Write-Host ("  Build FAILED (exit " + $rc + ")") -ForegroundColor Red
        Press-AnyKey
        return
    }

    $target = Get-TargetSerial
    if (-not $target) {
        Write-Host "  ERROR: no device. Run option 4 (wireless) or plug USB." -ForegroundColor Red
        Press-AnyKey
        return
    }

    Write-Host "  [2/3] installing to $target ..." -ForegroundColor Cyan
    & $Adb -s $target install -r $ApkPath
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  Install FAILED" -ForegroundColor Red
        Press-AnyKey
        return
    }

    Write-Host "  [3/3] launching app ..." -ForegroundColor Cyan
    & $Adb -s $target shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null
    Write-Host "  Deploy complete!" -ForegroundColor Green
    Press-AnyKey
}

function Do-InstallOnly {
    $target = Get-TargetSerial
    if (-not $target) {
        Write-Host "  ERROR: no device. Run option 4 (wireless) or plug USB." -ForegroundColor Red
        Press-AnyKey
        return
    }
    if (-not (Test-Path $ApkPath)) {
        Write-Host "  APK not found. Build first (option 1 or 3)." -ForegroundColor Yellow
        Press-AnyKey
        return
    }
    Write-Host "  installing to $target ..." -ForegroundColor Cyan
    & $Adb -s $target install -r $ApkPath
    if ($LASTEXITCODE -eq 0) { Write-Host "  Install OK" -ForegroundColor Green }
    else { Write-Host "  Install FAILED" -ForegroundColor Red }
    Press-AnyKey
}

function Do-Wireless {
    $usb = $null
    $devices = & $Adb devices
    foreach ($l in $devices) {
        if ($l -match '^(\S+)\s+device$') {
            $s = ($l -split '\s+')[0]
            if ($s -notmatch '^\d+\.\d+\.\d+\.\d+') { $usb = $s; break }
        }
    }

    $ip = $null
    if ($usb) {
        Write-Host ("  USB device: " + $usb) -ForegroundColor DarkGray
        $ipRaw = (& $Adb -s $usb shell ip -f inet addr show wlan0 2>$null) -join "`n"
        if ($ipRaw -match 'inet\s+(\d+\.\d+\.\d+\.\d+)') { $ip = $Matches[1] }
        & $Adb -s $usb tcpip 5555 | Out-Null
    }

    if (-not $ip) {
        $ip = Read-Host "  Phone IP not auto-detected. Enter IP (or blank to cancel)"
        if (-not $ip) { Press-AnyKey; return }
    }

    Write-Host ("  Connecting " + $ip + ":5555 ...") -ForegroundColor Cyan
    & $Adb connect ($ip + ":5555")
    Write-Host "  Wireless ADB ready. You may unplug USB." -ForegroundColor Green
    Press-AnyKey
}

function Do-Disconnect {
    Write-Host "  Disconnecting wireless ADB ..." -ForegroundColor Cyan
    & $Adb disconnect
    Write-Host "  Done." -ForegroundColor Green
    Press-AnyKey
}

function Do-Devices {
    Write-Host "  Connected devices:" -ForegroundColor Cyan
    Write-Host ""
    & $Adb devices
    Press-AnyKey
}

function Do-AppInfo {
    $target = Get-TargetSerial
    if (-not $target) {
        Write-Host "  No device connected." -ForegroundColor Yellow
        Press-AnyKey
        return
    }
    Write-Host "  App info on $target :" -ForegroundColor Cyan
    $pkg = (& $Adb -s $target shell pm list packages 2>$null) | Select-String $Package
    if ($pkg) {
        Write-Host "    installed: YES ($Package)" -ForegroundColor Green
        # versionName
        $d = & $Adb -s $target shell dumpsys package $Package 2>$null
        $vn = ($d | Select-String 'versionName=' | Select-Object -First 1).ToString().Trim()
        $vc = ($d | Select-String 'versionCode=' | Select-Object -First 1).ToString().Trim()
        Write-Host ("    " + $vn) -ForegroundColor White
        Write-Host ("    " + $vc) -ForegroundColor White
    } else {
        Write-Host "    installed: NO" -ForegroundColor Red
    }
    Press-AnyKey
}

# ---------- main loop ----------
Ensure-Jdk | Out-Null
while ($true) {
    Show-Header
    Write-Host "   [1] Deploy  (build + install + launch)"    -ForegroundColor White
    Write-Host "   [2] Install only (skip build)"            -ForegroundColor White
    Write-Host "   [3] Build only"                             -ForegroundColor White
    Write-Host "   [4] Wireless debug (connect)"              -ForegroundColor White
    Write-Host "   [5] Wireless debug (disconnect)"           -ForegroundColor White
    Write-Host "   [6] List connected devices"                -ForegroundColor White
    Write-Host "   [7] App info (installed version)"          -ForegroundColor White
    Write-Host "   [0] Exit"                                   -ForegroundColor White
    Write-Host ""
    $raw = Read-Host "  Select [0-7]"
    $choice = if ($null -eq $raw) { "" } else { $raw.Trim() }
    if ([string]::IsNullOrWhiteSpace($choice)) {
        Write-Host "  Bye!" -ForegroundColor Cyan
        exit 0
    }

    switch ($choice) {
        '1' { Do-Deploy }
        '2' { Do-InstallOnly }
        '3' { Do-Build }
        '4' { Do-Wireless }
        '5' { Do-Disconnect }
        '6' { Do-Devices }
        '7' { Do-AppInfo }
        '0' { Write-Host "  Bye!" -ForegroundColor Cyan; exit 0 }
        default { Write-Host "  Invalid choice." -ForegroundColor Yellow; Press-AnyKey }
    }
}