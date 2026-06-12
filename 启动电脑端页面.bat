@echo off
setlocal
cd /d "%~dp0"

if not exist "node_modules" (
  echo Installing dependencies...
  call npm install
  if errorlevel 1 pause & exit /b 1
)

call npm run build
if errorlevel 1 pause & exit /b 1

start "Codex Companion Bridge" powershell -NoExit -ExecutionPolicy Bypass -Command "Set-Location -LiteralPath '%~dp0'; npm run start:bridge"
timeout /t 3 /nobreak >nul
start "" "http://127.0.0.1:4518/app"

echo Codex Companion is starting at http://127.0.0.1:4518/app
endlocal
