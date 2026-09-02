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
- [ ] Visual sign-off against the prototype on a device

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

## Verification

`python tools/check_repo.py` · `python tools/validate_trip.py` ·
`./gradlew testDebugUnitTest assembleDebug`
