$ErrorActionPreference = "Stop"

if (Test-Path ".git") {
    Write-Host "This folder is already a Git repository."
    exit 0
}

git init
git add .
git commit -m "chore: bootstrap Travel Companion Android"

Write-Host ""
Write-Host "Git repository initialized." -ForegroundColor Green
Write-Host "Next: create a remote repository and add it with git remote add origin <url>"
