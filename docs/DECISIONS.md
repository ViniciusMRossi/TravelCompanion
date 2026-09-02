# Lightweight Decisions

Record only decisions that materially affect maintainability or visible behavior.

- **D001 — One app module for V1.**
- **D002 — Firebase deferred from bootstrap.** Group sync already has an interface boundary.
- **D003 — Stable API 36 baseline.**
- **D004 — Runtime state never mutates packaged trip content.**
- **D005 — Real font binaries are added later; design typography remains locked.**
- **D006 — Lifecycle pinned to 2.10.0.** 2.11.0 requires compileSdk 37 / AGP 9.1.0 and cannot build on the API 36 baseline. See `docs/technical/DEPENDENCY-BASELINE.md`.
- **D007 — JDK 17 toolchain is auto-provisioned.** Foojay resolver in `settings.gradle.kts` keeps the pinned JDK 17 baseline working on machines that only have Android Studio's JDK 21.
- **D008 — The packaged trip is the sample Day 9 dataset.** `app/src/main/assets/trip/trip.json` is a copy of `trip-package/sample/sample-trip.json`; the minimal starter could not exercise screens 02 and 05. The approved `02 -> 05` flow opens **Baščaršija**, so that attraction, its audioguide and its rain Plan B were transcribed from the approved prototype into the sample package. Latin Bridge stays in the package as a second attraction that does *not* start the walk.
- **D009 — Trip length comes from the trip date window,** not from how many days are packaged, so "Dia 9 de 21" stays true while content for the other days is produced. The sample `endDate` was `2026-10-02`, a 20-day window that contradicted the approved "21 dias"; it is now `2026-10-03`. `validate_trip.py` checks every `dayNumber` against the window so this class of drift cannot return.
- **D010 — No image-loading library.** Packaged photography is decoded straight from assets; a missing binary falls back to the approved striped placeholder.
- **D011 — A walk names its starting attraction explicitly.** `walk.startAttractionId` was added to `trip.schema.json`. Sharing a `cityId` is not an association: every attraction in Sarajevo would otherwise claim the walk departs from it, and screen 05 would offer "Iniciar passeio" from the wrong place.
- **D012 — Duplicate critical items are merged, never de-duplicated.** The same item is routinely declared twice — curated on the day and in full on the transport that owns it. `distinctBy(id)` silently dropped "Ver ponto de embarque"; `CriticalItem.mergedWith` now unions the action links and keeps the first non-null of every field.
- **D013 — "Offline" is earned, not declared.** A document is only badged offline when `availableOffline` is true *and* `AssetResolver` finds the packaged file. Claiming offline access over a missing PDF is the one failure that only surfaces at a bus station.
- **D014 — `validate_trip.py` checks what JSON Schema cannot:** unresolved references, duplicate IDs, `dayNumber` against the trip window, and documents that promise offline access without a packaged file. Findings are warnings while `contentStatus` is prototype/draft and errors once it is `production`.
