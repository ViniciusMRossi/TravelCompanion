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

## Phase 2 — Local audio

**Implemented, pending real-device verification.**

- [x] Media3/ExoPlayer in a `MediaSessionService`
- [x] `PlaybackController` separated from screens, application-scoped (D015)
- [x] Local audio resolved through `AssetResolver` as `asset:///` (D017)
- [x] Play / pause from the persistent compact player
- [x] Observable position and duration (`StateFlow<PlaybackState>`)
- [x] Position persisted per guide and restored on the next play
- [x] A finished guide replays from the start (D022)
- [x] Chapters tracked from position; the compact player names the current one
- [x] Screen 05 keeps its approved action; playback surfaces in the design
      system's persistent compact player (D020)
- [x] Notification/lock screen show the guide title and context, not an id
- [x] An unplayable guide never disturbs audio that is playing (D021)
- [x] No network and no group-sync dependency
- [x] Documented prototype audio placeholder so the flow can be exercised (D019)
- [ ] **Not verified on hardware** — see below
- [ ] **Seek is deferred to Phases 3/4** (approved). `seekTo` / `seekBy` exist
      on the controller and are unit-tested; the −15 / +15 transport belongs to
      screens 07 and 09. `TcAudioPlayerVariant.Full` implements it and waits.
- [ ] **Chapter selection is deferred to Phase 6** (approved). `seekToChapter` /
      `skipToNextChapter` exist and are unit-tested; the approved chapter list
      lives on screen 04 (Cidade).

### What could NOT be confirmed

No device, AVD or system image is available in this environment
(`adb devices` empty, `emulator -list-avds` empty, no `system-images`), so the
following are implemented but **unverified by observation**:

- playback continuing with the screen off;
- playback surviving navigation on a real device;
- notification and lock-screen transport controls, and the metadata they show;
- headset / Bluetooth media buttons;
- AudioFocus interruption and resume (incoming call, another app);
- behaviour when the phone becomes "noisy" (headphones unplugged);
- that the packaged placeholder WAV actually decodes on a device.

These are covered in code (`PlaybackService` sets `handleAudioFocus = true` and
`setHandleAudioBecomingNoisy(true)`; the session provides the notification and
media-button handling) and by unit tests at the controller boundary, but code
and tests cannot substitute for the device matrix in the brief.

### Reaching seek and chapters — decided

The capabilities are implemented and tested; what was missing was an *approved*
place to put them. The canonical 19 screens contain no solo full player:
screen 09 is **Ouvir juntos**, the shared player (Phase 4), screen 07 is Walk
Mode's transport (Phase 3), and the chapter list is part of screen 04 (Phase 6).

**Decision taken: option (a).** Seek and chapter selection are deferred to the
already-approved surfaces of Phases 3/4/6. No solo player screen is created,
and neither ±15 nor chapter selection is added to the compact player. Phase 2
ships play/pause as its reachable behaviour (D025).

### Audio content status

`trip/audio/attractions/bascarsija.prototype.wav` is a **synthetic placeholder**,
not a recording — origin, licence and replacement steps are in
`app/src/main/assets/trip/audio/README.md`. It runs the approved **720 s**, so
screen 05 reads "Ouvir audioguia · 12 min" exactly as the prototype specifies
and the stated duration matches the file on the device. Its three chapters are
marked by audible tones at 0 / 4 / 8 min; the titles are labelled as prototype
rather than posing as narration.

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

Unit tests: 74 passing. Lint: 0 errors, 25 warnings (dependency-hygiene
notices; no lint baseline is used). Instrumented/device tests: not run — no
emulator or device is available in this environment.
