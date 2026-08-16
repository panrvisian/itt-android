@echo off
title Big Brother Mobile - Deploy Menu
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0menu.ps1"

echo.
echo [Menu exited] Press any key to close this window...
pause >nul
