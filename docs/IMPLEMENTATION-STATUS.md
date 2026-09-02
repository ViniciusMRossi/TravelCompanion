# Implementation Status

Keep this short. This is not an SDD.

## Phase 0 — Foundation

- [x] Repository structure
- [x] Gradle/Kotlin/Compose baseline
- [x] Field Companion token scaffold
- [x] Navigation shell
- [x] Packaged starter `trip.json`
- [x] Starter typed trip loader/repository
- [x] Participant identity via DataStore
- [x] Group sync boundary / no-op implementation
- [x] Confirm Gradle sync on developer machine
- [x] Confirm unit tests
- [x] Confirm debug APK
- [ ] Confirm starter on emulator/device
- [ ] Add Room when structured runtime persistence is first needed

Phase 0 required two baseline corrections before it would build; see D006 and
D007 in `DECISIONS.md`.

## Phase 1 — Approved visual foundation

Implemented from the approved prototype, **not yet verified running**.

- [x] 01 Quem é você?
- [x] 02 Hoje
- [x] 05 Atração
- [x] Shared Field Companion components
- [x] Full `trip.schema.json` projection + `TripContent` lookups
- [x] `AssetResolver` with placeholder fallback
- [x] Today domain layer (current day, Now/Next, timeline state, critical items)
- [x] Field Companion icon set converted from the approved sprite
- [ ] Fraunces binary (still the serif fallback — D005)
- [ ] Real photography (heroes render the approved striped placeholder)
- [ ] **Visual fidelity is unverified.** The app has never been run on a device
      or emulator in this environment (none available), so no claim of visual
      fidelity to the approved prototype has been confirmed by observation.
      Structure, geometry, colour and copy were implemented from the prototype
      source; only a device run can confirm the result.

## Phase 2
- [ ] Local audio / MediaSessionService

## Phase 3
- [ ] Walk Mode / location / stories

## Phase 4
- [ ] Firebase group synchronization

## Phase 5
- [ ] Voice memories

## Later
- [ ] Remaining canonical screens
- [ ] Weather (live/cached states; only the trip fallback exists today)
- [ ] Notifications
- [ ] Real Balkans package
- [ ] Full Trip Validator
- [ ] Real-device QA

## What runs today

```text
01 Quem é você?  →  02 Hoje  →  05 Atração
```

Destinations that belong to later phases (Dia completo, Documento/QR, Plano B,
Modo Passeio, Audioguia, Memória) exist in the navigation graph and open a
screen that names the canonical screen and its phase.

## Content validation

`tools/validate_trip.py` now also checks what JSON Schema cannot: unresolved
references, duplicate IDs, `dayNumber` against the trip window, and documents
that declare `availableOffline` without a packaged file. These are warnings
while `metadata.contentStatus` is prototype/draft and errors once it is
`production` (D014).

Currently reported for the packaged trip: the two ticket/voucher PDFs are
declared offline but their files are not packaged yet. The UI reflects this —
it does not badge them "Offline" (D013).

## Verification

`python tools/check_repo.py` · `python tools/validate_trip.py` ·
`./gradlew testDebugUnitTest assembleDebug lintDebug`

Unit tests: 37 passing. Lint: 0 errors, 25 warnings (dependency-hygiene
notices; no lint baseline is used). Instrumented/device tests: not run — no
emulator or device is available in this environment.
