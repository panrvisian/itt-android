# =====================================================================
#  Big Brother Mobile - establish WiFi wireless ADB connection
#
#  Usage:
#     .\wifi-adb.ps1                  # USB connected -> switch to wireless
#     .\wifi-adb.ps1 -Ip 192.168.3.55 # already switched -> just reconnect
#
#  NOTE: tcpip mode resets on phone reboot. After a reboot, plug in USB
#        and run .\wifi-adb.ps1 again to re-establish.
# =====================================================================

param(
    [string]$Ip,
    [int]$Port = 5555
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

if (-not (Assert-AdbAvailable)) {
    exit 1
}

# ---------- 1. pick a connected USB device, if any ----------
Write-Host '[1] checking for USB device ...' -ForegroundColor Cyan
$usbSerial = $null
$devices = & $Adb devices
foreach ($line in $devices) {
    if ($line -match '^(\S+)\s+device$') {
        $serial = ($line -split '\s+')[0]
        if ($serial -notmatch '^\d+\.\d+\.\d+\.\d+') {
            $usbSerial = $serial
            break
        }
    }
}

if ($usbSerial) {
    Write-Host ("      USB device: " + $usbSerial) -ForegroundColor DarkGray

    # Read the phone's LAN IP before switching to tcpip; the USB serial then changes.
    if (-not $Ip) {
        $ipRaw = (& $Adb -s $usbSerial shell ip -f inet addr show wlan0 2>$null) -join "`n"
        if ($ipRaw -match 'inet\s+(\d+\.\d+\.\d+\.\d+)') {
            $Ip = $Matches[1]
        }
    }

    & $Adb -s $usbSerial tcpip $Port | Out-Null
    Write-Host ("      switched to TCP mode on port " + $Port) -ForegroundColor Green
} else {
    Write-Host '      no USB device (assuming wireless already configured)' -ForegroundColor DarkGray
}

# ---------- 2. discover LAN IP if still missing ----------
if (-not $Ip) {
    Write-Host 'ERROR: cannot auto-detect phone IP.' -ForegroundColor Red
    Write-Host ("  Re-run with -Ip <phone-ip>, e.g. .\wifi-adb.ps1 -Ip 192.168.3.55") -ForegroundColor Yellow
    Write-Host '  Find it in the phone Settings, usually under About phone or Wi-Fi details.' -ForegroundColor Yellow
    exit 1
}
Write-Host ("[2] phone IP: " + $Ip) -ForegroundColor Cyan

# ---------- 3. connect wirelessly ----------
Write-Host ("[3] connecting " + $Ip + ':' + $Port + ' ...') -ForegroundColor Cyan
& $Adb connect ($Ip + ':' + $Port)

Write-Host ''
Write-Host 'Wireless ADB ready. You can now unplug the USB cable.' -ForegroundColor Green
Write-Host 'Current devices:' -ForegroundColor DarkGray
& $Adb devices
