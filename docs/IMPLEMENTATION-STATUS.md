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
- [x] **Memories cannot be played back in the app.** *(Closed: a design
      handoff for the list arrived and playing, sharing and deleting are built
      - see below. Nothing was invented before it did.)*
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
- [x] **The icons that were missing are converted.** The claim that this was
      pending design work was wrong: the approved sprite already carries
      `tc-bed`, `tc-shield`, `tc-call`, `tc-document`, `tc-qr` and
      `tc-arrow-right`, and Phase 1 established how a sprite icon becomes an
      `ImageVector`. There was no decision outstanding, only a conversion. All
      six are converted, and the wallet's voucher row shows the bed rather
      than the wallet;
- [x] **Screens 15, 16, 17 and 18 — Transporte, Hospedagem, Emergência and
      Plano B — are the second block of the operational half** and are the
      section below. Screen 04 and the chapter list come after them.

## The operational half closes — screens 15, 16, 17 and 18 (2026-09-04)

Second and last block of the operational half, in two commits: 15 + 16
(operações) and 17 + 18 (segurança e recuperação). With this the app has
fifteen of its nineteen canonical screens.

**Verified on both emulators** (360 × 760 dp with the system font at 1.5, and
480 × 1040 dp). The Galaxy S24 was **not** asked for — see what that costs,
below.

- [x] Screen 15 Transporte: critical item at the top with the instruction
      apart from the departure time, *Mostrar passagem* and *Ver ponto de
      embarque*, the vertical journey with 28px times and full station names,
      the duration / people / price footer, the action list ending in the
      operator's telephone, and the Plan B summary linking to screen 18
- [x] Screen 16 Hospedagem: hero and name, "Pago", check-in and check-out
      cards, its own critical item (reception closes at 23:00, act by 22:45),
      the offline voucher and Maps actions, and the host's instructions as
      running text
- [x] Screen 17 Emergência: high contrast, **no bottom bar**, no editorial
      hierarchy, the location in plain text, one 88dp "Ligar 112" with the
      note that it works without credit, police and ambulance as two large
      buttons, three 48dp contacts each with its own accessibility label, and
      the ink card in Bosnian to show a stranger with the translation below
- [x] Screen 18 Plano B: the scenario in human language, the reassurance that
      nobody sleeps in the street, the steps numbered in order of attempt with
      what to expect, and the alternatives the package has already saved
- [x] `ActionWindow` and the current Plan B step are pure functions over a
      fixed clock in `domain/operations` — no Compose, no Android
- [x] Every telephone goes out as `ACTION_DIAL`; no permission was added

### Confirmed by observation

- **the dialer opens with the right number and stops there.** "Ligar 112" was
  pressed once on the emulator: the system dialer came up with `112` filled in
  and the call button untouched. **No call was completed, on any device, to
  112 or to anything beside it** — and `ACTION_DIAL` is what makes that a
  property of the app rather than of the tester's restraint (D067);
- **the withheld rows are rows, not buttons.** Insurance, hotel and consulate
  each draw with "O número chega com os dados reais da viagem." and reach no
  dialer, while 112 / 122 / 124 stay live in the same mock package (D064);
- **screen 17 fits without a bottom bar** — the whole screen at 480 dp, and
  scrolled at 360 dp with the font at 1.5 — and nothing sits under the system
  bar at either size;
- **screen 18 reads calm and in order**: step 1 marked as current in teal,
  2 and 3 neutral, the withheld host telephone inside step 2, and "JÁ
  GUARDADO" carrying the bus ticket the package actually holds.

### Found by reasoning, not in the field

- **`currentStepIndex` compared steps by value**, so a plan that repeated an
  identical step would have marked the first of them current whichever one was
  reached. It computes the index directly now. No packaged plan repeats a
  step, so nothing on screen was ever wrong.
- **Screen 17 took the first emergency profile in the package**, not the one
  for the country the traveller is in — so a trip crossing a border would have
  printed one country's name beside another country's city and dialled the
  wrong police. Found by review. The profile is now looked up by the current
  city's country and the country name comes from the city as well; the test
  whose name claimed the invariant was passing on a one-profile fixture, and
  both test files now carry a second country (D070).

### Not observed, and not claimed

- **Screen 17's contrast was seen only on an emulator.** Contrast on OLED in
  strong daylight is what decides whether that screen works at the moment it
  matters, and an emulator cannot answer it. The Galaxy S24 was not requested,
  so this stays open and is not described as verified;
- **no emergency call was placed and none will be.** What is asserted instead
  is the number handed to the launcher, in a Robolectric test that presses the
  real button (D067);
- **the withheld numbers have never been dialled**, because there are none to
  dial: the package ships `+000000000` and `+387000000000`.

### Not implemented, with reasons

- [ ] **Walking distance at both ends of the leg** (screen 15). The schema
      carries station names and platform and has no field for it. Left out
      rather than invented; `trip.schema.json` was not touched (D069);
- [ ] **The insurance policy number** (screen 17). `emergencyContact` has
      `label`, `phone` and `note`, and that `note` is an authoring instruction
      rather than a line for a traveller, so the row carries the label alone
      (D069);
- [ ] **Deadlines on the Plan B steps** (screen 18). Here the schema is not
      the gap — `planBStep.deadline` exists and the packaged plan declares
      none, so the steps draw without one and the first is current because
      nothing has expired (D069);
- [ ] **Screen 19 Mais**, whose list is screen 17's canonical way in. Until it
      exists the "Mais" placeholder carries an "Emergência" row, provisionally
      (D068);
- [ ] **What remains of the app**: screens 03 (dia completo), 04, 10, 11 and
      19, and the chapter list. The editorial and operational halves are both
      complete.

## Closing the bottom navigation — screens 03 and 19 (2026-09-04)

Three of the five tabs gave a placeholder; this closes two of them. Screen 04
follows in its own commit — it is the densest screen in the app.

**Verified on both emulators** (360 × 760 dp with the system font at 1.5, and
480 × 1040 dp). No hardware was needed and none was asked for.

- [x] Screen 03 Dia completo: date, "Dia 9 de 21 · Sarajevo", back to Hoje,
      48dp arrows for the previous and next day, the critical item first and
      summarised with the same two buttons Today gives it, then Manhã / Tarde
      / Noite carrying Today's own timeline rows, the end-of-day transport and
      stay cards, and a footer of this day's documents and its Plan B
- [x] Screen 19 Mais: Emergência at the top in oxblood at 68dp, then Na
      estrada, Grupo and Viagem e aparelho — including what is really saved on
      the device and how much room it takes, and "Trocar quem é você"
- [x] The period split, the day arrows and the choice of critical item are
      pure functions in `domain/` over a fixed date and clock
- [x] Today's timeline row, critical card and shortcut row are **reused where
      they stand** rather than copied or moved: screen 03 is the same day with
      another reading, not a second model

### Confirmed by observation

- **the critical card's second button was 6px wide** at 360 dp with the font
  at 1.5 — "Ver ponto de embarque" squeezed to one character per line. The
  defect was already known and already fixed on screen 15; screen 02's copy
  had been left alone because it was passing, and reusing it on 03 is what
  surfaced it. Both screens wrap now (D072);
- **the day arrows are dead ends at the edges of the package**: this trip has
  one packaged day, so both are drawn and dimmed rather than clamping back
  onto the same day;
- **"Conteúdo salvo" reports 5,8 MB in one audio file**, which is exactly what
  this build carries — no photography, no PDFs. The same rule as the wallet's
  badge (D013) one level up: only files that are really there are counted;
- **the Grupo section reads Offline for both travellers** with screen 09
  unopened, and the sentence about the group finding itself again is what
  carries the meaning (D071);
- **screen 16 is reachable now.** The day declares an accommodation even
  though none appears on its timeline, so screen 03's "Ver hospedagem" opens
  it — the gap recorded in D065 closes without the package changing.

### Found by reasoning, not in the field

- **The sheet for screen 03 names the wrong screen numbers** — (12) for
  transport, (13) for stay, 10 for documents, 17 for Plan B — which contradict
  the canonical index the same document uses in its own headings. Routed by
  name instead, and recorded rather than silently corrected (D073).

### Not implemented, with reasons

- [ ] **"Frases úteis" in Na estrada.** The schema has no phrase list; the
      only phrase in the package is the sentence screen 17 shows a stranger,
      which is not a phrasebook. Left out rather than invented, and
      `trip.schema.json` was not touched — the same treatment as D069;
- [ ] **Live group state on screen 19.** By decision, not omission (D071).

## Screen 04 — Cidade, and the chapter list (2026-09-04)

The last of the three placeholder tabs. With it the editorial layer is
complete and **only screens 10 and 11 remain of the nineteen canonical
screens** — seventeen exist.

**Verified on both emulators** (360 × 760 dp with the system font at 1.5, and
480 × 1040 dp). No hardware was needed and none was asked for.

- [x] Hero of 300px with the country, the name and the stretch of the trip
      spent in the city
- [x] Editorial opening, with no operational number in it — asserted, not
      just intended
- [x] Teal city-guide card with a 46dp play, and **the numbered chapter list
      with each chapter's length**, which is where D025 lands
- [x] Horizontal attraction cards with thumbnail, the hour the itinerary gives
      them, and status pills
- [x] Ink card for the day's walk: distance, duration, how many stories
- [x] Onde comer, with walking distance and the practical note
- [x] Short stories with *Ouvir* and *Ler*

### Confirmed by observation

- **the hero's text was nearly invisible** on the paper placeholder — the
  country and the dates in light grey-blue on light beige. Screen 05 had
  already solved this with the cool placeholder and white at 85%; the city
  hero and the attraction thumbnails now do the same (D075);
- **the pills wrap in the real package**: Baščaršija carries "Audioguia",
  "Offline" and "Entrada livre", and the third takes its own line at 360 dp
  rather than being squeezed;
- **the chapter list opens and each row carries its length** — "1 Introdução
  · 6 min" — and the guide's line reads "3 capítulos · 34 min · ainda não
  salvo neste aparelho", because that file is not in this build;
- **only the story that has audio offers *Ouvir***;
- **the restaurant row was fixed at 360 dp with the font at 1.5**: the name
  takes two lines there and a vertically centred distance floated into the
  middle of them. Top-aligned now.

### Found by reasoning, not in the field

- **A chapter picked while another guide is loaded would have sought inside
  that other recording.** Picking a chapter now loads the city guide and
  starts there (D074).

### Not observed, and not claimed

- **No chapter was heard.** The city guide's audio is not packaged, so
  pressing a chapter row asks the player for a guide it cannot load — the
  card says so ("ainda não salvo neste aparelho"). What is proved is the
  index handed to `seekToChapter`, in a Robolectric test that presses the real
  row (D074). Hearing chapter three start at 6:00 waits for the audio;
- **the attraction thumbnails and the hero have never drawn a photograph**,
  because none is packaged (D005 remains open).

### Not implemented, with reasons

- [ ] **The "10 KM" pill the sheet shows on attraction cards.** Distance from
      where — the city has no centre point in the schema, and an attraction's
      distance from the traveller is a live location, which this screen is
      explicitly not about. Left out rather than invented;
- [ ] **Fraunces** (D005): the name, the chapter titles and the story titles
      use the serif fallback;
- [ ] **A test for D055 on the attraction pills.** One was written and then
      deleted: it passed with the wrap and passed without it, so it proved
      nothing (D076). The wrap is verified on the emulator instead.

## The first-element class — screens 17 and 12 (2026-09-04)

D070 fixed one occurrence of `content.trip.<collection>.first()` where a
per-day lookup existed. A sweep for the same shape found two more, both in
code that had already been reviewed and approved.

- [x] **Screen 17's hotel contact** took the first accommodation in the
      package instead of the one the day declares — the wrong number from the
      second night onwards, on the emergency screen. It now reads
      `day.accommodationIds`, the mechanism screen 03 already used
- [x] **Screen 12's voice memory** attributed recordings to the first city in
      the package. This one is not a screen state: it goes into the `Memory`
      and into Room, so it would have been wrong permanently. `buildAttribution`
      takes a `date` now and resolves the city through `content.dayFor`

### Found by reasoning, not in the field

- **Both, by sweeping for the shape D070 exposed.** Neither is reachable with
  the packaged trip, which is exactly why they survived review: it ships one
  city, one stay and one day, so "the first element" and "the element for
  today" are the same answer and no test could tell the two implementations
  apart.

### Confirmed by observation

- **the two new assertions fail against the old code**, each with the
  neighbouring city's name — `expected:<Hospedagem em [Mostar] — exemplo> but
  was:<Hospedagem em [Sarajevo] — exemplo>` and `expected:<[Mostar]> but
  was:<[Sarajevo]>`. Run before the fix, as with D070.

### Not implemented, with reasons

- [ ] **A `check_repo` rule for this class.** Proposed to the reviewer rather
      than written: the guard was asked for as a proposal (D077).

## The last two screens - 10 and 11 (2026-09-04)

**The nineteen canonical screens exist.** This is the first time that can be
said.

**Verified on both emulators** (360 x 760 dp with the system font at 1.5, and
480 x 1040 dp). The Galaxy S24 was not asked for - see below.

- [x] Screen 10 Historia disparada pela localizacao: the walk still visible
      above and dimmed, the editorial sheet rising from the bottom on a light
      ground, the "Historia pelo caminho" pill, the title at 32px, the
      paragraph, a separate operational line with distance, exact place and
      audio length, *Ouvir agora* / *Ler* / *Depois*, and the footnote about
      the chime in the headphones
- [x] Screen 11 Fim do passeio: "Passeio concluido", the title at 36px, where
      and when it ended, four summary cards, **Ainda hoje** with the critical
      item in oxblood and its instruction, and *Gravar memoria* / *Voltar
      para Hoje*
- [x] The canonical **11 -> 12 -> 02** path exists. Screen 02's shortcut into
      12 stays: it is a legitimate entry and already tested (D079)
- [x] Reused rather than rebuilt: the S19 machine, `decideStoryTrigger`,
      `WalkModeController`, `StoryTriggerStore`'s `playedAt`, Today's timeline
      and `PlaybackController`

### Confirmed by observation

- **the offered story, on both emulators**, reached through a second debug
  scaffold that turns automatic stories off and then arrives through the same
  `onLocation` seam (D031, D078). *Ler* opened the full story in place,
  *Depois* closed the sheet and the walk carried on at the next stop;
- **a defect found by looking**: the sheet drew its own copy of the walk's
  progress line on top of the identical line screen 07 was already showing -
  "1 de 2 22 min restantes" overprinted, both illegible. The sheet carries no
  copy now; the walk above it is screen 07, dimmed by the scrim;
- **screen 11 after a real walk**: "Terminou em Sarajevo, as 14:45", the four
  cards, the 19:30 bus in oxblood with "Esteja na estacao ate 19:00", and
  *Gravar memoria* opening screen 12 with "Sarajevo - 14:46" - then back to
  Today, which is the canonical flow end to end;
- **"Historias ouvidas" reads 0 de 2** with this package, correctly: one story
  of that walk has no guide at all and the other's audio file is not in this
  build, so no audio ever started (D021, S20).

### Found by reasoning, not in the field

- **The critical item could have been listed twice on screen 11** - once as
  the oxblood card and once as a row of "Ainda hoje" - on any day whose next
  commitment is the critical one. It is excluded from the rows beneath it now.

### Not observed, and not claimed

- **No story has fired from a real GPS fix.** Every arrival in this pass came
  from the debug scaffold feeding the story's own packaged coordinates through
  `onLocation` - the real decision, not a real walk. Standing in Bascarsija is
  the only thing that would prove the rest, and it is not a device question;
- **screen 10's offered case is not reachable in the running app** with this
  package: every packaged story declares `autoPlayInWalk` and the walk
  declares `automaticStoriesDefault`, so a real arrival always plays. A
  content gap the schema already lets a package close, not a design one
  (D078);
- **no audio was heard on either screen**, for the reason above.

### Not implemented, with reasons

- [ ] **A measured walking distance on screen 11.** Nothing tracks it; the
      card shows the route's declared length. An odometer would be a
      subsystem, not a field (D079);
- [ ] **`WalkModeController.pause()` still has no caller.** Neither screen 10
      nor 11 pauses a walk - 10 lets it carry on by design, and 11 only ever
      sees a walk that has already ended. The item stays open exactly as it
      was, since Phase 3.

### What the nineteen screens unlock, and is not done

- [ ] **The second full visual pass**, this time over nineteen screens rather
      than the eight the first one covered;
- [ ] **The first end-to-end run of the canonical flow**, which has never been
      walked whole because it has never been whole.

## The fourth first-element, and a rule (2026-09-04)

- [x] **Screen 12's context line** read the first city in the package while the
      attribution beneath it read the city of the day, so the same screen named
      two places seconds apart. Both come from the same answer now
- [x] **`check_repo` grew a rule for the class.** In `domain/**`,
      `feature/**State.kt` and `feature/**UseCase.kt`, a packaged collection's
      first element read with no predicate has to carry `// fallback:` and a
      reason. Three deliberate reads exist and now say why

### Confirmed by observation

- **the rule was made to fail before it was kept**, against the line that
  prompted it:

      FAIL: a packaged collection's first element is read with no predicate
            and no reason...
      - app/.../feature/memory/MemoryState.kt:63: val city = content.trip.cities.firstOrNull()?.name

### Not implemented, with reasons

- [ ] **The rule does not look at `data/`**, where resolving a collection by
      identity is the job, and it does not try to judge a first element - only
      to insist that somebody said which (D080).

## Screen 12's list learns to play, share and delete (2026-09-04)

A design handoff arrived for the list under the recorder, and is versioned at
`docs/design/prototype/handoff-12-memorias/`. Nothing above the list changed.

**Verified on both emulators** for layout (360 x 760 dp with the system font at
1.5, and 480 x 1040 dp) **and on the Galaxy S24** for the three things an
emulator cannot close.

- [x] Playing a memory from its row, with the 5dp progress bar inside the same
      card, indented 34dp to line up with the title
- [x] Sharing through the system sheet: `ACTION_SEND`, audio MIME, a
      `FileProvider` that exposes `files/memories/` and nothing else
- [x] Deleting with a named confirmation - "Apagar" and "Manter", never "OK"
      and "Cancelar" - which removes the row and the file
- [x] The empty state, which is what this build actually shows until somebody
      records: the reference's two example memories are the reference's
- [x] A memory never touches `PlaybackController`, the media session, the
      media notification or the lock screen (D081)

### Confirmed by observation - Galaxy S24

- **the audio really comes out**: `dumpsys audio` shows
  `type:android.media.MediaPlayer ... state:started ... sampleRate=44100` for
  this app while the row's bar advances;
- **and it stays out of the media session**: `dumpsys media_session` lists no
  session for this app while a memory plays, so nothing reaches the lock
  screen (D081);
- **the real share sheet** shows "Sarajevo.m4a" - the memory's own name, not
  its id - and the system's own direct-share row put **"Erika - WhatsApp"
  first**, with no direct-share target, sharing shortcut or `ShortcutManager`
  registered by this app. That answers the handoff's open question: the tile
  stays out of the delivery and the system supplies the person anyway (D082);
- **the confirmation line**: choosing a destination - Bluetooth, whose transfer
  was then cancelled at the device picker - left "Enviada para Bluetooth" in
  green with a tick on that row, and the file untouched;
- **the destructive dialog**: tapping outside left both files on disk;
  confirming removed the row and the file, and only that one - the memory the
  traveller had already recorded on the device was still there afterwards.

### Confirmed by observation - emulators

- **a layout fix at 360 dp with the font at 1.5**: the duration as a third
  column left the date reading "4 de se...". It moved into the metadata line,
  which is D055's rule - the qualifier gives way, not the identity;
- **the empty state** reads "Nenhuma memoria gravada nesta viagem." before
  anything is recorded;
- **back and tapping outside both mean "Manter"**.

### Not implemented, with reasons

- [ ] **No undo after deleting.** Asked and answered: the dialog says the
      recording cannot be recovered, and that is true. A snackbar would make
      the sentence a half-truth;
- [ ] **No "Erika" tile drawn by this app.** The handoff's own fallback, and
      the system does it better (D082);
- [ ] **No share icon of our own.** The approved sprite has none; `tc-near-me`
      stands in, as the handoff proposes, and the note for design stands.

## The field pass - nineteen screens, end to end (2026-09-04)

The first time the product was walked rather than the code tested. The first
visual pass covered eight screens; eleven had never been compared with the
prototype, and the canonical flow had never been walked whole because it had
never been whole.

**On the Galaxy S24, with Bluetooth headphones**, for what only hardware
answers. **On both emulators** (360 x 760 dp with the system font at 1.5, and
480 x 1040 dp) for layout and for colour - see D086 for why colour cannot be
sampled on the phone.

### Reachability map - what actually opens, with this package

| # | Screen | Reached | How, or what is missing |
| --- | --- | --- | --- |
| 01 | Quem é você | yes | 19 -> "Trocar quem é você" |
| 02 | Hoje | yes | root tab |
| 03 | Dia completo | yes | "Viagem" tab, and 02 -> "Ver dia completo" |
| 04 | Cidade | yes | "Explorar" tab |
| 05 | Atração | yes | 02 -> "Ver atração" |
| 06 | Iniciar passeio | yes | 05 -> "Iniciar passeio" |
| 07 | Passeio ativo | yes | 06 -> "Começar passeio" |
| 08 | Participantes sincronizados | **no** | a three-second transition that only happens when a *shared* listen starts: needs a second phone in the group and a reachable backend. Verified with two devices in Phase 4; not this session |
| 09 | Ouvir juntos | partly | 07 -> "Ouvir juntos" opens it, and it reads "Esperando o grupo". Sincronizado, âmbar and divergente need a peer |
| 10 | História por localização | **no**, in the running app | every packaged story declares `autoPlayInWalk` and the walk declares `automaticStoriesDefault`, so a real arrival always plays and the *offered* case never happens. Seen through the debug scaffold, which turns automatic stories off and arrives through the real `onLocation` (D031, D078). **Content:** one story with `trigger.autoPlayInWalk: false`, or `walk.automaticStoriesDefault: false` |
| 11 | Fim do passeio | yes | 07 -> "Encerrar passeio" |
| 12 | Gravar memória | yes | 11 -> "Gravar memória", and 02's shortcut |
| 13 | Carteira | yes | root tab |
| 14 | Documento, ficha | yes | 13 -> a row |
| 14 | Documento, **QR** | **no** | the only `generated-from-text` document holds `MOCK-ABC123` in a package marked `isMockContent`, and §18 forbids a placeholder code (D061, D063). **Content:** real ticket data and `contentStatus: production` |
| 15 | Transporte | yes | 03 -> "Ver transporte", and 02's timeline row |
| 16 | Hospedagem | yes | 03 -> "Ver hospedagem" |
| 17 | Emergência | yes | 19 -> "Emergência" |
| 18 | Plano B | yes | 02's shortcut, 19's list, 15's footer |
| 19 | Mais | yes | root tab |

**Nothing is unreachable for want of code.** Seventeen of the nineteen open
in the running app; the two that do not are waiting on content, and one more
(09's live states) is waiting on a second phone.

One more state is blocked by content rather than by code:

- **11's "Histórias ouvidas" can only ever read 0 de 2**: one story of the
  packaged walk has no guide at all and the other's audio file is not in the
  build. **Content:** `audio.latin-bridge`.

**13's "Tudo offline" pill is no longer blocked.** It was, for as long as the
app read only the sample package, whose two documents declare `availableOffline`
over files that are not in the build. With the real package installed the
Wallet header reads **"27 documentos"** and the pill appears — observed on the
S24 this session. The sample package still cannot show it, and still should
not: D013 is the reason it does not.

**Correction — 07's location states were in that list and do not belong there.**
`Denied` is not blocked by content and never was: it is reached by refusing the
location permission, and it *was* reached that way on a device in Phase 3 — "a
refused location permission leaving the walk and the audio running, with no
foreground service and no GPS request at all" is that state, observed, and it
is where D030 and D032 came from. What is true is narrower and belongs in "Not
observed": no session since has re-exercised it. `Searching` is a third thing
again — transitory, between the permission being granted and the first fix
arriving — and it is short by design rather than blocked by anything.

### Confirmed by observation - Galaxy S24, with headphones

- **"Fones conectados"** on screen 06 - the first time that check has had real
  hardware to report on; every previous run said "Nenhum" on an emulator;
- **the audioguide plays to the Bluetooth headset with the screen off**:
  `AudioTrack ... state:started`, `usage=USAGE_MEDIA`,
  `content=CONTENT_TYPE_SPEECH`, `deviceIds:[50285]` - the headset, not the
  speaker. That is Walk Mode's whole promise, observed;
- **the compact player survives navigation between root tabs**, and moved from
  "capítulo 1" to "capítulo 2" by itself while the traveller was on another
  screen;
- **Walk Mode's foreground service** runs with `types=0x00000008` (location)
  and an `ONGOING|NO_CLEAR|SILENT` notification, `category=navigation`;
- **screen 10 rises over screen 07 dimmed**, with the walk's own progress line
  above it and the guide still playing underneath;
- **a whole loop in airplane mode**: the walk finished, a memory was recorded,
  played back through the headset and deleted, and screen 17 opened the
  dialer - all with the radios off. §3.3 held: nothing local stopped;
- **the dialer opened with 112 filled in, in airplane mode, and was left
  there.** No call was completed, to 112 or to anything else, at any point;
- **the traveller's own memory was not touched**: only the two recordings made
  during this pass were deleted, and `files/memories/` still holds theirs.

### Confirmed by observation - emulators

- **screen 19's emergency row measures 68dp**, screen 07's transport measures
  56 / 76 / 56dp, and screen 12's record button measures 82dp - the numbers
  the sheets declare, measured rather than eyeballed;
- **colour is exact on the emulator**: `#7A2E2E` and `#F5F1E8` sample as the
  tokens themselves;
- **two layout defects at 360dp with the font at 1.5, both fixed** (D085):
  screen 02's "Abrir no Maps" squeezed to 77 x 85dp beside a button that kept
  its line, and screen 05's placeholder caption printed on top of the city
  line. Both re-checked after the fix;
- **the release build still carries no scaffold**: `Protótipo` appears twice
  in the debug DEX and zero times across all four release DEX files - the
  positive control the D031 check needs, re-run now that the scaffold has a
  second entry.

### Found by reasoning, not in the field

- **A device screenshot cannot be compared to a hex token on this phone.** The
  S24 is on Samsung's automatic screen mode, which transforms the frame before
  `screencap` sees it: the emergency row samples `#713331` there and `#7A2E2E`
  on the emulator. Nothing is wrong with the app; the method splits (D086).

### Not observed, and not claimed

- **Screens 08 and 09's live states**, for want of a second phone in the group
  this session. They were verified on two devices in Phase 4 and nothing in
  this cycle touched synchronization;
- **screen 17's contrast in strong daylight.** It was seen on the real OLED
  panel indoors, and it is legible there. Daylight is the question that
  decides that screen and it is still open;
- **story triggering from a real GPS fix.** Every arrival came through the
  debug scaffold feeding the story's own packaged coordinates into the real
  decision. Standing in Baščaršija is what would prove the rest;
- **07's Searching and Denied location states**, and **09's amber and
  diverged notes**;
- **no chapter was heard on screen 04**, and **no QR was scanned**, for the
  content reasons in the map above.

### Not implemented, with reasons

- [ ] **The prototype answers nothing new that this pass had to ask.** No item
      went to the design confirmation stack: both defects had an established
      remedy in this repository already (D066, D072), and nothing else
      diverged from a sheet that draws it.

## Phase 7 — The real trip inside the app

**Verified on hardware, 2026-09-05.** Samsung SM-S921B (Galaxy S24, Android 16)
with the real package installed, plus a Pixel emulator for the days the phone's
clock cannot reach. Eight days before departure.

### What was installed

`trip-package/generated/` → `trip-package/production/` →
`app/src/main/assets/trip-production/`: **`trip.json` and the 27 documents it
declares, and nothing else.** The authoring reports, the asset manifest and
`research/SOURCES.md` stayed out of the APK — the runtime resolves 27 asset
paths and every one of them is a document. Both destinations are ignored by
Git, confirmed with `git check-ignore` and with `git status --short` showing no
new file. `contentStatus` stays `draft`, which nothing in the app reads.

The debug APK is 32.6 MB and the release 27.4 MB, each carrying 28 files under
`assets/trip-production/` and the sample's 3 under `assets/trip/`.

### Found by running it with real content

**Three defects, all fixed in this commit, none of them reachable with the
sample package.** That is the point worth keeping: each needed a *shape* the
one-day, one-city, no-files sample does not have.

- **Screen 03 marked a browsed day "Agora".** Paging to 15 September on
  5 September said the 07:15 flight to Corfu was under way. The guard existed
  and compared a browsed day against itself (D089). Needed a second day to
  browse to;
- **screen 17 named the wrong country on a day that crosses a border.** Day 3
  read "Amsterdã · Países Baixos" and offered the Dutch consulate to someone
  sleeping in Ksamil, above a row saying "Guesthouse em Ksamil" (D090). Needed
  a day that touches more than one city;
- **no packaged document could be opened.** `isPackaged` was computed and no
  screen read it; the Wallet said "Tudo offline" over files with nowhere to go.
  Asked, and built on the answer: "Abrir arquivo" hands the file to a viewer
  through the provider (D091). Needed the files to exist.

### Confirmed by observation — Galaxy S24 unless noted

- **the six time zones read at their own clock.** Day 3: 04:30 and 07:15
  Amsterdam, 11:40 and 14:00 Athens, 16:00 Tirane. Nothing converted;
- **the seven emergency profiles switch by country**, checked on five days in
  five countries — Brazil on day 1 (S24), then Albania, Montenegro, Bosnia and
  Croatia on the emulator, whose clock can be moved and the phone's cannot:
  190/SAMU with **no 112 note**, then 112 + 127, 112 + 122/124, 112 + 122/124,
  112 + 192/194, each with its own consulate. Belgrade answers for Montenegro,
  which is what the Itamaraty page says;
- **the Wallet holds 27 documents and the "Tudo offline" pill appears**,
  grouped by urgency with the LATAM ticket and the parking under HOJE;
- **a real PDF opened**: "Abrir arquivo" on the LATAM ticket materialised
  62 694 bytes into `cache/documents/` and Samsung's reader rendered it at six
  pages. First time in this project;
- **the stay is the day's**: day 17 shows "Estúdio em Čilipi · Check-in a
  partir de 15:00", the value read from that Airbnb voucher;
- **the two stays with no voucher declare themselves**: screen 16 for the
  Bastasi camp draws `00:00` in both cards *and* the host-instructions block
  saying "00:00 é um marcador, não um horário";
- **a whole loop in airplane mode** with both radios down
  (`mVoiceRegState=POWER_OFF`, Wi-Fi disabled): screens 02, 13, 14 and 17, and
  the dialer opened on 190. No crash anywhere in the session's logcat.

### Not observed, and not claimed

- **screens 08 and 09's live states**, for want of a second phone;
- **screen 07's location states**. Untouched this session;
- **the other fifteen days of screen 17**, and the Greek profile: the S24 will
  not let `adb` move its clock (`Operation not permitted`) and the emulator
  images are production builds with no root, so each day cost a pass through
  the Settings date picker. Five countries were sampled rather than all seven;
  **the Netherlands and Greece were not opened on a screen**;
- **the QR mode**, still unreachable and still by decision: all 27 documents
  declare `qr: {mode: none}` because the codes live as images inside the PDFs.
  The files now open, so the printed code is reachable through the reader;
- **colour**, deliberately. D086's split: the S24's screen mode transforms the
  frame before `screencap` sees it.

### Went to the design confirmation stack

- **What screen 17 says about a contact that has no number.** With the sample
  package "O número chega com os dados reais da viagem." was true. With the
  real one it is false: the data arrived and three Airbnb hosts simply publish
  no telephone, so the sentence appears under Sarajevo, Dubrovnik and Čilipi
  promising something that is not coming. Distinguishing "mock package" from
  "real content, no number" is new copy, and the prototype draws neither.

## Phase 8 — The app stops waiting to be opened

**Verified on hardware, 2026-09-05.** Samsung SM-S921B (Galaxy S24, Android 16)
with the real package. Eight days before departure.

The brief's §32 lists thirteen things for V1. This was the one that existed in
no form at all: no `AlarmManager`, no `WorkManager`, and a README naming four
channels of which two were built. The package carries **twelve deadlines** with
an `actionByTime` — the gate at 06:45, the bag drop at 04:45, the ticket
window at 06:50 — and the app knew every one of them and announced none.

### The shape

The decision is a pure function, `domain/alerts/criticalAlerts`, and
`AlarmManager` is handed a list already decided (D092). That is where the risk
is: this is the first thing in the app that **computes** with `day.timeZone`
instead of printing it, and an hour out is invisible on every screen and only
wrong at the gate.

### The twelve, resolved

Run against the real package, each in the zone the content wrote it in:

| Deadline | Local | Zone | UTC |
| --- | --- | --- | --- |
| `ci.anne-frank` | 14/09 15:25 | `Europe/Amsterdam` | 13:25Z |
| `ci.gate-corfu` | 15/09 06:45 | `Europe/Amsterdam` | 04:45Z |
| `ci.checkin-ksamil` | 15/09 15:30 | **`Europe/Tirane`** | 13:30Z |
| `ci.nightbus-ksamil` | 16/09 19:15 | `Europe/Tirane` | 17:15Z |
| `ci.checkin-kotor` | 17/09 20:00 | `Europe/Podgorica` | 18:00Z |
| `ci.bus-zabljak` | 20/09 06:40 | `Europe/Podgorica` | 04:40Z |
| `ci.canyoning` | 22/09 10:15 | `Europe/Podgorica` | 08:15Z |
| `ci.train-mostar` | 26/09 06:50 | `Europe/Sarajevo` | 04:50Z |
| `ci.tour-herzegovina` | 27/09 09:20 | `Europe/Sarajevo` | 07:20Z |
| `ci.bus-dubrovnik` | 28/09 06:30 | `Europe/Sarajevo` | 04:30Z |
| `ci.caiaque` | 28/09 12:45 | **`Europe/Zagreb`** | 10:45Z |
| `ci.bagdrop-dubrovnik` | 30/09 04:45 | `Europe/Zagreb` | 02:45Z |

Two things this table proves. **`ci.checkin-ksamil` is read in Tirane, not in
the day's Amsterdam** — day 3's timeline item declares the override and the
critical item inherits it. And **`ci.checkin-kotor` appears once**, on the
night of arrival, though the package declares it on three days of the stay.

The honest caveat: every zone in this trip except Athens and São Paulo is
+02:00 in September, so those overrides change no instant *in this package*.
The rule is demonstrably applied — the zone column is asserted, not the
instant alone — and the cases where offsets differ are unit tests: Athens
(+03:00) inside an Amsterdam day, and the day-20 crossing to São Paulo, which
is five hours and which the real package has no deadline on.

### Confirmed by observation — Galaxy S24

- **the permission is asked after "Quem é você?", not at launch** — the same
  timing Phase 3 used for location and Phase 5 for the microphone;
- **the first build registered every alarm with a one-hour window.**
  `SCHEDULE_EXACT_ALARM` starts denied on Android 13+, exactly as the decision
  predicted. The app now opens Android's own settings screen for it once, and
  granting it upgraded all thirteen alarms to `window=0` **with no relaunch**,
  through the permission-changed broadcast (D093);
- **an alarm fired at the minute it was set for**, 15:22:08 for 15:22, on
  channel `operational`, `category=reminder`, `BigTextStyle`;
- **the text is the package's**: title "Prova de campo — balcão fecha às
  04:45", body "Esteja no balcão da companhia até 04:45. Saia de Čilipi às
  04:00." Nothing composed;
- **the tap opened screen 15**, the transport the deadline belongs to, with
  the critical card at the top — not the generic Today;
- **the twelve came back after a reboot**, exact, **without the app being
  launched**: `BOOT_COMPLETED` → `BootRescheduleReceiver`, about 85 seconds
  after `sys.boot_completed`.

Deadlines in the future cannot be waited for, and `adb` moves neither the
S24's clock nor the emulator's, so the firing was proven by temporarily
injecting a deadline two minutes out into the installed copy of the package.
The copy was restored from `trip-package/production/trip.json` afterwards and
the two files hash identically.

### Went to the design confirmation stack

- **The evening memory prompt.** The channel is created so it can be switched
  off before it ever speaks, and it posts nothing: what it would say, at what
  hour, and whether it is nightly or only after a walk is copy no sheet draws
  (D094).

## The row that led nowhere, and two checks nobody was running (2026-09-05)

Three defects of different sizes, none of them a new screen: a timeline row
that named a walk and answered nothing, a declared audio duration that no tool
compared against its file, and a guard that could not see the shape of the
fourth defect it was written for.

**Screen 02's 11:00 line opens the walk (D096).** D065 made a timeline row
tappable only when it points at a screen, and then said so in three places - a
set of openable kinds beside the row, and a `when` in each of `TodayRoute` and
`FullDayRoute`. All three listed transport and accommodation; `walk` was in
none of them. Day 9's packaged 11:00 item is `kind: "walk"` with `refId:
"walk.sarajevo.historical"`, so it drew its hour and its marker and did nothing,
while `Routes.WALK` and screen 06 had been there since Phase 3. During the day
the traveller opens **Hoje**, not Explorar - and Explorar -> Cidade -> card was
the only way in. `timelineDestination(kind, refId)` is now the single rule the
row and both routes ask, so the affordance and the destination cannot fall out
of step again. `attraction` deliberately stays out: screen 02's "now" block
already carries "Ver atracao" for the attraction under way.

**The declared audio duration is checked against the file (D095).**
`AudioGuideRequest.Playable.declaredDurationMs` is `durationSeconds * 1000`,
and that number - not the recording - draws the progress bar and writes the
duration label. Nothing compared the two, and nothing looked at chapters at
all: `grep chapter tools/validate_trip.py` returned nothing. The validator now
reads the real length out of the packaged file and checks the declared duration
against it, that no `chapters[].startSeconds` sits at or past the end, and that
the chapter marks increase. Two formats, pure stdlib, no new dependency: the
`mvhd` atom of MP4/m4a, which is what the real guides will be, and WAV, which
is what this repository already carries. **It is an error at every stage**, like
the IANA zone check and unlike the offline-document check - see D095 for the
argument. A format that cannot be timed, or a file not packaged yet, is an
absent check and is reported by name rather than failed.

**The first-element rule reaches lists scoped to a day (D097).** The guard
written after D090 matched `content.trip.<collection>.firstOrNull()` and could
not have caught D090 itself, which arrived as `day.cityIds.firstOrNull()` - a
list already scoped to the day, with no `.trip.` on the line. It now also
accuses `.first()`/`.firstOrNull()` on any property ending in `Ids`. Five
correct sites became offenders and all five were marked rather than rewritten.
`AppNavigation.kt:512` is a sixth of the same shape and is knowingly out of
scope: the scan covers `domain/` plus `*State.kt` and `*UseCase.kt`, and the
navigation graph is none of those.

### Confirmed by observation - Pixel emulator, sample package, clock at 21/09

The build under test was assembled from a clean worktree, so it carries the
sample package and not the promoted one.

- **before the change**, the 11:00 "Caminhada Historica de Sarajevo" row on
  screen 02 answered a tap with nothing at all - not a wrong screen, no screen.
  The 19:30 transport row in the same card opened screen 15 on the same run,
  which is what makes the first observation a defect rather than a missed tap;
- **after**, the same row opens screen 06 with the walk's own header, its route
  and "Comecar passeio";
- **screen 03's copy of the row does the same thing**, from the "Viagem" tab on
  the same day. It reuses `TimelineRow`, so it had inherited both halves of the
  defect and inherits the fix.

### Found by reasoning, not in the field

- **Two more first-element sites than the survey expected.** Widening the rule
  accused `StayState.kt` and `TransportState.kt` reading `documentIds
  .firstOrNull()` for the one voucher and the one ticket each screen has room
  for. Both are correct and both are now marked: the schema declares no primary
  document, so a stay or a leg carrying two shows whichever the author listed
  first. Nobody had written that down anywhere.

### Not implemented, with reasons

- **Walk stop ordering is still unchecked.** §27 asks for it and no tool
  confers a walk's `stops[].order`. It is the check nearest to this commit and
  it is not in it. *(Corrected on 2026-09-05: this bullet first said `order` was
  "read nowhere by the validator", which was true, and then let that stand as if
  nothing read it at all. `WalkModeState.kt:105` sorts the stops by it - see
  D099, which is why the check confers the numbering rather than applying it.)*
- **Coordinates, weather and the two design-stack questions** are unchanged and
  were out of scope by instruction.

## The coordinate that agrees with nothing, and the stop numbered twice (2026-09-05)

The last two of §27's eleven checks. Neither is a screen, and one of them
turned out not to be the check the brief's wording suggested.

**A coordinate is measured against the rest of its own city (D098).** "Invalid
coordinates" had an obvious reading that was already built: the schema bounds
latitude to -90..90 and longitude to -180..180, and schema validation runs
first. The errors that get through are the ones that stay inside the range.
The Latin Bridge is 43.8576 / 18.4289; transposed it is Saudi Arabia, and it
satisfies every rule the schema has. A flipped sign is the South Atlantic. A
transposed integer digit is another country. `city` carries no `geo`, so the
anchor comes from the points themselves, grouped by the `cityId` that
attraction, walk, story and accommodation all declare, and each point is
measured against the **nearest** other point in its city. Over 100 km it is a
finding; when swapping the two values lands within 5 km of that neighbour, the
finding says the values look transposed, which is the difference between an
accusation and an instruction.

**A walk's stop numbering is checked, in two severities (D099).** The status
line here previously said `stops[].order` was read by nothing. That was wrong,
and the correction changes the target: `WalkModeState.kt:105` sorts by it, so a
walk numbered 1, 3, 2 is silently reordered to 1, 2, 3 and runs correctly.
What was missing was the conference. A **duplicate** is an error at every
stage - two stops numbered 2 leave the walking order decided by `sortedBy`
being stable, which is correct by accident. A **gap**, or a first stop that is
not 1, is a warning until `production` - 1, 2, 4 sorts into the right sequence
and runs, and the missing 3 is content that has not arrived yet.

### Proved by failing, on the sample package

Each check was proved by adulterating `trip-package/sample/sample-trip.json`,
reading the accusation, and restoring:

- **transposed coordinate** - `story.latin-bridge`'s trigger with its two
  values swapped: *"story 'story.latin-bridge': trigger.geo (18.4289, 43.8578)
  is 3690.7 km from the nearest other point in city 'sarajevo' (attraction
  'latin-bridge' at 43.8578, 18.4289). Swapping latitude and longitude puts it
  0.0 km from that point - the two values look transposed."* `rc=1`.
- **duplicate stop order** - both stops numbered 1: *"walk
  'walk.sarajevo.historical': 2 stops both declare order 1, so which one the
  traveller walks first depends on the sort being stable"*. `rc=1`.
- **gap in the stop order** - stops numbered 1, 3: reported as a warning with
  `rc=0` while `contentStatus` is prototype, and as an error with `rc=1` with
  the same package promoted to `production`. The severity split is the proof.
- **the coincident pair** - a second point placed exactly on an existing one:
  silent, `rc=0`. That is the regression somebody will cause the day zero
  metres looks suspicious, and it is now a test as well.

### What the packaged trips actually exercise

- `app/src/main/assets/trip/trip.json` and `trip-package/sample/sample-trip.json`
  each hold **four** coordinates, all in `sarajevo`: two attraction locations
  and two story triggers. One cluster is checked and nothing is left
  unanchored - the bus to Mostar declares `location` with only a `name` and a
  `mapsQuery` on both ends, so there is no endpoint coordinate to skip.
- `trip-package/starter/trip.json` has **no coordinates at all**, so the check
  prints nothing for it. That is not a missing output.
- The real package carries no coordinate yet either, and 18 transport
  endpoints and 10 accommodations without one. The check was built before the
  content it guards, on the same reasoning as D095: the same check written next
  month guards a number that has already been wrong for a month.

### Found by reasoning, not in the field

- **The swap test is symmetric, and cannot name the culprit on its own.**
  Transposing either half of a transposed pair lands on the other half, by
  construction, so a city holding exactly two coordinates gives no signal for
  which of them was typed wrong. With three or more the wrong one is the one
  standing alone and is named; with exactly two the finding names both and says
  there is no third point to break the tie. Reporting one finding per *pair*
  rather than per point is the other half of that: a two-point city would
  otherwise accuse the correct point as loudly as the wrong one.

### Not implemented, with reasons

- **A transport endpoint's coordinate is not anchored**, and is printed by name
  when one exists. `transportEndpoint` declares no city and both ways to infer
  one are worse than nothing (D098).
- **A city holding one coordinate is not checked**, and is printed by name. The
  only cure is a bounding box per `countryCode`, which is invented data inside
  a validator.
- **A transposed decimal is out of reach**: 43.8576 typed as 43.5878 moves the
  point 30 km and passes. Recorded with its number, the way `.mp3` is recorded
  as a format the duration check cannot time.
- **Weather and the two design-stack copy questions** are unchanged and were
  out of scope by instruction.

### Worth writing down, and not a defect

- **`transport.ams-corfu.u2` in the real package carries two `documentIds`** -
  one easyJet ticket per traveller, since the content session separated them.
  Screen 15 shows the first, which is exactly what the marker at
  `TransportState.kt:52` says it does: the approved sheet draws one ticket
  action, and both documents are in the Wallet. Until now that only existed in
  a code comment, and somebody opening screen 15 on 15 September would have no
  way to know a second ticket is one screen away.

## Later

- [x] **All nineteen canonical screens exist.**
- [ ] Weather (live/cached states; only the trip fallback exists today)
- [x] **Notifications — operational.** §32's "critical notifications are
      scheduled locally" is true: the twelve packaged deadlines are registered
      with `AlarmManager`, exact where the permission allows, rescheduled
      after boot and after any content or traveller change, opening the screen
      the deadline belongs to. **What is not done and is not claimed:** the
      Memory channel exists and posts nothing, pending the copy question
      (D094); Stories and Walk are unchanged from Phase 3.
- [x] **Real Balkans package** *(installed in Phase 7: `trip.json` and 27
      documents under `assets/trip-production/`.)*
- [x] **Full Trip Validator** *(§27's eleven checks are all built as of 2026-09-05; two of them remain deliberately partial and say so — see the table under "Content validation".)*
- [x] **Real-device QA** *(Closed for this build: the nineteen screens were
      walked on a Galaxy S24 with headphones, including a full loop in
      airplane mode - see the field pass above. What remains is named there
      and is not device work: two screens waiting on content, screens 08/09's
      live states waiting on a second phone, and screen 17's contrast in
      daylight.)*

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

Declared audio lengths are checked the same way the zones are — always, at any
stage (D095). `durationSeconds` drives the progress bar and the duration label,
so it is compared against the packaged file, chapter marks are compared against
the end of that file, and the marks must increase. MP4/m4a is read from its
`mvhd` atom and WAV from its header, both in stdlib; a format that cannot be
timed and a file not packaged yet are reported by name as checks that did not
run, never as failures.

Coordinates are checked the same way, and for the same reason (D098). The
schema already bounds the range, so what is checked is agreement: each point
against the nearest other point in the city it declares, over 100 km being a
finding, and the transposition named when swapping the two values lands within
5 km of that neighbour. A transport endpoint declares no city and is printed as
unanchored; a city holding one coordinate is printed as unchecked; a pair of
coincident points is correct content and is silent.

Walk stop numbering is checked in two severities (D099). A duplicate `order` is
an error at every stage, because which stop the traveller walks first then
depends on `sortedBy` being stable. A gap, or a first stop that is not 1, is a
warning until `contentStatus` is `production`, because the missing stop may
still be on its way.

Currently reported for the packaged trip: the two ticket/voucher PDFs are
declared offline but their files are not packaged yet. The UI reflects this —
it does not badge them "Offline" (D013). One audio guide of three is timed
against a real file — `ag.bascarsija`, 720 declared over a 720.0s prototype
WAV — and the other two are named as untimed, their `.mp3` files not being in
the package.

Against §27's list of eleven checks the Trip Validator "must check at minimum",
after this commit:

| §27 check | State |
| --- | --- |
| JSON Schema validation | done |
| duplicate IDs | done |
| missing refs | done |
| missing assets | partly — offline documents only; an audio guide with no packaged file is named, not failed |
| invalid coordinates | **done** — not the range, which the schema already bounds: each point against the nearest other point in its own city, with the transposition named when the swap resolves (D098) |
| missing offline documents | done |
| audio duration/chapter inconsistencies | **done this commit** — both halves: the duration against the file, and the chapters against it |
| invalid date ranges | done |
| bad timeline refs | done |
| bad walk ordering | **done** — duplicates are an error at every stage, gaps and a first stop that is not 1 are warnings until production (D099) |
| unresolved mock data in production mode | partly — `content_preflight.py` flags markers and refuses `isMockContent` in production |

**With these two, the table closes.** Every one of §27's eleven checks is
built. Two of them stay deliberately partial and say so in the row itself:
"missing assets" fails only for a document that promises offline access, and
names an unpackaged audio file rather than rejecting it, and "unresolved mock
data" lives in `content_preflight.py`. Nothing on the list is unstarted, and
what each check cannot see is printed by name when it runs - an endpoint with
no city, a city with one coordinate, an audio format that cannot be timed.

## Verification

`python tools/check_repo.py` · `python tools/validate_trip.py` ·
`python tools/content_preflight.py <package> --allow-incomplete-authoring-files`
(no generated package exists yet, so this is exercised against the runtime
and starter trips) · `python -m unittest tools/test_validate_trip.py` ·
`./gradlew testDebugUnitTest assembleDebug assembleRelease lintDebug`

Unit tests: 341 passing (Kotlin), plus 28 in `tools/test_validate_trip.py`.
Lint: 0 errors, and no lint baseline is used. The
warnings are dependency-hygiene notices only (`GradleDependency`,
`UseTomlInstead`, `NewerVersionAvailable` and the like); their count moves
with what has been published upstream since the last run, so no number is
promised here. Instrumented tests: none written. Real-device testing is manual and
is recorded per phase; Phase 4's was re-done on two devices on 2026-09-04.

`git diff --check` reports trailing whitespace inside `content/templates/`.
Those are Markdown hard line breaks on the fill-in label lines, where dropping
them would run the labels together into one paragraph; they are deliberate.
