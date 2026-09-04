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
- [x] Confirm starter on emulator/device (2026-09-04, two emulators)
- [x] Add Room when structured runtime persistence is first needed (Phase 5: voice memories are the first table — D057)

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
- [x] **Visual fidelity compared with the approved prototype** on 2026-09-04,
      across two emulator sizes, for every screen that exists (01, 02, 05, 06,
      07, 08, 09 and the persistent compact player). Two divergences were found
      and corrected (D051); one system-bar defect was found and corrected
      (D052); four differences went to the design-confirmation stack, which has
      since been decided and emptied (D053–D055). What was compared, what matched and what did
      not is under "The first visual pass" below. Fraunces is still absent
      (D005), photography is still the approved striped placeholder, and
      screen 09's hero still has no asset to resolve (D036) — none of those
      were treated as divergences.

## Phase 2 — Local audio

**Verified on hardware.** Samsung SM-S921B (Galaxy S24), Android 16 (API 36),
Media3 1.10.1, over ADB on 2026-09-03.

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
- [x] **Verified on hardware** — see below
- [ ] **Seek is deferred to Phases 3/4** (approved). `seekTo` / `seekBy` exist
      on the controller and are unit-tested; the −15 / +15 transport belongs to
      screens 07 and 09. `TcAudioPlayerVariant.Full` implements it and waits.
- [ ] **Chapter selection is deferred to Phase 6** (approved). `seekToChapter` /
      `skipToNextChapter` exist and are unit-tested; the approved chapter list
      lives on screen 04 (Cidade).

### Confirmed by observation on the device

Each of these was watched on the Galaxy S24, with `dumpsys media_session`,
`dumpsys audio` and logcat as evidence. No crash, ANR or ExoPlayer/Media3
error appeared in any run.

- first playback after a cold start — audio ~1.3 s after the tap;
- playback surviving navigation between screens and root tabs;
- playback continuing with the screen off, held by
  `PARTIAL_WAKE_LOCK 'ExoPlayer:WakeLockManager'`;
- notification and lock-screen transport, showing the guide title and its
  context and never a media id;
- headset media buttons, from a Bluetooth headset's own button — outside a
  shared listen. Inside one the group wins and a pause from the headset, the
  notification or the lock screen is undone within about five seconds; only
  screen 09's own transport speaks for the group (D047, D049);
  (`MediaKeyEvt pkg=com.android.bluetooth`), pause and play both ways;
- pausing instead of switching to the speaker when the headphones go away
  (`AS.AudioDeviceBroker: broadcast ACTION_AUDIO_BECOMING_NOISY`);
- AudioFocus in both cases the brief names: another app taking focus
  (`onAudioFocusChange(-1)` → pause) and an incoming call, which paused on the
  ring and **resumed on its own** when telecom abandoned focus;
- position persisted across `force-stop` and restored on the next play;
- a finished guide replaying from the start, not from its end;
- the whole flow with Wi-Fi and mobile data off — cold start, navigation and
  playback with `Active default network: none` and no IP route at all;
- the packaged placeholder WAV decoding natively at its own rate
  (`FormatInfo{channelMask=0x1, sampleRate=8000}`) for exactly 720 s
  (`duration = 720000 ms`), which the UI reads as `12:00`;
- the Activity being destroyed and recreated on rotation
  (`Checking to restart … changed={CONFIG_ORIENTATION}` →
  `finishDrawing of relaunch`) without interrupting the audio, because the
  controller lives on the application;
- tapping the notification returning to the task on the screen the traveller
  had left, with a single Activity instance in the task.

### Fixed after the device run

Three defects the device run found, all in how playback surfaces rather than in
playback itself, and all re-verified on the device afterwards:

- the compact player and fixed bars such as "Iniciar passeio" were drawn under
  the system navigation bar on every route without bottom navigation, because
  the only `navigationBars` inset lived inside `TcBottomNavigation`. The shell
  now reserves `WindowInsets.systemBars` once, for every route;
- the notification had no `contentIntent`, so tapping it did nothing.
  `setSessionActivity` now carries an immutable `PendingIntent` to
  `MainActivity` with `NEW_TASK or SINGLE_TOP` — `NEW_TASK` alone stacked a
  second Activity showing Hoje instead of resuming the screen in view;
- the notification and lock screen offered "Ir para o item anterior", which a
  single-item audioguide cannot honour. The player is now wrapped so it does
  not report the queue commands. Restricting the media notification
  controller's commands was not enough: `DefaultMediaNotificationProvider`
  builds its buttons from `player.getAvailableCommands()`, and media button
  preferences can only replace that button, never remove it.

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

## Phase 3 — Walk Mode, location and stories

**Verified on hardware.** Samsung SM-S921B (Galaxy S24), Android 16 (API 36),
over ADB on 2026-09-03, against the final binary.

- [x] Screen 06 Iniciar passeio: the ink card, the readiness checklist and the
      participant pills, all read from the packaged trip and the device
- [x] Screen 07 Passeio ativo: ink field, segmented walk progress, the walking
      instruction as the most legible thing on screen, next stop, and the
      location state as a dot and a word
- [x] The §19 state machine explicit rather than inferred — Idle, Preparing,
      Active, Paused, Finishing, Completed — as pure functions in `domain/walk`
- [x] §20 story triggering: idempotent, `notifyOncePerTrip` enforced against a
      persisted record, and the active walk's route taking precedence over
      passive geofence behaviour
- [x] Foreground location while the walk runs, behind the persistent
      notification Android requires, and released the moment it ends
- [x] The −15 / +15 transport on screen 07 (D025's deferred seek, D029)
- [x] Location permission asked immediately before Walk Mode needs it, never at
      first launch
- [x] Headphone connection reported from the device rather than asserted
- [ ] **Screen 10 História pelo caminho and screen 11 Fim do passeio are not
      implemented.** Closing the walk completes it and returns; a story
      triggered outside a walk posts a notification that opens the app. Both
      canonical screens remain placeholders, as later-phase screens already are.
- [ ] **Passive background geofencing is not implemented.** The decision layer
      carries the outside-a-walk branch and is tested, but registration of
      background geofences — and the background-location permission flow it
      needs — was not in this phase's scope. Only the foreground strategy runs.
- [ ] **The `Paused` phase has no approved trigger yet.** §19 lists it and the
      state machine implements it, but screen 07's only transport is the
      audio's; nothing in the approved design pauses the walk itself, so
      `WalkModeController.pause()` has no caller. Kept because the brief asks
      for the state, recorded here rather than left to be discovered.

### Confirmed by observation on the device

Each item below was watched on the Galaxy S24 against the final binary, with
`dumpsys`, the app's own DataStore and logcat as evidence. No crash, ANR or
ExoPlayer/Media3 error appeared.

- the walk surviving the screen going off: `mAwake=false` with the service
  still `types=0x00000008` and the notification reading "Parada 1 de 2", and
  `ProviderRequest[@+5s0ms, HIGH_ACCURACY, WorkSource{com.travelcompanion.app}]`
  still registered on the gps provider;
- audio and story triggering with Wi-Fi and mobile data off — two stories
  triggered and recorded with `Active default network: none` and no IP route;
- headset media buttons during the walk, from the Bluetooth headset's own
  button (`MediaKeyEvt pkg=com.android.bluetooth`), pause and play both ways,
  with the walk still in the foreground;
- `notifyOncePerTrip` surviving process death: after `force-stop` and a fresh
  walk, three arrivals at already-triggered stories added no record and moved
  the walk not at all;
- −15 / +15 moving the position for real (−14472 ms and +33200 ms measured,
  each including the seconds that played during the measurement);
- a refused location permission leaving the walk and the audio running, with
  no foreground service and no GPS request at all.

**Not observed, and not claimed:** arrival at the real Baščaršija coordinates.
Story triggering was driven through the approved prototype's debug-only
scaffold (D031), which feeds the packaged story's own coordinates through the
same `onLocation` a GPS fix uses — the decision exercised is identical, only
the source of the coordinate differs. That real fixes arrive at all *was*
observed: screen 07's "Localização ativa" only turns on when a fix reaches the
controller, and the gps provider shows the app's request with a real position.

### Found by running it on a device

Four defects that the unit tests did not catch, all fixed and re-verified:

- refusing the location permission **crashed the app**. Android refuses a
  foreground service of type `location` without a location permission, and the
  refusal is a `SecurityException` thrown inside `onStartCommand` — past the
  `runCatching` that guarded the call site. Worse, the unit test asserted the
  wrong behaviour: it expected the service to start anyway. Code and test are
  both corrected (D030);
- `playedAt` was recorded for a story whose audio is not packaged. The record
  claimed audio that never reached the traveller, because `audioGuideRequest`
  answers `NotPackaged` rather than null. §20 keeps `playedAt` separate from
  `triggeredAt` precisely so the two can disagree;
- the debug-only scaffold **was not debug-only**. Behind `BuildConfig.DEBUG` in
  `main`, and with no R8 in this project, both `simulateArrival` and the string
  "simular chegada" were present in the release DEX — unreachable, but shipped.
  Reading the binary is what showed it; the source looked correct. It now lives
  in `src/debug` with a null-returning counterpart in `src/release`, and the
  rebuilt release DEX carries neither. Re-checked in Phase 4 against the
  current binary: the label string appears in the debug DEX and in no
  release one. Grep the label, not the old symbol name (D031);
- ending a walk could leave an undismissable notification. `stopWalk()` relied
  on the foreground service owning it, but since D030 no service is started
  without a location permission, and the walk still posts progress. The
  notification is now cancelled explicitly (D032).

## Phase 4 — Group synchronization, screens 08 and 09

**Verified on hardware, two devices, 2026-09-04.** Samsung SM-S921B
(Galaxy S24, Android 16, API 36) as Vinícius and a Pixel_10 AVD
(`google_apis_playstore`, Android 17 / API 37.1, x86_64, Play Store image) as
Érika, over ADB against a debug build with a real Firebase project configured.

The original Phase 4 pass (2026-09-03, one phone, the Firebase console standing
in for the other) is superseded: everything it claimed has been re-watched
against the current binary with a real second device, and three of its claims
turned out to be artefacts of the console standing in — see "The second device"
below.

- [x] Screen 08 Sincronizando: the shared start as a 3 → 2 → 1 transition into
      screen 09, not a place anyone navigates to
- [x] Screen 09 Ouvindo juntos: the teal 52 / 68 / 52 transport, the
      participant list, the transcript toggle, and the amber note for a group
      that could not be reached
- [x] A real `GroupSyncRepository` over Firebase Realtime Database, behind the
      boundary Phase 0 already had (D002) — `FirebaseGroupSyncRepository`
      replaces nothing above it
- [x] The shared start anchored on **server** time, never a device wall clock:
      `startTogether` publishes `serverNow + 3s`, so both phones compute the
      same moment even when their clocks disagree (brief §6)
- [x] Drift correction from the group's anchor, with a two-second tolerance,
      as pure functions in `domain/sync`
- [x] Participant states from each phone's own `seenAt` against server time,
      rather than assuming everyone present is keeping up (D040)
- [x] Anonymous, invisible sign-in so database rules can require `auth != null`
      — no account, no screen, nothing entered (D035)
- [x] Group sync never blocks local behaviour: every path from the group into
      the player goes through one function, and no branch of it stops, pauses
      on failure, or unloads audio (brief §3.3)
- [x] **Two devices in the group.** The list below is what each of them did.
- [ ] **Screen 09 shows the striped placeholder, not a photograph.** Schema 1.1
      gives `story` no asset field and no link to the attraction that has one,
      so there is nothing to resolve the hero image from. Guessing the
      convention was written and then removed rather than shipped (D036).
- [ ] **A paused group with divergent positions is left alone.** When the group
      is paused and two phones sit at different points, `syncCorrection`
      answers `None` rather than seeking a paused player around. It resolves
      itself on the next resume, which re-anchors both. Deliberate, recorded
      here rather than left to be discovered.

`google-services.json` belongs at `app/google-services.json` and is not in the
repository. The `com.google.gms.google-services` plugin is applied only when
that file exists, so a fresh clone still builds; without it the repository
reports `Disabled` and everything else behaves identically (D034). No project
id, database URL or key is recorded in the repository or in these notes.

### Confirmed by observation — one phone and the Firebase console (2026-09-03)

Superseded by the two-device pass below, and kept because it is where three of
this phase's defects were found. Watched on the Galaxy S24 with
`dumpsys media_session`, screenshots and the Firebase console driving the other
side. No crash, ANR or Media3 error.

- **the group pausing this phone**: `isPlaying` set to false in the console
  moved the local player to `PAUSED(2), position=314037`;
- **drift correction landing exactly**, in both directions and in one run:
  moving the anchor into the future seeked the player *back* from 126176 to
  71151 — the group's own `positionMs`, since elapsed time clamps at zero —
  and then setting `positionMs` to 360000 seeked it to **exactly 360000**,
  still `PLAYING`, with no residue;
- **a quiet but reachable group staying green**: ninety seconds idle on screen
  09 with nobody touching anything, audio at 01:59, "Sincronizado" and no note;
- **a real outage raising the amber state**: radios off at 22:22:47, and at
  22:23:18 the traveller's own row read "Sincronizando novamente" with the
  amber note under it;
- **recovery without a restart**: radios back at 22:23:19, and at 22:23:49 the
  row read "Sincronizado" and the note was gone;
- **audio untouched throughout the outage**: 03:10 at the amber capture and
  03:41 thirty-one seconds later, `PLAYING` at both — continuous to the second.

**What this pass could not see, and why it mattered:** the console is not a
phone. It changes the group's data, which is precisely the case that kept
working after the real one broke (D046), and it never pauses anything itself,
which is why a pause that no code ever published looked verified (D047). Both
were found the first hour two devices were in the group.

### Found by running it on a device

Two defects the unit tests did not catch, both fixed and re-verified:

- **screen 09 accused a healthy group of having failed.** Left idle with the
  radios on and Firebase plainly reachable — console edits were landing within
  a second — the amber note appeared after twenty quiet seconds and stayed.
  The staleness rule measured silence, and two people listening to the same
  guide are silent for minutes at a time, so a normal shared listen was
  indistinguishable from a dead one. Liveness is now an acknowledged round
  trip, which is the one thing silence cannot fake (D039);
- **the screen told two contradictory things at once.** "Vinícius · você —
  Sincronizado" sat directly above "Não foi possível sincronizar o grupo
  agora". The traveller's own row was hard-coded to synchronized on the
  grounds that a phone knows its own playback — true, but not when it is that
  phone that lost the group. Status and the traveller's row now move together
  (D040).

A third defect was found after those two, by reasoning rather than by
watching: the heartbeat makes the group's snapshot arrive every few seconds
instead of only when someone acts, which puts every branch of the correction
on the same loop. Nothing publishes a stop when a guide simply ends, so a
finished shared listen left the group saying "playing" and resurrected the
guide on each beat — a failing test showed five snapshots producing five
commands to the audio engine. A correction that points at or past the end of
the loaded guide is now `None` (D039). This one was never observed on the
device and is not claimed as such.

`NoOpGroupSyncRepository` was written this phase and deleted before it was
committed: the no-configuration case is already handled by the Firebase
repository reporting `Disabled` (D034), so nothing ever constructed it.

### The second device — what two phones showed (2026-09-04)

Vinícius on the Galaxy S24, Érika on the Play Store emulator image, both on
the packaged trip, both signed in by nobody. Positions below are
`dumpsys media_session` in milliseconds unless a screen is quoted.

**Observed, first time:**

- **a shared start reaching the second phone.** Screen 08 captured on both:
  the S24 counting "2" with both avatars, and the emulator counting "2" as a
  *joiner* — the guide it was about to hear was the group's, not its own;
- **two phones narrating in step, and staying there.** The joining phone
  landed at 126821 while the other was at 126879 — **58 ms apart** — and the
  pair ran the whole 12-minute guide to the end together, stopping at 720007
  and 720005, **2 ms apart**. Intermediate samples: 107 ms, 160 ms, 310 ms,
  467 ms, 542 ms;
- **another participant's row rendered from a real phone.** Érika's row on the
  S24 went amber, "Sincronizando novamente", when her device actually lost the
  network — the S24's own row staying green beside it (screenshot);
- **F1, both sides of it.** A phone holding nothing joined a listen already
  running and took the group's guide at the group's position (above). The
  other side — a phone holding a *different* guide — **could not be produced**:
  see below;
- **F2.** With the second phone deliberately 32 s ahead (07:34 against 08:06),
  it left screen 09 and came back: the first phone carried straight on,
  07:41 → 08:00, never pulled to the other's position, and the second was
  seeked *back* into step. Before 832e88e this dragged the other traveller
  every time the screen was reopened;
- **F3.** Sampled across a proposal at ~0.3 s intervals, the proposing phone's
  position only ever advanced — 295546 → 298547 → 315040 — with no seek back
  to the anchor position. The sub-second behaviour inside the three seconds is
  the unit test's (D041); what the device adds is that nothing visible moves;
- **the clocks, which is what §6 exists for.** The emulator's wall clock was
  pushed **7 min 19 s** behind the S24's (`cmd time_detector`, both then
  restored to automatic). With the two devices that far apart, the joining
  phone landed at 706335 against 707409 — **1.07 s** — and a second, fresh
  shared start under the same skew landed 16974 against 16994, **20 ms**. A
  device wall clock would have put them 439 s apart in a 12-minute guide. This
  is the one claim in §6 that no unit test can close, and it is now closed;
- **remote pause and remote resume, phone to phone.** Pause on the S24 at
  283625; the emulator followed to 283313, **312 ms**. Play on the S24; the
  emulator resumed within one round. Neither was possible before this commit
  (D047);
- **drift closing with nobody writing anything.** A pause dispatched to the
  emulator's media session — deliberately bypassing screen 09, so nothing was
  published — was undone by the beat and the phone brought back into step
  (D046). Before this commit it stayed paused indefinitely. The same
  observation, read as the traveller would meet it: inside a shared listen a
  pause from the headset, the notification or the lock screen comes back on
  its own within about five seconds, because the group is still playing and
  only screen 09's transport speaks for the group. Recorded as a decision in
  D049 rather than left as a side effect, with the alternative flagged for
  confirmation rather than implemented;
- **ninety seconds idle staying green.** Nobody touching either phone: both
  read "Sincronizado", no note, and afterwards 409514 against 410056, still in
  step;
- **§3.3, the way it was always meant to be watched.** Airplane mode on the
  emulator at 08:42:18. Within 25 s its screen showed both rows amber and the
  approved note, "Não foi possível sincronizar o grupo agora. Seu audioguia
  continua funcionando normalmente."; the S24 showed itself green and Érika
  amber. **Both phones kept narrating throughout** — the emulator 425077 →
  455107 → 506151, the S24 424543 → 454600 → 505684, monotonic across the
  outage. Network back at 08:43:02; within 30 s both rows were green again on
  both phones, with no restart. Only the group's state was ever unavailable.

**Found by running it on two devices, and fixed here:**

- **a group node outliving the listen it describes** — the very first thing two
  phones hit, and it stopped the shared start from ever happening (D045);
- **corrections stopping once the group settled** — the `StateFlow` conflating
  equal snapshots, invisible to a pass whose "other participant" was a console
  making the data change (D046);
- **the transport on screen 09 publishing nothing** — pause, resume and ±15
  were local-only, so the two phones desynchronised silently while both read
  "Sincronizado" (D047);
- **screen 06 calling the wrong participant "você"** — labelled by list
  position, so Érika's phone called itself Vinícius (D048);
- **an ask spent on an answer that had nothing in it** — introduced by the
  D045 fix and caught by the same two phones an hour later (D045).

**Not observed, with reasons:**

- **the divergence state — "Ouvindo outra história" — was never rendered on a
  device.** The runtime package carries exactly one playable audioguide
  (`audio.bascarsija`; the city and Latin Bridge assets are declared but not
  packaged), so two phones cannot hold two different guides. Adding a second
  placeholder asset is a change to `trip.json`, which D043 froze until the
  content phase. Covered by unit tests only;
- **a paused group node from an earlier session** — see D045 for why it was
  left rather than guessed at;
- **the joining phone shows "Comece um audioguia para ouvir junto" for about a
  second** while the group is being asked — measured at roughly 1.2 s of a
  13 s screenshot series, longer on a cold start where anonymous sign-in is a
  round trip. Honest at that instant and self-correcting, but it is the wrong
  sentence for someone who has just tapped "Ouvir juntos". *(Corrected: the
  screen now says "Esperando o grupo. Assim que alguém começar, você entra
  junto." — see D054.)*;
- **screen 07 announces a story whose audio is not packaged.** Simulating
  arrival at Latin Bridge put "TOCANDO AGORA · Latin Bridge" in the header
  while the player carried on with Baščaršija, because that guide's asset is
  not in this build. *(Corrected: the header names the arrived story only while
  the player is on that story's guide, and otherwise names the walk — no new
  copy was needed. See D053.)*;
- **no emulator/device behaviour difference was observed** in anything above.
  The two agreed on every state, every note and every correction; the only
  differences were incidental — screen height, gesture bar, and the emulator's
  system language.

### Corrections after review — the second participant

Three synchronization defects and one process defect were found in review of
933e4a6 / 68ceda6. All three sync defects are invisible with one phone, which
is how they survived the device pass above. They are now covered by unit tests
that carry the other phone as a value — a `GroupPlayback` written by another
`updatedBy`, delivered as a sequence of snapshots — which is what `domain/sync`
was shaped for.

- [x] **A phone with nothing playing can now join a listen already running.**
      Nothing anywhere loaded `group.playback.mediaId`: the `Load` branch was
      `Unit`, and screen 09 answered "Comece um audioguia para ouvir junto"
      while the other phone counted down. Arriving on screens 08/09 is now the
      traveller's ask, and it is the only thing that can put the group's guide
      on this player (D042).
- [x] **A phone playing another guide keeps it, and the screen says the two
      are apart.** Two travellers on different stories both read
      "Sincronizado", with nothing anywhere reporting the disagreement. The
      guide is still never swapped (D037); the phone that is out of step now
      shows the amber dot with "Ouvindo outra história" and the note under the
      list (D042).
- [x] **Opening screen 09 no longer re-anchors the group.** Every arrival
      published a fresh anchor carrying this phone's position, so 09 → 07 → 09
      dragged the other traveller to wherever this one was. Proposing a start
      is now only for when there is nothing to join (D042).
- [x] **The countdown no longer drags the player backwards.** Found by
      arithmetic, not by watching: inside the three-second window the group's
      expected position stands still while the local one advances, so the
      drift rule fires and seeks back. Reproduced by a failing test before it
      was fixed — three snapshots inside one countdown produced one `seekTo`
      that should not exist — and now `None` until the agreed moment arrives
      (D041).
- [x] **Trip prose written outside the authoring pipeline is reverted.**
      `story.latin-bridge` went back to what it was before Phase 4 in both the
      runtime and sample packages, which stay byte-identical to each other
      (D043).

**Not implemented, with reasons:**

- [ ] **The other phone is not told that this one diverged.** Divergence is
      reported only on the phone that is out of step. The group payload is
      `{mediaId, positionMs, isPlaying, anchorServerMs, updatedBy}` plus
      `seenAt`, with one shared guide and no per-participant guide, so there is
      nothing for the other phone to read. Widening it is a payload change,
      not a correction, and was not made here (D042).
- [x] **"Ouvindo outra história" is approved.** A fifth participant state
      beside the four the boards draw, in their register, with no network
      vocabulary in it (D042).
- [x] **A phone joining a listen in progress hears the guide while 3–2–1 is
      still on screen — kept.** Screen 08 is a three-second transition into
      shared playback, and the joiner is given it so the screen is never blank
      while the guide loads; the group is already running, so the audio comes
      into step at once rather than at zero. Three silent seconds would tell
      the traveller less. Decided, not carried.
- [ ] **Divergence is still untested in the field.** One playable guide in the
      package means two phones cannot hold two different ones (D043).
- [x] **A phone arriving while the group is paused now says what it is
      waiting for.** The invitation is still kept rather than spent and the
      countdown still does not start (D044); what changed is that the screen
      says "Esperando o grupo. Assim que alguém começar, você entra junto."
      instead of telling the traveller to start an audioguide. The same
      sentence covers the second or so while the group is being asked (D054).
      Loading into a paused player is the other way out and was not built.
- [x] **Re-watched on two devices against the current binary**, including
      everything the one-phone pass had claimed. See "The second device".

### Corrected after the field passes

- [x] **Screen 07's header no longer names a story it is not playing.** Arriving
      at a story whose audio this build does not carry put "TOCANDO AGORA ·
      Latin Bridge" above a transport still running Baščaršija. The header names
      the arrived story only while the player is on that story's guide, and
      otherwise names the walk, which is what it already did before the first
      arrival. The walking instruction still follows the arrival: where to walk
      is true whether or not the story could be narrated (D053). Found on the
      device, fixed with four unit tests, and re-watched on the emulator at
      360 dp with the system font at 1.5.

## The first visual pass — screens against the prototype (2026-09-04)

The oldest open line in this repository. Four field passes had happened and
none had compared a screen with the approved prototype; all of them were about
behaviour.

**Setup.** Two emulator instances of the same AVD image
(`system-images/android-37.1/google_apis_playstore_ps16k/x86_64`, Play Store,
Android 17 / API 37.1), overridden to two sizes so a layout break would have
somewhere to show:

- **compact** — `wm size 720x1520`, `wm density 320` → **360 × 760 dp**;
- **large** — `wm size 1440x3120`, `wm density 480` → **480 × 1040 dp**.

Colours were compared by sampling screenshot pixels against
`Field-Companion-Design-Tokens-v0.1.json`, not by eye. Where
`TELAS-E-FUNCIONALIDADES-APPROVED.md` declares a measurement, the measurement
was taken.

**Measured against the declared numbers, and matching exactly:**

| Declared | Measured |
| --- | --- |
| screen 09 transport 52 / 68 / 52 dp | 52 / 68 / 52 dp (`TcAudioPlayer`) |
| screen 07 transport 56 / 76 / 56 dp | 56 / 76 / 56 dp (`TcWalkTransport`) |
| screen 08 avatars 72 dp | 144 px at 2 px/dp = 72.0 dp, 2 dp moss ring |
| screen 08 countdown circle 120 dp | 240 px at 2 px/dp = 120.0 dp, 2 dp border |

**Sampled colours, all exact:** paper `#F5F1E8`, surface `#FFFDF8`, ink
`#16232E`, the cool placeholder `#8FA3A0`/`#7F948F` and the paper placeholder
`#D7CFBC`/`#CDC4AE` (±1 from PNG rounding). `FieldCompanionColors` was read
against the token file line by line: brand, neutral scale and all four semantic
families match.

**Per screen:**

- **01 Quem é você?** — matches. Hero 262 dp, card min-height 76 dp, avatar
  44 dp, radius 14 dp, border `#C8CCC5`, gutters 20 dp and the 26/22/12/10
  rhythm are the prototype's numbers exactly. On compact the offline note falls
  below the fold; the screen scrolls, which is the prototype's own
  `overflow-y:auto`.
- **02 Hoje** — **diverged, corrected.** The location line and the day counter
  could be crushed to one character per line (D051). Everything else matches:
  the ink Now card with its teal quarter-circle, both critical cards with the
  oxblood rule and `#FDECEC` fill, weather and outfit cards, the five-item
  bottom navigation. Critical state covered by the package's own two
  "NÃO PODE DAR ERRADO" items.
- **05 Atração** — **diverged, corrected.** The hero was drawing nothing at
  all, so the white title sat on the card's surface and was invisible (D051).
  After the fix: cool stripes, legible eyebrow / title / subtitle, teal-subtle
  and neutral chips, 20 dp card radius, the teal bottom action bar.
- **06 Iniciar passeio** — matches. Ink walk card, readiness lines,
  participant pills, the bottom bar and its reassurance line. On compact the
  readiness card sits mostly below the fold; it scrolls.
- **07 Passeio ativo** — matches, in two location states: **denied** on
  compact, where the "Localização ativa" row is correctly absent, and
  **active** on large. Ink field, segmented progress, the walking instruction
  as the most legible thing on screen, 56/76/56 transport, "Ouvir juntos".
- **08 Sincronizando** — matches, measured above, on both sizes.
- **09 Ouvindo juntos** — matches, in both states: two green rows, and amber
  with the approved note after a real airplane-mode drop. Teal player card,
  52/68/52 transport, listeners card, "Próxima história".
- **the persistent compact player** — matches the S4 audio board in playing and
  paused states, on screens 05, 06 and 07: teal-subtle card, 46 dp teal circle,
  title, chapter line, progress track and times.

**Environment:**

- **system font at 1.5** — every screen holds after the D051 fix; the chapter
  line on screen 09 ellipsizes, which is what a single-line title should do;
- **night mode** — the app has no dark palette and stays light, correctly. It
  did turn the system-bar icons white on the paper strip above every screen,
  which is corrected (D052);
- **rotation** — landscape at 480 dp reflows, scrolls and keeps state; nothing
  overlaps and the bottom navigation is intact.

**Went to the design-confirmation stack — all four now decided:**

- the screen 02 location line and the screen 09 listener row were the same
  question twice, and it is answered once, as a rule: when a line will not
  fit, the operational identity wins and the qualifier gives way (D055). The
  city keeps its line and the country ellipsizes; the name keeps its line and
  the state word ellipsizes beside a dot that always survives. Re-verified at
  both sizes and at a 1.5 system font;
- the dark palette in the token file stays and is marked not implemented (see
  "One palette" below);
- screen 05's "no packaged audio" state was **not observed**: the runtime
  package's one audioguide is packaged, so the unplayable case cannot be
  reached from the UI. Screen 09's divergent state was not observed either,
  for the same reason already recorded (D043). Both remain observations rather
  than open questions — the behaviour is decided and unit-tested; what is
  missing is a package that can exercise it.

### One palette

The app implements `lightColorScheme` and nothing else: paper everywhere, with
ink used as a surface inside screens rather than as a window background.
`Field-Companion-Design-Tokens-v0.1.json` carries a complete dark palette that
nothing reads. It **stays in the file and is not in scope** — it is a design
artefact, and deleting it would lose the information rather than resolve it.
This is also why D052 pins the system bars to light: with one palette there is
nothing for a night setting to switch to, and letting it switch the bar icons
put white on paper.

### Two guards under the way this looks

The `TcHero` defect above was invisible to every one of the unit tests: a
modifier resolving to zero under an infinite constraint, which no state
assertion can see. Both cheap guards proposed for it are now built (D056), and
both were proved by reintroducing the defect and watching them fail.

- [x] **`TcHeroGeometryTest`** composes the hero in the shape that broke —
      content-driven height inside a vertical scroll — and asserts two things:
      the hero is the height its own content asked for, and the backdrop is
      exactly the hero. Three cases: placeholder, photograph, fixed height.
      Runs on the JVM under Robolectric, inside `testDebugUnitTest` — no
      device, no extra command. It reached only half the component when first
      written (see below).
- [x] **A source rule in `tools/check_repo.py`** fails the build if any
      composable whose name mentions a hero sizes a layer with `fillMaxSize`.
      It catches the class before it is written: the token file already names
      `TcCityHero` and `TcAttractionHero` as components still to come.
- [x] **Both were corrected after review, and the corrections matter more than
      the guards.** The test composed `TcHero`, which in this build can only
      reach the placeholder branch — no photograph ships — so reintroducing the
      defect on the `Image` left it green; the source rule caught that one,
      which is why having two was worth it. `TcHero` now delegates to an
      `internal TcHeroWith` taking the photograph already resolved, and a
      second case measures it. Two assertions had to be discarded on the way:
      "at least the declared minimum" let a broken `Image` through, because it
      measures to its painter's aspect ratio (320dp past a 200dp floor), and
      plain equality passed too, because a `fillMaxSize` backdrop still sizes
      its parent, so hero and backdrop inflated together. The rule had a hole
      of its own: it matched names *ending* in "Hero", so extracting
      `TcHeroWith` took the layering out of its reach until it was widened.
- [ ] **A screenshot suite is still not built**, and remains the right call:
      golden images to maintain and a rendering backend to produce them, for
      screens a human pass reads in an afternoon. The visual pass is the net
      for everything these two do not cover.

The cost of the two is one unit-test dependency (Robolectric) and
`unitTests.isIncludeAndroidResources`. No new module, no instrumented source
set, no second command.

**Not observed, and would want a real device:** anything about the *display*
rather than the layout — colour rendering on OLED, the actual legibility of
the placeholder stripes and of ink-on-paper contrast in daylight, and how the
Fraunces fallback reads at arm's length. Emulator screenshots settle geometry
and hex values; they do not settle how a screen looks in the hand. Nothing in
this pass claims otherwise.

## Phase 5 — Screen 12, Gravar memória por voz

**Verified on hardware.** Samsung SM-S921B (Galaxy S24), Android 16 (API 36),
over ADB on 2026-09-04, plus both emulators for layout.

- [x] Screen 12 with the sheet's three states in one place: the 82dp oxblood
      button with its 28dp core, the pulsing "Gravando" chip over a 38px
      timer, eight waveform bars, attribution, Pausar / Concluir / Cancelar,
      and the green "Memória salva" confirmation with place, hour and duration
- [x] "Memórias desta viagem" — the list of past recordings, newest first,
      with date, author and duration
- [x] No text field anywhere, and no title asked for. A row is named by where
      it happened, which is something the app filled in by itself
- [x] Audio at `files/memories/<id>.m4a`, metadata in Room (brief §22, D057)
- [x] A finished recording is never lost: past `stop()` the audio is on disk
      and no failure in the bookkeeping throws it away — a row that cannot be
      written is reported as "A gravação foi salva no aparelho, mas não entrou
      na lista desta viagem", not as nothing having happened
- [x] `RECORD_AUDIO` asked for immediately before the first recording, never
      at launch (brief §23). A refusal costs the recording and not the screen
      (D030). The permission was declared in Phase 0 and unused until now, so
      the manifest does not change
- [x] The audioguide pauses for the microphone and comes back afterwards
      (D058), locally — nothing is published to the group
- [x] Recording is application-scoped, like playback and Walk Mode: something
      that cannot be recorded again must not belong to a screen

### Confirmed by observation

On the Galaxy S24, with the guide playing before the recording started:

- **the guide gives way and comes back**: `PLAYING position=448873` before,
  `PAUSED(2) position=466611` during, `PLAYING position=469572` after;
- **the microphone actually captures**. A 30.8s recording came back as mono
  AAC, 44.1kHz, **97.8 kbps** against the 96 kbps asked for, 376 812 bytes —
  and, decoded, **mean −45.8 dBFS with peaks at −22.1 dBFS**, which is a quiet
  room's floor rather than the −∞ of a dead microphone path;
- **the memory survives the process**: force-stopped and reopened, the list
  still read "Sarajevo · 4 de setembro · Vinícius · 00:31".

On both emulators (360 × 760 dp with the system font at 1.5, and
480 × 1040 dp):

- the whole flow — idle, recording, pause, resume, finish, saved, list;
- **pausing really pauses**: the timer held at 00:16 across four seconds and
  then continued, which is what the banked-time machine exists for;
- **a refused microphone leaves the screen standing**, with the sentence about
  device settings and the record button still there;
- the audio file lands where §22 says, and its size matches the bitrate
  (263 101 bytes for 21s).

### Not observed, and not claimed

- **whether a person speaking at arm's length is intelligible.** Nobody spoke
  into the phone — the level test proves the microphone path is live and the
  encoder is producing real audio, and it proves nothing about a voice. This
  is the one claim in the phase that only a person can settle, by recording a
  memory and listening to it;
- **a Bluetooth headset connected while recording** — no headset was paired to
  the device during the session;
- **audio focus taken mid-recording by an incoming call.** Phase 2 verified
  the two playback cases; the recording case was not exercised, because
  placing a real call to the traveller's own phone was out of proportion to
  the session.

### Not implemented, with reasons

- [ ] **A recording interrupted by process death is lost.** The invariant is
      about a *finished* recording (§22), and that one holds. A recording still
      running when the process is killed leaves an unfinalized file and no row.
      Recovering it would mean writing the row before the audio exists and
      reconciling on the next launch, which is a design of its own;
- [ ] **Memories cannot be played back in the app.** The approved sheet's list
      carries title, date, author and duration and does not draw a play
      control, and screen 12's job is to record. Nothing was invented;
- [ ] **Screens 10 and 11 remain out of scope**, so the canonical 11 → 12 flow
      is still not walkable end to end. Screen 12 is reached from the "Gravar
      memória" shortcut on screen 02 (D058).

## The operational half — screens 13 and 14 (2026-09-04)

The editorial half of the app (05, 06, 07, 08, 09, 12) was finished and well
tested; the operational half did not exist. This is its first block.

**Verified on both emulators** (360 × 760 dp with the system font at 1.5, and
480 × 1040 dp). No part of it needed the Galaxy S24, and the one thing that
does need hardware could not be reached — see below.

- [x] Screen 13 Carteira: every document, grouped Hoje / Transporte da viagem /
      Seguro e documentos, with the count and the "Tudo offline" pill in the
      header, 48dp rows carrying icon, name, operational detail and the
      booking status the trip's own transport or stay declares
- [x] Screen 14 Documento, ficha mode: the ticket drawn as a ticket from trip
      data — 28px times at both ends, passengers, platform, locator in tabular
      numerals, price when the package has one — with the dashed perforation
      and the redundancy line about the driver taking the locator
- [x] Screen 14 QR mode: white, full screen, 300dp code, locator at 20px, the
      one-line summary and "Brilho no máximo · tela não apaga"
- [x] Grouping as a pure function over content and a date, reusing
      `TripContent.dayFor` rather than growing a second current-day rule
- [x] `Routes.WALLET` and `Routes.DOCUMENT` filled in; no new destinations
- [x] Nothing touches the network and no document leaves the phone (§17)
- [x] The document repository is a resolution step in front of the screens, so
      encrypted storage and a biometric gate would be a third case behind it
      without a screen changing (§17)

### Confirmed by observation

- **the header does not lie.** The package's two documents both declare
  `availableOffline` with no file in the build — the D013/D014 warning that
  `validate_trip` has printed since Phase 1 — and the wallet renders "2
  documentos" with **no "Tudo offline" pill**, each row saying "arquivo não
  está neste aparelho" for itself;
- **booking status comes from the package**: "Reservado" on the bus ticket
  (its transport is `reserved`) and "Pago" on the voucher (its stay is `paid`);
- **screen 14 stays useful without the file**: the amber note names the
  missing file and the ficha below it still reads 19:30 → 22:00, Vinícius ·
  Érika, "A confirmar", MOCK-ABC123;
- **a layout break found and fixed at 360 dp with the font at 1.5**:
  "PLATAFORMA" wrapped mid-word into "PLATAFOR / MA". The two fields now share
  the row and the label gives way before the value does, which is D055's rule
  applied where it had not been yet.

### Found by reasoning, not in the field

- **`embedded` and `generated-from-text` had been collapsed into one string**,
  so an embedded code would have had its *asset path* encoded into a QR. Found
  while working out what could be verified, before anything rendered it (D061).

### Not observed, and not claimed

- **No camera has read a QR from this app.** With this package there is no
  code to read: the only `generated-from-text` document holds `MOCK-ABC123` in
  a trip marked `isMockContent`, and §18 forbids a placeholder code, so the
  generator correctly declines. The verification is **blocked by content, not
  by a device**, and it returns the day real ticket data does (D061);
- **brightness and keep-awake were not watched on hardware**, for the same
  reason: QR mode is unreachable while there is no code, so the screen that
  raises them cannot be opened on a phone. What is proved instead is a test
  that takes both and gives them back, and that fails when the restoration is
  removed (D062). That is not the same as watching a real screen brighten;
- **no PDF was rendered**, because no PDF is packaged. The approved screen 14
  draws a ficha from trip data, not a page.

### Not implemented, with reasons

- [ ] **PDF rendering.** `PdfRenderer` is in the framework and needs no
      dependency, but with no packaged PDF in the build there is nothing to
      render and nothing to verify, and writing an unexercised viewer is the
      speculative code this repository avoids. The resolution step in front of
      the screens is where it goes when a file arrives;
- [ ] **`sensitive` is carried by the schema and does nothing yet.** §17 asks
      only that the repository not close the door on encrypted storage and
      biometric gating, and it does not;
- [ ] **The icon set has no bed and no shield**, so a document that is not a
      ticket shows the wallet's icon. Deciding what those look like is design
      work, not this commit's;
- [ ] **Screens 15, 16, 17 and 18 — Transporte, Hospedagem, Emergência and
      Plano B — are the second block of the operational half** and are not
      started. Screen 04 and the chapter list come after them.

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
                                    ↓
                    06 Iniciar passeio  →  07 Passeio ativo
                                                  ↓
                              08 Sincronizando  →  09 Ouvindo juntos
```

Destinations that belong to later phases (Dia completo, Documento/QR, Plano B,
Audioguia, Memória, and screens 10 and 11) exist in the navigation graph and
open a screen that names the canonical screen and its phase.

## Content pipeline (infrastructure only)

Isolated from the Android phases: nothing below changes what the app does or
which phase it is in.

- [x] Schema **1.1** — every local time names its IANA zone (D026). `city`,
      `day` and both transport endpoints require `timeZone`; a timeline item
      may override its day. Projected into `TripModels.kt`; the three packaged
      trips are migrated and stay on Sarajevo/Mostar content unchanged.
- [x] `trip-package/source/` (`itinerary/`, `user-notes/`, `private/`),
      `generated/` and `production/` exist as the pipeline skeleton.
      `production/` and `source/private/` are ignored except for the one file
      that keeps each folder in the repository (D027).
- [x] The authoring documents are integrated — `content/` (generator, style
      guide, fact-check rules, package spec, templates) and
      `tools/content_preflight.py` are in the repository and required by
      `tools/check_repo.py`. `content_preflight.py` shares its IANA time-zone
      check with `validate_trip.py` (`known_timezones()`) instead of
      duplicating it.
- [ ] No content has been generated, nothing has been promoted, and the
      runtime still reads only `app/src/main/assets/trip/` — integrating the
      authoring workflow and its checks did not change where the app loads
      content from.

## Content validation

`tools/validate_trip.py` now also checks what JSON Schema cannot: unresolved
references, duplicate IDs, `dayNumber` against the trip window, and documents
that declare `availableOffline` without a packaged file (D014). These are
warnings while `metadata.contentStatus` is prototype/draft and errors once it
is `production`. Every declared time zone against the tz database (D026) is
checked separately and always fails, prototype/draft included — a wrong zone
is a real defect regardless of how finished the package is. A missing tz
database is not a warning either: the validator stops (exit 2).
Zone names that resolve but carry no DST rule — anything under `Etc/`, plus
`UTC`, `GMT`, `Zulu` and the other fixed-offset aliases — are rejected too:
they are in the tz database, so the "unknown name" check let them through,
and they are exactly what naming the zone was meant to avoid.
`tools/content_preflight.py` runs the schema validation and the zone check
through `validate_trip.py` itself (`schema_errors`, `timezone_problem`), so it
cannot pass a package that `validate_trip.py` would reject, and a package
declaring no zone at all is an error rather than a quiet pass.

Currently reported for the packaged trip: the two ticket/voucher PDFs are
declared offline but their files are not packaged yet. The UI reflects this —
it does not badge them "Offline" (D013).

## Verification

`python tools/check_repo.py` · `python tools/validate_trip.py` ·
`python tools/content_preflight.py <package> --allow-incomplete-authoring-files`
(no generated package exists yet, so this is exercised against the runtime
and starter trips) · `python -m unittest tools/test_validate_trip.py` ·
`./gradlew testDebugUnitTest assembleDebug assembleRelease lintDebug`

Unit tests: 206 passing. Lint: 0 errors, and no lint baseline is used. The
warnings are dependency-hygiene notices only (`GradleDependency`,
`UseTomlInstead`, `NewerVersionAvailable` and the like); their count moves
with what has been published upstream since the last run, so no number is
promised here. Instrumented tests: none written. Real-device testing is manual and
is recorded per phase; Phase 4's was re-done on two devices on 2026-09-04.

`git diff --check` reports trailing whitespace inside `content/templates/`.
Those are Markdown hard line breaks on the fill-in label lines, where dropping
them would run the labels together into one paragraph; they are deliberate.
