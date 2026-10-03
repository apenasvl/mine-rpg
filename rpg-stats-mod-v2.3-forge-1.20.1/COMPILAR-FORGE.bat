@echo off
setlocal
pushd "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\build-forge.ps1"
set "RPG_BUILD_EXIT=%ERRORLEVEL%"
echo.
if not "%RPG_BUILD_EXIT%"=="0" echo Falhou. Envie o arquivo build-forge.log desta pasta.
pause
popd
exit /b %RPG_BUILD_EXIT%
