# Dependency Baseline

Pinned: 2026-09-01.

The starter uses a stable API 36 baseline.

- JDK 17
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- Kotlin 2.3.20
- compileSdk / targetSdk 36
- minSdk 26
- Compose BOM 2026.06.00
- Activity Compose 1.13.0
- Lifecycle 2.10.0
- Navigation Compose 2.9.8
- DataStore 1.2.1
- Media3 1.10.1
- Play Services Location 21.4.0

Toolchain resolution: `settings.gradle.kts` applies the Foojay resolver so Gradle
provisions the pinned JDK 17 toolchain on machines that only ship Android
Studio's bundled JDK 21.

Firebase is deliberately not wired in Phase 0. Add it later behind `GroupSyncRepository`.

Do not upgrade dependencies just because newer versions exist; keep dependency changes intentional.

## Correction applied 2026-09-02

Lifecycle was pinned at 2.11.0, which requires `compileSdk 37` and AGP 9.1.0 and
therefore could not build against the API 36 baseline (D003). It is pinned to
2.10.0, the newest release compatible with AGP 8.13.2 / compileSdk 36. Revisit
when the API baseline itself moves.
