# Lightweight Decisions

Record only decisions that materially affect maintainability or visible behavior.

- **D001 — One app module for V1.**
- **D002 — Firebase deferred from bootstrap.** Group sync already has an interface boundary.
- **D003 — Stable API 36 baseline.**
- **D004 — Runtime state never mutates packaged trip content.**
- **D005 — Real font binaries are added later; design typography remains locked.**
- **D006 — Lifecycle pinned to 2.10.0.** 2.11.0 requires compileSdk 37 / AGP 9.1.0 and cannot build on the API 36 baseline. See `docs/technical/DEPENDENCY-BASELINE.md`.
- **D007 — JDK 17 toolchain is auto-provisioned.** Foojay resolver in `settings.gradle.kts` keeps the pinned JDK 17 baseline working on machines that only have Android Studio's JDK 21.
- **D008 — The packaged trip is the sample Day 9 dataset.** `app/src/main/assets/trip/trip.json` is a copy of `trip-package/sample/sample-trip.json`; the minimal starter could not exercise screens 02 and 05. The attraction already described in that package was also placed on the day timeline so `02 -> 05` is reachable. No new travel content was authored.
- **D009 — Trip length comes from the trip date window,** not from how many days are packaged, so "Dia 9 de 20" stays true while content for the other days is produced.
- **D010 — No image-loading library.** Packaged photography is decoded straight from assets; a missing binary falls back to the approved striped placeholder.
