# Travel Companion Android

Starter repository for implementing the approved **Travel Companion** Android app.

The product design is already locked. This repository is prepared so Claude Code can begin from a known baseline instead of re-planning the product.

## First run — Windows

```powershell
python tools/check_repo.py
.\gradlew.bat tasks
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

The first Gradle command downloads Gradle 8.13. The starter intentionally does not distribute the binary Gradle wrapper JAR.

Once Gradle is available, you may generate the standard wrapper and commit it:

```powershell
.\gradlew.bat wrapper --gradle-version 8.13
```

## Requirements

- Android Studio
- JDK 17+
- Android SDK 36
- Internet on first dependency download

## Start with Claude Code

Clone/unzip this repository, open Claude Code in the repository root and use:

> Read CLAUDE.md and docs/START-HERE.md. Verify the repository baseline, then continue Phase 0 without redesigning the product.

## Already scaffolded

- Kotlin + Jetpack Compose
- Field Companion token/component scaffold
- five-root navigation shell
- first-run participant selection persisted with DataStore
- packaged local `trip.json`
- typed starter trip loader/repository
- Today screen fed by local trip data
- group-sync interface with no-op implementation
- Media3 and location dependencies ready for later phases
- approved design artifacts and prototype
- `trip.schema.json`, sample/starter packages, validation scripts
- lightweight implementation status/decisions documents

## Deliberately not wired yet

- Room
- Firebase
- MediaSessionService
- Walk Mode/background location
- voice recording
- Wallet/PDF/QR
- weather
- notifications

Follow `docs/technical/TECHNICAL-IMPLEMENTATION-BRIEF-v1.md`.

## Optional: initialize Git immediately

Windows:

```powershell
.\scripts\init-git.ps1
```

macOS/Linux:

```bash
./scripts/init-git.sh
```

The first Claude instruction is also saved in `FIRST-PROMPT-FOR-CLAUDE.md`.
