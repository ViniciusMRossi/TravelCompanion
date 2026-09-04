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
  sentence for someone who has just tapped "Ouvir juntos";
- **screen 07 announces a story whose audio is not packaged.** Simulating
  arrival at Latin Bridge put "TOCANDO AGORA · Latin Bridge" in the header
  while the player carried on with Baščaršija, because that guide's asset is
  not in this build. Found on the device, not fixed: it is a screen 07 question
  about what to say when a story has no audio (D021's territory), and the
  answer is copy the approved design does not carry;
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
- [ ] **"Ouvindo outra história" is copy the approved design does not carry.**
      The state boards draw four participant states — synchronized,
      connecting, reconnecting, offline — and none of them is "listening to a
      different story". The words keep the register of the ones that are drawn
      and contain no network vocabulary, but they are engineering's and want a
      design confirmation.
- [ ] **A phone joining a listen in progress hears the guide while 3–2–1 is
      still on screen.** Screen 08 is a three-second transition into shared
      playback, and the joiner is given it so the screen is never blank while
      the guide loads — but the group is already running, so the audio comes
      into step at once rather than at zero. The approved design has no state
      for arriving late; the alternative, three silent seconds, tells the
      traveller less.
- [ ] **Divergence is still untested in the field.** One playable guide in the
      package means two phones cannot hold two different ones (D043).
- [ ] **A phone arriving while the group is paused waits, and says nothing
      while it waits.** The invitation is kept, not spent, and the countdown
      does not start — a transition into playback must not run at a group that
      has not begun (D044). What the traveller sees meanwhile is screen 09's
      "Comece um audioguia para ouvir junto", which is true of this phone but
      says nothing about the waiting. A state of its own would need copy the
      approved design does not carry, and one such item is already open
      ("Ouvindo outra história"), so it was not invented. Loading into a paused
      player is the other way out and was not built either.
- [x] **Re-watched on two devices against the current binary**, including
      everything the one-phone pass had claimed. See "The second device".

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

Unit tests: 160 passing. Lint: 0 errors, and no lint baseline is used. The
warnings are dependency-hygiene notices only (`GradleDependency`,
`UseTomlInstead`, `NewerVersionAvailable` and the like); their count moves
with what has been published upstream since the last run, so no number is
promised here. Instrumented tests: none written. Real-device testing is manual and
is recorded per phase; Phase 4's was re-done on two devices on 2026-09-04.

`git diff --check` reports trailing whitespace inside `content/templates/`.
Those are Markdown hard line breaks on the fill-in label lines, where dropping
them would run the labels together into one paragraph; they are deliberate.
