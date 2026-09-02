$ErrorActionPreference = "Stop"

Write-Host "Travel Companion bootstrap" -ForegroundColor Cyan
python tools/check_repo.py

if (-not (Test-Path ".venv")) {
    python -m venv .venv
}

& .\.venv\Scripts\python.exe -m pip install -q -r tools\requirements.txt
& .\.venv\Scripts\python.exe tools\validate_trip.py app\src\main\assets\trip\trip.json

Write-Host ""
Write-Host "Next:" -ForegroundColor Green
Write-Host "  .\gradlew.bat tasks"
Write-Host "  .\gradlew.bat test"
Write-Host "  .\gradlew.bat assembleDebug"
