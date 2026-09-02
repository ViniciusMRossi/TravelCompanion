@echo off
setlocal

set GRADLE_VERSION=8.13
set BOOTSTRAP_ROOT=%USERPROFILE%\.gradle\tc-bootstrap
set GRADLE_HOME=%BOOTSTRAP_ROOT%\gradle-%GRADLE_VERSION%
set ZIP=%BOOTSTRAP_ROOT%\gradle-%GRADLE_VERSION%-bin.zip
set URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo Bootstrapping Gradle %GRADLE_VERSION% from %URL%
  if not exist "%BOOTSTRAP_ROOT%" mkdir "%BOOTSTRAP_ROOT%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop';" ^
    "Invoke-WebRequest -Uri '%URL%' -OutFile '%ZIP%';" ^
    "if (Test-Path '%GRADLE_HOME%') { Remove-Item -Recurse -Force '%GRADLE_HOME%' };" ^
    "Expand-Archive -Path '%ZIP%' -DestinationPath '%BOOTSTRAP_ROOT%' -Force"
  if errorlevel 1 exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
