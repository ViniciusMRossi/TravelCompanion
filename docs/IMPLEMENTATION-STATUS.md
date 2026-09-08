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
      triggered outside a walk posts a notification that opens screen 04, where
      that story already is (D105 (a)). Both canonical screens remain
      placeholders, as later-phase screens already are.
- [x] **Passive background geofencing.** The three packaged Sarajevo triggers
      are registered as Play Services geofences whenever the app has content
      and the permission, and put back after a restart on the same receiver
      that puts the deadlines back. A transition wakes the app; the position it
      carries goes through the same `decideStoryTrigger` at the trigger's own
      radius, and only `Notify` acts. The circles are registered at a 120 m
      floor because Android cannot be trusted with the content's 80 m — a
      registration number that never reaches the decision (D103). A story is
      recorded as triggered before it is announced and its circle comes down
      immediately, so it speaks once in the trip. `ACCESS_BACKGROUND_LOCATION`
      is declared and asked for once, from screen 19; refusing it costs the
      automatic notice and nothing else (D104). A tap opens screen 04, which is
      where the same story already lives outside a walk — a proposal, because
      the approved prototype draws screen 10 only over a running walk and no
      out-of-walk story surface at all (D105 (a)). Passive discovery is silent
      while a walk is running, because the walk owns the decision and holds the
      record in memory (D105 (b)). All three circles are registered, unfiltered
      by day or city, for the reason in D105 (c).
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

- [x] **The second full visual pass** — done on 2026-09-06 with the real
      package inside, over seventeen of the nineteen screens. See "The second
      visual pass" below, including the two that could not be opened at all.
- [ ] **The canonical flow from 01, on a physical phone.** The reason this
      item carried — "never walked whole because it has never been whole" — no
      longer holds: the screens all exist, and on 2026-09-06 the chain
      **02 → 06 → 07 → 10 → 09 → 11 → 12** was walked whole on the emulator
      with the real package (see "The day-13 chain, end to end"). What is
      still untried is the run that **starts at 01 Quem é você?** on a first
      launch and ends at 12, on hardware rather than an emulator, against the
      signed `app-release.apk` (D110). Screens 05 and 08 are not part of what
      is missing here: 05 has no attraction in the real package and 08 needs a
      second phone, both recorded separately.

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

## The friend that was also wrong, and a line printed twice (2026-09-05)

Two corrections to the coordinate check of the commit before this one, and a
guard for a defect that reached the APK. All three came from review, none from
a tool.

**A sentence that could be false (D100).** D098's no-cluster finding said "with
no third coordinate in the city to say which of them belongs there". The branch
is reached whenever *neither member of the pair has a near neighbour*, which
three mutually distant points satisfy as well as two, so a city holding
Sarajevo, Paris and Sao Paulo printed that sentence twice while carrying three
coordinates. The behaviour was right and did not change; only the stated reason
was wrong. One wording, true wherever the branch is reached, replaces the two
that could drift.

**A group transposed in one go (D101).** The nearest neighbour asks "does this
point have a friend nearby?" and not "is this city one place?", and **two
coordinates wrong the same way are each other's friend**. Transposing two of the
four packaged Sarajevo points leaves two tight groups 3,691 km apart, every
point with a neighbour at 300 m, and the check reported nothing: `rc=0`, not one
line. The city is now also measured whole - the distance between its two
furthest points, against the same 100 km - and only where the nearest neighbour
found nothing there, so one defect stays one finding.

**A title printed twice (D102).** The three Sarajevo guides were promoted with
`title` identical to their story's. The player does not choose between the two,
it shows both: guide as the title, story as the subtitle, in the compact player
and in the media notification. On the lock screen - Walk Mode with headphones
and the screen off - that is the same sentence on both lines. Nothing checked
it; review found it by reading the player chain. Now `content_checks` reads the
pair from the story side.

### Proved by failing, on the sample package

- **F1** - three of the four sample coordinates moved to Paris, Sao Paulo and
  Tokyo: three findings, none of them claiming a third coordinate is missing,
  each saying *"neither has a near neighbour in the city to anchor it"*. `rc=1`.
- **F2** - two of the four transposed together. **Run against the validator as
  committed at 778a6e4, the same adulterated file gives `PASS` and `rc=0`.**
  Against this commit: *"city 'sarajevo': its coordinates span 3691.1 km, from
  attraction 'bascarsija' location.geo (43.8595, 18.431) to story
  'story.meeting-of-cultures' trigger.geo (18.4257, 43.859). Every point here
  has a close neighbour, so the city holds more than one group of coordinates
  and nothing says which group is the city. Swapping either end's latitude and
  longitude puts the two 0.6 km apart - one group has its values transposed."*
  `rc=1`.
- **F3** - the sample needed no adulteration: it already carries the clash, and
  the guard found it on its first run (below). Promoted to `production`, it is
  an error and `rc=1`.

### The guard's first run found one in the tracked packages, and it was the source

`app/src/main/assets/trip/trip.json` and `trip-package/sample/sample-trip.json`
both declared `story.latin-bridge` and `ag.latin-bridge` with the title **"Latin
Bridge"** - the real defect D102 describes, in the package this repository
ships, and there since the bootstrap. The three Sarajevo guides did not
introduce the pattern; they copied it from the example, which is the artefact
this repository uses to teach how content is written. The guard caught the
source, not only the copies.

Both files are `prototype`, so it was a **warning and `rc` stayed 0**: the two
tracked packages reported three content issues where they had reported two. It
was left standing for one commit because both files were byte-locked by
instruction, and **fixed in the commit that followed** - `ag.latin-bridge` is now
"Audioguia da Ponte Latina", which is the convention its two sibling guides
already followed, and the two packages are back to two content issues. Only the
guide's title moved: `story.latin-bridge` keeps its own, which
`StoryTriggerStateTest.kt:44` asserts against the packaged content.

### Found by writing the test, not by planning it

- **The second F3 proof proved the opposite of what it was for.** Retitling
  `ag.bascarsija` to collide with a story changed nothing, because no story
  points at it - it belongs to an attraction. What was meant as a second
  positive case turned into the false-positive guard on real data: a guide
  nobody points at has nothing to collide with, whatever it is called.

### Not implemented, with reasons

- **A city moved wholesale cannot be caught**, and there is a test that says so.
  Transposing all four Sarajevo points leaves a city that is internally
  consistent and in the wrong place; nothing inside the package contradicts it.
  The anchor would have to be external, which is the bounding-box-per-country
  table D098 refused.
- **A city holding both a lone wrong point and a transposed group reports only
  the lone point.** The diameter runs only where the nearest neighbour was
  silent, so the group surfaces on the next run once the first is fixed.

## Later

- [x] **All nineteen canonical screens exist.**
- [x] **Weather — live and cached (D164).** All four states of §14 exist:
      `Live`, `Cached`, `FallbackFromTrip`, `Unavailable`, degrading in that
      order and never stepping backwards. Open-Meteo, **no key and no
      registration**, and **no dependency added** — `HttpURLConnection` and
      `kotlinx.serialization`, both already on the classpath, with `INTERNET`
      already in the manifest. The coordinate is the **centroid of the
      attractions of the city the day happens in**, which is 19 of the 20
      days; day 1, São Paulo, packages no attraction, so it makes no call and
      keeps its packaged forecast. The last successful reading is stored in
      DataStore with its coordinate and its instant, and is **refused when the
      day's coordinate differs** (D089). **This closes §32's Definition of
      Done at 13 of 13.** Not yet seen on a device — see the session entry at
      the end of this file.
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
schema already bounds the range, so what is checked is agreement, and it is
asked twice against the same 100 km. **Each point against the nearest other
point in its city** (D098) answers "is this point in the wrong place", and names
the transposition when swapping the two values lands within 5 km of that
neighbour. **The city's own diameter** (D101) answers "is this city one place",
and it is the one that sees a group transposed in one go — two coordinates wrong
the same way are each other's near neighbour, so the first question is blind to
them. The diameter runs only where the first found nothing in that city, so one
defect stays one finding. A transport endpoint declares no city and is printed as
unanchored; a city holding one coordinate is printed as unchecked; a pair of
coincident points is correct content and is silent.

A story and the audio guide it points at may not carry the same title (D102).
The player shows the pair — guide as the title, story as the subtitle — so equal
titles print the same line twice in the compact player and on the lock screen.
Reported through `content_checks`, which makes it a warning until the package
declares `production`; the two tracked packages currently trip it, on
`story.latin-bridge` / `ag.latin-bridge`.

Walk stop numbering is checked in two severities (D099). A duplicate `order` is
an error at every stage, because which stop the traveller walks first then
depends on `sortedBy` being stable. A gap, or a first stop that is not 1, is a
warning until `contentStatus` is `production`, because the missing stop may
still be on its way.

Currently reported for the packaged trip: the two ticket/voucher PDFs are
declared offline but their files are not packaged yet, and `story.latin-bridge`
shares its title with `ag.latin-bridge` (D102). The UI reflects this —
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
| invalid coordinates | **done** — not the range, which the schema already bounds. Two questions of the same 100 km: each point against the nearest other point in its city (D098), and, where that finds nothing, the city's own diameter, which is what sees a group transposed in one go (D101). The transposition is named whenever the swap resolves |
| missing offline documents | done |
| audio duration/chapter inconsistencies | **done this commit** — both halves: the duration against the file, and the chapters against it |
| invalid date ranges | done |
| bad timeline refs | done |
| bad walk ordering | **done** — duplicates are an error at every stage, gaps and a first stop that is not 1 are warnings until production (D099) |
| unresolved mock data in production mode | partly — `content_preflight.py` flags markers and refuses `isMockContent` in production |
| *(not in §27)* story and audio guide sharing one title | **done** — the player shows the pair, not one or the other, so equal titles print the same line twice on the lock screen (D102) |

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

Unit tests: 341 passing (Kotlin), plus 39 in `tools/test_validate_trip.py`.
Lint: 0 errors, and no lint baseline is used. The
warnings are dependency-hygiene notices only (`GradleDependency`,
`UseTomlInstead`, `NewerVersionAvailable` and the like); their count moves
with what has been published upstream since the last run, so no number is
promised here. Instrumented tests: none written. Real-device testing is manual and
is recorded per phase; Phase 4's was re-done on two devices on 2026-09-04.

`git diff --check` reports trailing whitespace inside `content/templates/`.
Those are Markdown hard line breaks on the fill-in label lines, where dropping
them would run the labels together into one paragraph; they are deliberate.

## The second visual pass — nineteen screens with the real package (2026-09-06)

The first visual pass compared eight screens against the prototype, and it ran
against the **sample**: one day, one city, short strings, no hard diacritic.
The build now carries the real package — twenty days, six countries, nineteen
cities, twenty weather cards and twenty outfit cards, twenty-seven documents,
seven emergency profiles. Nothing had been *looked at* with that inside.

**Method: the first pass's, unchanged.** Two instances of the same AVD
(`android-37.1`, Play Store, Android 17), overridden to two sizes:

- **compact** — `wm size 720x1520`, `wm density 320` → **360 × 760 dp**, 2 px/dp;
- **large** — `wm size 1440x3120`, `wm density 480` → **480 × 1040 dp**, 3 px/dp.

Colour sampled from screenshot pixels against
`Field-Companion-Design-Tokens-v0.1.json`; measurements taken wherever
`TELAS-E-FUNCIONALIDADES-APPROVED.md` declares a number, read from
`uiautomator` bounds rather than from pixels by eye.

**The clock.** `dayFor` clamps, so on 6 September the app opens on day 1 and
that is all there is to see. Both AVDs are Play images and `adb root` is
refused, so `adb shell date` cannot work. What does work, with no root, is
**Android's own Settings**: `am start -a android.settings.DATE_SETTINGS`, then
the Date row and the picker, driven through `uiautomator dump`. Four dates were
set that way — 15, 20, 22 and 25 September — and the rest of the day sweep went
through **screen 03's own day arrows**, which reach any day without touching
the clock at all and are the cheaper instrument for everything except the
weather and outfit cards, which live only on Hoje.

### Verdict per screen

| # | Screen | Verdict |
| --- | --- | --- |
| 01 | Quem é você | **matches** |
| 02 | Hoje | **matches** — the three worst content days hold |
| 03 | Dia completo | **matches** |
| 04 | Cidade | **matches**; the city-audioguide card has no content to draw |
| 05 | Atração | **not seen** — the real package declares zero attractions |
| 06 | Iniciar passeio | **matches** |
| 07 | Passeio ativo | **matches** |
| 08 | Participantes sincronizados | **not seen** — needs a second phone in the group |
| 09 | Ouvir juntos | **matches**, in the "ouvindo outra história" state |
| 10 | História pelo caminho | **diverged, corrected** — D106, D107 |
| 11 | Fim do passeio | **diverged, corrected** — D106, D108 |
| 12 | Gravar memória | **matches** |
| 13 | Carteira | **matches** |
| 14 | Documento | **matches** as a ficha; QR mode has no content to draw |
| 15 | Transporte | **matches** |
| 16 | Hospedagem | **matches** |
| 17 | Emergência | **diverged; one corrected, two registered** — D108 |
| 18 | Plano B | **matches** |
| 19 | Mais | **matches** |

### Measured against the declared numbers

| Declared | Measured (compact / large) |
| --- | --- |
| 01 participant rows 76dp | 83.5 dp / **76.0 dp** — a minimum, exceeded on compact because the real package's line wraps |
| 03 day arrows 48dp | **48.0 / 48.0 dp** |
| 04 hero 300px | **299.5 / 300.0 dp** |
| 06 "Começar passeio" 56dp | **56.0 / 56.0 dp** |
| 07 close 48dp | **48.0 / 48.0 dp** |
| 07 transport 56 / 76 / 56dp | **56 / 76 / 56 dp** on both |
| 09 transport 52 / 68 / 52dp | **52 / 68 / 52 dp** on both |
| 10 "Ouvir agora" 56dp | 52 dp at 118 dp wide **before**; **316 × 56 / 436 × 56 dp** after |
| 11 "Gravar memória" 56dp | 52 dp at 150 dp wide **before**; **320 × 56 / 440 × 56 dp** after |
| 12 record button 82dp | **82.0 / 82.0 dp** |
| 13 document rows, 48dp minimum | **70.0 / 70.0 dp** |
| 17 contact rows, 48dp minimum | **76.0 / 76.0 dp** |
| 17 122 / 124 border 2px ink | absent **before**; **4px `#16232E` at 2px/dp = 2.0 dp** after |
| 19 emergency row 68dp | **68.0 / 68.0 dp** |

### Sampled colours, all exact against the token file

`#F5F1E8` paper, `#FFFDF8` surface, `#16232E` ink, `#1F6F78` teal,
`#7A2E2E` oxblood, `#FDECEC` critical-subtle, `#E7EDE3` success-subtle,
`#E4F0F0` primary-subtle, `#F3E8D0` warning-subtle, `#DDDCD4` neutral-200.
The Now card's teal quarter-circle samples **`#1A454F`**, which is the
prototype's `rgba(31,111,120,.45)` over ink to the byte.

### The content days, and what they proved

The rule going in was that a text which does not fit is a layout defect and
never a text to shorten. Nothing had to be shortened.

- **day 11 (23/09)** — the longest weather summary in the package, 154
  characters, in a card that shares a `weight(1f)` row with the outfit card.
  The card **grows**, the outfit card matches its height, and nothing is
  clipped or ellipsized at either size. This was the case most likely to break
  and it is clean;
- **day 8 (20/09)** — the second-longest summary, 153, *and* a 78-character
  `special`, both cards loaded at once. Both grow, both end together, and the
  "ATENÇÃO" line is whole. It is also the biggest weather swing of the trip,
  which is why the text is long;
- **day 10 (22/09)** — the longest outfit item, 100 characters, over seven
  lines. Here the **outfit** card is the taller one and the weather card
  matches it: the pairing works in both directions;
- **day 20 (02/10)** and **day 6 (18/09)** — the two longest timeline details,
  170 and 168 characters. Both wrap and the row grows; no truncation;
- **day 3 (15/09)** — the longest `day.title`, "Amsterdã → Corfu → Sarandë →
  Ksamil", 35 characters with three arrows. Three clean lines at 360 dp, and
  the day's five timeline items and its critical card all draw;
- **day 15 (27/09)** and **day 17 (29/09)** — four cities and five timeline
  items respectively, both fine;
- **day 1 (13/09)** — seven documents in the day, and the Wallet reads
  **"27 documentos"** with the "Tudo offline" pill, a state the sample package
  could never show.

**Diacritics, seen on screen rather than in the JSON:** `Baščaršija` (screens
02, 06, 07, 09, 11), `Žabljak`, `Sarači`, `Ilidža`, `Počitelj`, `Čilipi`,
`Sarandë`, `Amsterdã`, `São Paulo`, `Ônibus`, and `Srđ` on screens 18 and 19 —
the lowercase d-stroke. None rendered as an empty box. **Uppercase `Đ` was not
seen, because the package contains none:** zero occurrences in `trip.json`. The
city the content spells `Bastasi` renders `Bastasi`; that is what the package
says, and content is not this pass's to change.

### The day-13 chain, end to end

With the clock at 25 September, on both sizes:

02 → the 09:00 timeline row → **06 Iniciar passeio** ("Do Sebilj ao rio", route
"Baščaršija · Sarači · Ferhadija · Ponte Latina") → *Começar passeio* → **07
Passeio ativo** → a mocked fix inside the 80 m radius → **10 História pelo
caminho**, in **2.2 s** → *Ouvir agora* → audio playing → screen off, playback
continuing at position 51 s under `PARTIAL_WAKE_LOCK 'ExoPlayer:WakeLockManager'`
→ *Ouvir juntos* → **09** → *Encerrar passeio* → **11 Fim do passeio**, reading
**"HISTÓRIAS OUVIDAS · 1 de 3"** → *Gravar memória* → **12**.

**Screen 10's offered case is reachable in the running app for the first
time.** The field pass could only reach it through the debug scaffold, because
every story in the sample declared `autoPlayInWalk`; none of the three real
triggers does, so a real arrival offers rather than plays.

**D102's correction seen on screen, which had never happened.** With the guide
playing, the media notification and the lock screen read

    Sebilj, Baščaršija
    O Sebilj tem 1891; a praça tem 1462

— the guide's title over the story's, two different lines, where the defect was
the same line printed twice. Screen 09's player card and screen 11's compact
player show the same pair.

`adb emu geo fix` does nothing on these AVDs; what works is
`appops set --uid 2000 android:mock_location allow` plus
`cmd location providers set-test-provider-location`, **with `--accuracy 8`**,
because the default 100 m is correctly refused by
`DeviceLocation.isUsableFor(80.0)`.

### Corrected in this commit

All four are corrections **to** the prototype, and all four were re-measured
afterwards at both sizes and at a 1.5 system font.

- **screens 10 and 11's primary action** — full width at 56dp with its icon,
  and the secondaries beneath at 52dp, instead of a `FlowRow` of auto-width
  52dp buttons that left the primary narrower than the button beside it (D106);
- **screen 10's operational line** — it printed the story's title a second
  time, two lines under the headline, because the field read a walk stop's
  title and a stop's title *is* the story's title. The unit test asserted the
  duplicate; it asserts the rule now (D107);
- **screen 11's four stat tiles** — a 1dp hairline, and equal height within a
  row, which the approved grid gets for free and a `FlowRow` does not (D108);
- **screen 17's 122 and 124** — the 2dp ink border the sheet draws, missing
  entirely (D108).

### Registered, not corrected

- **screen 10's *Depois*** is `background:transparent;border:0;color:#1F6F78`
  in the prototype — the only tertiary button of that kind in the whole
  approved set. It renders as an outlined secondary. No such component exists
  in the design system, and adding one is a design-system addition rather than
  a layout correction, so it is written down instead of invented;
- **screen 17's "Ligar 112" composition.** The prototype draws a left-aligned
  row: a 40px handset, then "Ligar 112" at 28px over "Emergência geral ·
  funciona sem crédito" at 16px in `#F4D9D7`, `min-height:88px`. The app draws
  it centred and stacked, at 150dp. The colour, the label and the reachability
  are right and the target is larger rather than smaller; the *arrangement* is
  not the sheet's. Not redrawn unilaterally on the safety screen — it is the
  one place where a change of composition deserves the design's own answer
  first;
- **`generalEmergency.note` reaches no screen.** The real Bosnian profile
  carries "O 112 ainda está em implantação na Bósnia; as páginas oficiais do
  país publicam 122, 123 e 124…", which is exact and true of the country the
  traveller is standing in, and the big button shows the app's own fixed
  sentence instead. Where that note should go is a design question.

### What could not be looked at, and why

Said plainly, because a visual pass that hides its holes is worse than none.

- **screen 05 Atração was not seen at all.** `attractions: []` in the real
  package — there is no attraction to open and no "Ver atração" to open it
  from. It was compared with the prototype in the first pass against the sample
  package, and this pass adds nothing to it;
- **screen 08 Participantes sincronizados was not seen.** It is a three-second
  transition that only happens when a *shared* listen starts, which needs a
  second phone in the group and a reachable backend. Tapping "Ouvir juntos"
  goes straight to 09, in its "ouvindo outra história" state;
- **screen 14's QR mode was not seen.** All 27 real documents declare
  `qr: {mode: "none"}` — a content decision, and a different reason from the
  mock-code block the field pass recorded;
- **screen 04's city audioguide card and its chapter list were not seen.** The
  real Sarajevo city carries no `audioGuideId`, and all three packaged guides
  have zero chapters, so the declared "play de 46dp, 5 capítulos, 34 min" has
  nothing to draw;
- **screen 09's Sincronizado and divergent states**, for the same want of a
  peer as screen 08;
- **screen 07's Denied state was not re-exercised** this session; Searching was
  seen in passing;
- **uppercase `Đ`** has no occurrence in the package to render;
- **nothing was checked on the Galaxy S24.** Colour cannot be sampled there
  (D086) and this pass is about colour and measurement, so it is both emulators
  and neither phone;
- **rotation was not re-checked**, and **night mode was checked on one screen
  only** — it stays light, with dark bar icons on the paper strip, which is
  D052 holding.

Everything else in the table above was opened, measured and sampled at both
sizes.

## A promise that was false on the safety screen, and a release nobody could install (2026-09-06)

Two corrections, seven days before departure, both found in the package that
actually boards rather than in the sample.

### The note that lied on screen 17 (D109)

`WITHHELD_NOTE` — "O número chega com os dados reais da viagem." — belongs to
D061/D064 and to mock content: the package ships `+000000000` and
`+387000000000`, and the row stays to say what is missing and when it arrives.
`phone()` in `domain/operations/Contacts.kt` folded two different states into
one `withheld` flag, so a number that is simply **absent from a real package**
got the same sentence.

In `assets/trip-production/trip.json` (`isMockContent=false`) five of the ten
stays carry no `contactPhone`: `acc.bastasi.camp`, `acc.sarajevo.estudio`,
`acc.dubrovnik.cidade-velha`, `acc.cilipi.estudio`, `acc.amsterdam.amigo` —
**days 11, 12, 13, 16, 17, 18 and 19**. On each of those seven days screen 17
drew a "Onde estão as malas" row promising a number that is never coming.

- `phone()` now splits the gate: withheld-as-mock keeps the note; a null or
  blank number in a real package is undialable with **no note**. Mock
  behaviour is byte-for-byte what it was;
- screen 17's stay row follows the rule its own header states — everything on
  it is either a number to call or a sentence to show a stranger — so with no
  number there is **no row**: `stay?.contactPhone?.let`, the same shape
  `StayState` (screen 16) already used. Screen 16 never had the defect;
- no new copy invented. The sheet does not draw "sem telefone cadastrado" and
  the emergency screen is not where to experiment.

Every other `phone()` caller was read. In the production package none of them
can pass a null: the six emergency-profile numbers are all present in all
seven profiles, no transport declares a `phone` action, and no Plan B step
does either. So the only screen whose output changes is 17, on those seven
days. `OperationsUi`'s `PhoneRow` needed nothing — it already draws
`phone.note` only when non-null, and greys the handset when `number` is null.

### The release that did not install (D110)

`app/build.gradle.kts` had no `buildTypes` block, so `assembleRelease`
produced `app-release-unsigned.apk`. `apksigner verify` on it: **DOES NOT
VERIFY — Missing META-INF/MANIFEST.MF**. Device QA recorded above was run
"against a debug build", which is what is on the telephones, and the debug
build is the one that draws "Protótipo · simular chegada" on screen 07 — so
the release-DEX guard was proving something about an artefact nobody could
install.

One line, `signingConfig = signingConfigs.getByName("debug")`. No minify, no
new keystore. **The binary for device QA is now `app-release.apk`**, and the
DEX check finally means something.

Worth recording for the next pass: counting `META-INF/*.{RSA,SF}` zip entries
does **not** detect signing here. With `minSdk 26` AGP signs with v2/v3 only,
whose block is not a zip entry — the installable debug APK scores 0 by that
measure too. Use `apksigner verify --print-certs`.

### Verified

- 6 validators rc=0, output unchanged; `test_validate_trip.py` 39 tests, OK;
  `check_repo.py` PASS; `git diff --check` clean;
- Kotlin unit tests **357, 0 failures** (355 before, plus the two written to
  fail first), counted from the 44 XML files in
  `app/build/test-results/testDebugUnitTest/`;
- `lintDebug`: **0 errors**, 33 warnings;
- both APKs carry **31 entries under `assets/trip-production/`** (27 PDF,
  3 `.m4a`, `trip.json`), verified inside the APK;
- DEX strings: debug "Protótipo" 2 / "simular chegada" 2; **release 0 and 0**,
  now measured in the signed `app-release.apk`;
- `apksigner verify --print-certs app-release.apk`: verifies under v2, one
  signer, `CN=Android Debug`, certificate SHA-256
  `530dacfc…b009fc` — the same certificate as `app-debug.apk`.

## Hygiene, and the first two lines of log this app has ever had (2026-09-06)

No behaviour change, no UI change, no dependency. Five small things, seven
days out.

1. **`CLAUDE.md` described a repository seven phases gone.** Its "Current
   repository phase" section still read "This is a Phase 0 starter … implement
   approved screens 01, 02 and 05" — the first file every agent reads,
   carrying the defect class this repository keeps finding: a note describing
   a state that stopped existing. Only that section was rewritten. It now
   names Phase 8, §32 at 12 of 13, the gitignored production package, the
   signed `app-release.apk` as the QA binary, the two tracked `trip.json`
   blobs that must stay equal, and the trip dates. **No counts were copied
   into it** — test totals and lint numbers age, and they live here.

2. **A §32 line this file already contradicted.** "The first end-to-end run of
   the canonical flow, which has never been walked whole because it has never
   been whole" was written before the screens existed. The 2026-09-06 pass
   walked 02 → 06 → 07 → 10 → 09 → 11 → 12 whole, on the emulator, with the
   real package. The item is not deleted and not ticked: it is rewritten to
   the thing that is actually still untried — **starting at 01 on a first
   launch, ending at 12, on hardware, against the signed release APK**.

3. **A test whose name outlived its meaning.** After D109 "withheld" means the
   mock note, and `a missing number is withheld even in a real package` names
   the one case that is *not* withheld. Its assertion was and remains correct,
   so only the name changed, to `a missing number is never dialable, mock or
   real`.

4. **Room's version 1 says what happens next (D111).** No migration, no
   `fallbackToDestructiveMigration()`, and until now nothing said so. A KDoc on
   `@Database` now states that the first unmigrated schema change stops the app
   opening at all, with the trip's memories inside — and that the destructive
   fallback is refused on purpose, because it converts that crash into a
   silent `DROP TABLE` of the one thing this app holds that exists nowhere
   else.

5. **Two `Log.i` sites, and nothing else (D112).** The app had no `Log.*` call
   anywhere. `CriticalAlertScheduler.schedule()` now logs one line per alarm
   and one total; `PlayServicesStoryGeofences.register()` logs one line per
   circle, one naming which permission is missing on an early return, and one
   if the call throws. Tag `TravelCompanion`, `Log.i` so it survives into the
   release build. No personal data, no continuous position, nothing on screen.

### The logcat, from the signed release APK

`app-release.apk` installed on the Pixel emulator, whose **clock is at 25
September 2026** — day 13 of the trip, so 5 of the package's 12 deadlines are
still ahead of it and only those are registered. Location granted by `adb`;
exact alarms left at the system default, which is denied:

```text
I TravelCompanion: alarm scheduled id=ci.train-mostar at=2026-09-26T01:50:00-03:00[America/Sao_Paulo] mode=inexact
I TravelCompanion: alarm scheduled id=ci.tour-herzegovina at=2026-09-27T04:20:00-03:00[America/Sao_Paulo] mode=inexact
I TravelCompanion: alarm scheduled id=ci.bus-dubrovnik at=2026-09-28T01:30:00-03:00[America/Sao_Paulo] mode=inexact
I TravelCompanion: alarm scheduled id=ci.caiaque at=2026-09-28T07:45:00-03:00[America/Sao_Paulo] mode=inexact
I TravelCompanion: alarm scheduled id=ci.bagdrop-dubrovnik at=2026-09-29T23:45:00-03:00[America/Sao_Paulo] mode=inexact
I TravelCompanion: alarms scheduled total=5 canBeExact=false
I TravelCompanion: geofence requested id=story.sarajevo.sebilj radius=120.0m
I TravelCompanion: geofence requested id=story.sarajevo.encontro-de-culturas radius=120.0m
I TravelCompanion: geofence requested id=story.sarajevo.ponte-latina radius=120.0m
```

**`mode=inexact` is the proof the line is worth having**: the emulator denies
`SCHEDULE_EXACT_ALARM` by default, so every one of those deadlines is on a
maintenance window rather than the minute (D093), and nothing on any screen
says so. Granting the appop and relaunching flips the same six lines to
`mode=exact` / `canBeExact=true`, so the field reports the state rather than a
constant. The zone shown is the *phone's* — `America/Sao_Paulo` on this
emulator — which is the rendering that answers "will it ring at 04:45 where I
am standing": `ci.bagdrop-dubrovnik` at `2026-09-29T23:45-03:00` is 04:45 on
30 September in Dubrovnik, the deadline Phase 8 was built for.

The three geofences are the three Sarajevo triggers, each at **120 m** — the
registration floor, not the content's 80 m, which is `GEOFENCE_FLOOR_RADIUS_METERS`
behaving as D103 describes.

### What the log found on its first run, and is not fixed here

**Every one of those lines appears twice per launch**, on two different
threads. `LaunchedEffect(content, participantId)` in `AppNavigation` re-runs
when its second key resolves, so the whole set is cancelled and re-registered
a second time on every cold start. It is harmless — `cancelAll()` then the
same five alarms, same ids, same instants — and it is real duplicated work
that nothing could see before this commit. Not touched seven days out;
recorded so the next pass has it.

### Verified — baseline unchanged by this commit

- 6 validators rc=0; `test_validate_trip.py` 39 tests OK; `check_repo.py`
  PASS; `git diff --check` clean;
- Kotlin **357 tests, 0 failures**, counted from the 44 XML files in
  `app/build/test-results/testDebugUnitTest/`, the renamed test present under
  its new name;
- `lintDebug` **0 errors, 33 warnings**;
- both APKs carry **31 entries** under `assets/trip-production/`; release DEX
  `"Protótipo"` 0 and `"simular chegada"` 0;
- `apksigner verify --print-certs app-release.apk`: verifies, `CN=Android
  Debug`.

## Three content corrections, and one that could not be made (2026-09-06)

Content only. No Kotlin, no `tools/`, no screen. Six days of trip content, six
days from departure.

### The five missing host telephones stay missing (D113)

The five stays with no `contactPhone` — `acc.bastasi.camp`,
`acc.sarajevo.estudio`, `acc.dubrovnik.cidade-velha`, `acc.cilipi.estudio`,
`acc.amsterdam.amigo` — were taken back to the 28 vouchers in
`trip-package/source/private/`, and **not one of them could be filled**. This
is the scope's main result and it is a finding, not a failure to look:

| stay | document consulted | why it stayed empty |
| --- | --- | --- |
| `acc.sarajevo.estudio` | `Hospedagem Saravejo.pdf` | Airbnb trip sheet. Host "Dino", co-host "Mona", a *"Ligar para o anfitrião"* button — **no number printed** |
| `acc.dubrovnik.cidade-velha` | `Hospedagem Dubrovnik.pdf` | Airbnb. Host "Teo", same button, no number |
| `acc.cilipi.estudio` | `Hospedagem Čilipi.pdf` | Airbnb. Host "Mihaela", same button, no number |
| `acc.bastasi.camp` | **none exists** | no file in `source/private/` mentions Bastasi, rafting or the Tara |
| `acc.amsterdam.amigo` | **none exists** | a friend's spare room; no voucher was ever issued |

The split is clean and explains itself: **every stay that has a number was
booked through Booking.com or Decolar**, whose vouchers print the property line
beside the address — that is where Kotor's `+382 67 268 787`, Ksamil's,
Žabljak's and Mostar's came from. **Every stay that lacks one is an Airbnb**,
which by design routes contact through the app and prints no host telephone.
The `"Ligar para o anfitrião"` button was checked for a `tel:` link annotation
in the raw PDF objects as well as in the extracted text: there is none.

So the field is left absent, on purpose (D064, D109). **The consequence is
unchanged and worth restating**: on days 11, 12, 13, 16, 17, 18 and 19 screen
17 offers no accommodation row and screen 16 no host button. Closing that
needs a number from the traveller — from the Airbnb app or a message to the
host — not from this repository.

### Day 3's coat pointed past the end of the trip (D115)

Before:

> O casaco sai de cena aqui e só volta a servir em **Žabljak, no dia 20**

After:

> O casaco sai de cena aqui e só volta a servir na **subida ao Lovćen, no dia 7**

Wrong twice. The "20" was the *date*, 20 September, on a screen whose header
reads "Dia 3 de 20" — so read as the screen invites, it pointed past the last
day of the trip. And Žabljak is Dia 8 in any case, while the coat comes back
on **Dia 7**, whose own outfit line already reads "Casaco fino mesmo com 27 °C
na baía" for the Lovćen massif. Days 4, 5 and 6 are beach and city and need
none, so the new sentence is the first day the coat is actually wanted.

### Montenegro's consular note was filed against the 112 button (D114)

Before — on `generalEmergency`, the emergency number:

> Montenegro não tem posto brasileiro; quem cobre é Belgrado, por jurisdição
> cumulativa.

After — on `consular`, where the schema already allowed a `note`, reworded so
it no longer repeats the label standing above it:

> Montenegro não tem posto brasileiro próprio; esta embaixada o cobre por
> jurisdição cumulativa.

**Bosnia's `generalEmergency.note` was deliberately not touched.** It claims
the screen shows "122, 123 e 124" when the schema has no fire-brigade field and
the profile carries only 122 and 124 — a separate defect whose fix waits on a
design decision. No `generalEmergency.note` reaches any screen today.

### The three copies, and proving the invariant is real

`generated`, `production` and `assets/trip-production` must stay byte-identical
except for `metadata.contentStatus`, because copying `generated` over
`production` would carry `"draft"` along and silently drop the validator out of
its strictest mode. All three were edited, and the invariant was **proved by
breaking it**: with `production`'s `contentStatus` tampered to `"draft"` the
comparison reports `MISMATCH` and exits 1; restored, it exits 0 with all three
agreeing. Final state:

```text
generated        contentStatus='draft'       expected='draft'       OK
production       contentStatus='production'  expected='production'  OK
trip-production  contentStatus='production'  expected='production'  OK
production       identical to generated (ignoring contentStatus): True
trip-production  identical to generated (ignoring contentStatus): True
rc=0
```

**None of the three is in the commit** — all are gitignored (D028), and the
only tracked authoring source, `source/itinerary/`, carries neither of the two
corrected sentences. This commit therefore contains documentation only, and the
content changes live in the working copy and in the APK built from it.

### Verified

- 6 validators rc=0; `check_repo.py` PASS; `test_validate_trip.py` 39 tests OK;
  `git diff --check` clean;
- `content_preflight` **PASS 3 warning(s)** on `generated` and **PASS 8** on
  `assets/trip`, both unchanged;
- Kotlin **357 tests, 0 failures** from the 44 XML files in
  `app/build/test-results/testDebugUnitTest/`; `lintDebug` 0 errors;
- both APKs carry **31 entries** under `assets/trip-production/`; release DEX
  `"Protótipo"` 0 and `"simular chegada"` 0.

## Editorial migration, phase 0: four corrections from the detailed itinerary (2026-09-07)

Content only. No Kotlin, no `tools/`, no schema, no screen. The first pass that
reads the traveller's **detailed itinerary** — 1808 lines of prose that stay
outside the repository (booking locators, door PINs, card references) — against
the packaged trip. This phase carries the four corrections that document forces;
the editorial migration proper (`usefulApps`, `attractions`, `restaurants`)
follows in phases 1 to 3.

### What changed

| # | Field | Before | After |
| --- | --- | --- | --- |
| 0.1 | `acc.bastasi.camp.contactPhone` | absent | `+381 64 420 1956`, second number in `instructions` |
| 0.2 | `days[2].outfit.special[0]` | "…só volta a servir na subida ao Lovćen, no dia 7" | "…só volta a servir em **Žabljak, no dia 8**" |
| 0.3 | `transport.ksamil-budva.night` | 1 `criticalItem`; the 4h38 wait unmodelled | 2 `criticalItems`; new Dia 5 timeline row at 05:00 |
| 0.4 | `OU 663` (6 strings, id included) | `OU 663` | `OU 661` |

### The coat reverses D115, and the source is why (D116)

D115, six days ago, moved the coat from "Žabljak, no dia 20" — a *date* on a
screen headed "Dia 3 de 20" — to "na subida ao Lovćen, no dia 7". The move away
from the date was right. The destination was not: it was inferred from Dia 7's
own `carry` line, "Casaco fino mesmo com 27 °C na baía", and an inference from
a neighbouring field loses to the source. The itinerary calls the Lovćen day
*"o dia mais leve para vestir da viagem inteira"* and says, on the next day in
Žabljak, **"O casaco sai da mochila aqui."** Dia 8. D115's rule — in prose the
day is `dayNumber` — is untouched; only where the coat comes back changed.

### The camp's telephone was never in a voucher, because there is no voucher

D113 searched the 28 files in `source/private/` and reported, correctly, that
none mentions Bastasi, rafting or the Tara. The camp issues no voucher; it
confirmed by e-mail, and the two numbers live in the itinerary's day-23
contingency. So the rule D113 set holds — a number enters the package only if a
document prints it — and what widened is which documents count. **Screen 17 now
has an accommodation row on Dia 11**, the most isolated base of the route. The
four remaining stays without a number stay without one, for D113's reasons.

### Podgorica: 4h38 that existed only as a subordinate clause

The night bus is one booking and stays one `transport`. What it lacked was any
representation of the change: arrival 05:00, next boarding 09:38, a second leg
by another company (Touring Kotor, route CHR3229, **no FlixBus livery**), and
station fees in cash only. It now carries `ci.podgorica-conexao`
(`actionByTime` 09:20), and Dia 5 opens with `d05.podgorica` at 05:00. No
`timeZone` override: both times are `Europe/Podgorica`, which is the zone Dia 5
declares — the row would have needed one only on Dia 4 (`Europe/Tirane`).

### OU 663 was never a flight

The itinerary says OU661 twice; the package said OU 663 six times. Neither
document can settle that against the other, so the ticket was opened:
`Croatia Airlines Ticket.pdf`, flight table
`30SEP 0615 DUBROVNIK ZAGREB 0710 OU661`. All six corrected, **including
`transport.dubrovnik-zagreb.ou663` → `…ou661`** — nothing outside the package
referenced the id, and it was the last place the wrong number survived.

### The three copies

Proved by breaking it. With `production`'s `contentStatus` tampered to
`"draft"` the comparison reports `MISMATCH` and exits 1; restored, it exits 0.
Final state:

```text
generated        contentStatus='draft'       expected='draft'       OK
production       contentStatus='production'  expected='production'  OK
trip-production  contentStatus='production'  expected='production'  OK
production       identical to generated (ignoring contentStatus): True
trip-production  identical to generated (ignoring contentStatus): True
rc=0
```

### Verified

- 6 validators rc=0; `check_repo.py` PASS; `test_validate_trip.py` 39 tests OK;
  `git diff --check` clean;
- `content_preflight` **PASS 3 warning(s)** on `generated` and **PASS 8** on
  `assets/trip`, both unchanged;
- Kotlin **357 tests, 0 failures**, counted from the 44 XML files in
  `app/build/test-results/testDebugUnitTest/`; `lintDebug` **0 errors, 33
  warnings**;
- both APKs carry **31 entries** under `assets/trip-production/`; release DEX
  `"Protótipo"` 0 and `"simular chegada"` 0;
- read back **from inside `app-release.apk`**: `contactPhone` present, coat line
  reads Žabljak/dia 8, `ci.podgorica-conexao` and `d05.podgorica` present,
  `OU 661` with zero occurrences of `OU 663` or `ou663`;
- `app/src/main/assets/trip/trip.json` and `trip-package/sample/sample-trip.json`
  both still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Editorial migration, phase 1: seven usefulApps (2026-09-07)

Content only. `usefulApps` was `[]`, so screen 19's "Na estrada" section drew
Plan B rows and nothing else. It now carries **7 apps**, in the priority order
the itinerary's *Apps e ferramentas* section sets.

| id | name (what screen 19 renders) | countryCodes |
| --- | --- | --- |
| `app.mapa-offline` | Organic Maps ou Maps.me · mapa offline dos seis países | — |
| `app.tradutor` | Google Tradutor · pacotes de albanês, bósnio, croata e grego | AL BA HR GR ME |
| `app.whatsapp` | WhatsApp · como o camp, os operadores e os táxis falam | AL ME BA HR |
| `app.busticket4me` | BusTicket4.me · horários e compra de ônibus | AL ME BA HR |
| `app.getbybus` | GetByBus · horários e compra de ônibus | AL ME BA HR |
| `app.ferryhopper` | Ferryhopper · o ferry de Corfu a Sarandë saiu daqui | GR AL |
| `app.conversor` | XE ou conversor offline · são quatro moedas na mesma viagem | — |

`MoreState.kt:57` maps these to `UsefulAppUi(it.name, it.action)` and
`MoreScreen.kt:87` draws one `ActionRow` each, so all seven are on screen today.

### Two deviations, both deliberate (D117)

**`requiresInternet` is `true` on all seven, including the two apps that work
offline.** The field documents the *action*, not the app — `TripModels.kt` says
so at the default: *"assume an action needs the network unless the content
package states otherwise, so offline UI never over-promises"* — and the only
`false` in the repository is `directionsAction`, whose primary `uri` is
`google.navigation:`, an installed app opened with no network. Every action
here is a **Play Store listing**, which needs the network whoever asks. So the
offline fact went into `name` instead, which is the one string screen 19
renders, and it reaches the traveller rather than sitting in a flag no screen
reads.

**The bank app, the document's eighth item, is left out.** It names no bank, no
app and no identifier; `action.uri` is required, and the only URI available
would have been invented. Its real content is a reminder tied to the Kotor
withdrawal on days 5–7, which `usefulApp` has no field for.

`uri` is a store **search** on the name the document uses, never a package id —
a wrong id is a dead end at the moment the row is pressed. `ExternalActionLauncher.open`
falls through `market://search?q=…` to `https://play.google.com/store/search?q=…`.
`dayIds` is set nowhere: the document ties no app to a day.

### Verified

- 6 validators rc=0; `check_repo.py` PASS; `test_validate_trip.py` 39 tests OK;
  `git diff --check` rc=0;
- `content_preflight` **PASS 3 warning(s)** on `generated` and **PASS 8** on
  `assets/trip`, both unchanged;
- three copies identical except `contentStatus` (rc=0);
- Kotlin **357 tests, 0 failures**, from the 44 XML files in
  `app/build/test-results/testDebugUnitTest/`; `lintDebug` **0 errors, 33
  warnings**;
- both APKs carry **31 entries** under `assets/trip-production/`; release DEX
  `"Protótipo"` 0 and `"simular chegada"` 0;
- read back **from inside `app-release.apk`**: 7 `usefulApps`, ids, names,
  `countryCodes` and `market://` URIs all as written;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Editorial migration, phase 2: twenty-seven attractions (2026-09-07)

Content only. `attractions` was `[]` across 19 cities, so screen 04 listed none
and screen 05 was unreachable. It now carries **27**, all linked from
`city.attractionIds`, plus the **11** timeline `refId`s that were `null` and the
walk start that screen 05 needs.

| city | n | attractions |
| --- | --- | --- |
| amsterdam | 2 | Jordaan e os canais · Casa de Anne Frank |
| ksamil | 1 | Ksamil e as ilhotas |
| butrinto | 1 | Butrinto |
| budva | 1 | Casco antigo de Budva |
| kotor | 4 | Muralhas e forte de São João · Casco antigo · Ladder of Kotor · Teleférico Lovćen e Alpine Coaster |
| zabljak | 3 | Lago Negro · Bobotov Kuk · Cânion Nevidio |
| bastasi | 1 | Rafting no cânion do Tara |
| sarajevo | 3 | Baščaršija · War Childhood Museum · Vijećnica |
| butmir | 1 | Túnel da Guerra |
| mostar | 3 | Stari Most · Bazar Kujundžiluk · Minarete da Koski Mehmed Pasha |
| blagaj | 1 | Tekke de Blagaj |
| kravice | 1 | Cachoeiras de Kravice |
| pocitelj | 1 | Počitelj |
| dubrovnik | 4 | Caiaque e Lokrum · Cidade velha · Muralhas · Teleférico do Monte Srđ |

Four carry a `planBId` — Lovćen, Bobotov Kuk, the kayak and the Srđ — and three
carry `documentIds` into tickets already in the wallet.

### No coordinate was packaged, and that is the result (D118)

The itinerary carries map **queries**, never latitude and longitude, so every
`location` here is `name` + `mapsQuery` and sometimes `address`. A coordinate
would have had to come from outside the document, and D098/D100/D101 catch a
wrong point but not a plausibly wrong one. `AttractionState.directionsAction`
already falls back to `mapsQuery` when there is no `geo`, so "Como chegar"
works on all 27. The validator reports, unchanged from before this commit:

```text
Coordinates: 1 city cluster(s) checked, 0 point(s) with nothing to anchor them to
- checked city 'sarajevo' (4 coordinates)
```

### What the eleven refIds do, and what they do not

They do **not** make the rows clickable. `timelineDestination`
(`AppNavigation.kt:151`) routes transport, accommodation and walk and returns
null for attraction, and `TimelineDestinationTest` asserts exactly that, on
purpose. What they do buy is real: `CityState.kt:104` builds its schedule map
from these rows, so screen 04 now prints the hour beside eleven attraction
cards. Navigation waits on a code change no content session should make.

| day | row | points at |
| --- | --- | --- |
| 2 · 15:30 | `d02.anne-frank` | `attr.amsterdam.anne-frank` |
| 4 · 08:30 | `d04.butrinto` | `attr.butrinto.sitio` |
| 6 · 06:15 | `d06.muralhas` | `attr.kotor.muralhas` |
| 12 · 09:00 | `d12.rafting` | `attr.bastasi.rafting-tara` |
| 13 · 14:45 | `d13.tunel` | `attr.butmir.tunel` |
| 15 · 11:00 | `d15.blagaj` | `attr.blagaj.tekke` |
| 15 · 13:30 | `d15.kravice` | `attr.kravice.cachoeiras` |
| 15 · 16:00 | `d15.pocitelj` | `attr.pocitelj.vila` |
| 16 · 13:00 | `d16.caiaque` | `attr.dubrovnik.caiaque` |
| 17 · 08:00 | `d17.muralhas` | `attr.dubrovnik.muralhas` |
| 17 · 17:30 | `d17.teleferico` | `attr.dubrovnik.teleferico-srd` |

`walk.sarajevo.bazar-ao-rio` also gained `startAttractionId:
attr.sarajevo.bascarsija`, so screen 05 for Baščaršija shows the walk's
departure strip on Dia 13 — the 02 to 05 flow D008 describes.

### Where the document hesitates, the field hesitates

- **Kotor's walls**: `price` is the itinerary's own sentence, *"As fontes
  divergem entre €8 e €15 por pessoa… com uns €30 trocados no casal a dúvida
  não importa"*, not a number chosen from it.
- **Kravice**: the document contradicts itself, ~€5 per person in the
  description against €10 at the gate in the tour voucher. `price` records both
  and says to carry the €10.
- **A duration given as a range sets no `recommendedDurationMinutes`.** 8–10 h
  on the Bobotov Kuk, 45–60 min at the Tunnel, 2h30–3h at the Nevidio: the chip
  reads "Visita ~N min", and any N would be this session choosing. Only single
  figures the document states became one — 180 Butrinto, 90 Lago Negro and
  Blagaj, 210 rafting, 120 Anne Frank and Dubrovnik's walls, 60 Počitelj.

### Corfu has no attraction

The itinerary describes Corfu as a port to pass through — which terminal, and
not to confuse it with the old one — and that is transport, already modelled on
the ferry leg. There is no sight in it to write, and inventing one is the single
thing this session must not do.

`historySections` (3) and `interestingFacts` (0) were written only where the
document carries real history. **Neither reaches a screen today**:
`AttractionState` reads `whatToObserve`, not those two. Packaged as content, not
as a promise.

### Verified

- 6 validators rc=0; `check_repo.py` PASS; `test_validate_trip.py` 39 tests OK;
  `git diff --check` rc=0;
- `content_preflight` **PASS 3 warning(s)** on `generated` and **PASS 8** on
  `assets/trip`, both unchanged;
- three copies identical except `contentStatus` (rc=0);
- Kotlin **357 tests, 0 failures**, from the 44 XML files in
  `app/build/test-results/testDebugUnitTest/`; `lintDebug` **0 errors, 33
  warnings**;
- both APKs carry **31 entries** under `assets/trip-production/`; release DEX
  `"Protótipo"` 0 and `"simular chegada"` 0;
- read back **from inside `app-release.apk`**: 27 attractions, all 14 city links,
  all 11 `refId`s, the walk start and the 4 `planBId` links;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Editorial migration, phase 3: plan Bs, restaurants, and five dates that read as day numbers (2026-09-07)

Content only. `planBs` 6 → **12**, `restaurants` 0 → **4**, plus **9** new
`planBId` links, five corrections of the D115 class and one dangling document
reference that has been silently dropping a row from the Plan B screen.

### The six contingencies the itinerary tabulates but the package did not carry

| new plan B | hangs off |
| --- | --- |
| `planb.voo-corfu` — perder o voo das 07:15 para Corfu | `transport.ams-corfu.u2` |
| `planb.ferry-corfu-sarande` — ferry cancelado por vento | **nothing — see below** |
| `planb.embarque-ksamil` — perder o ônibus das 19:30 | `transport.ksamil-budva.night`, Dia 4 |
| `planb.nevidio-chuva` — chuva adia o Nevidio | `attr.zabljak.nevidio`, Dia 10 |
| `planb.trem-mostar` — perder o trem das 07:15 | `transport.sarajevo-mostar.train`, Dia 14 |
| `planb.voo-volta-dbv` — atraso do voo de volta | `transport.dubrovnik-zagreb.ou661`, Dia 18 |

**The ferry plan hangs off nothing, and that is a finding.** The Corfu → Sarandë
crossing is a timeline row with `refId: null` and **no `transport` entity**, and
Dia 3's single `planBId` is already spent on the Ksamil check-in. Screen 19
lists every plan B in `planBs` whatever points at it, so the traveller can reach
it; what it lacks is a route in from the leg it belongs to. Closing that means
modelling the ferry as a transport — more than a content session should do.

`planb.nevidio-chuva` carries **the first `kind: "phone"` action in the
package** (`tel:+38268001150`), which `PlanBScreen.kt:163` draws as a dial row.
The document's instruction for that contingency is to message the operator the
moment the forecast turns, and a plan whose first step is a telephone call
should offer the telephone.

### Four restaurants, and the table that deliberately did not become forty

| city | place | practicalNote (screen 04 joins it with priceNote) |
| --- | --- | --- |
| sarajevo | Morića Han | Não é espresso e não se toma rápido: reservem meia hora sentados |
| sarajevo | Zlatna Ribica | Passando de dia parece fechada e abandonada: insistam |
| kotor | Forza Kuk | No alto do Lovćen, nos dois planos do dia |
| kotor | Monte 1350 | No alto do Lovćen, ao lado da estação Kuk |

These are the only four places the itinerary names. Its *Comida* table lists
dishes and price bands by country — general knowledge about a cuisine, not
places with doors — and **none of it became a restaurant row**.

### Two scenarios rewritten, one reassurance that was simply wrong

- `planb.bobotov-kuk` said *"para a travessia do Bobotov Kuk"*, on a route the
  same package describes as out-and-back from Žabljak. Now: *"Se o tempo fechar
  no Bobotov Kuk, ou o dia parecer longo demais."*
- `planb.caiaque-dubrovnik` said *"por falta de participantes"* where the
  voucher says a **minimum of six**. Now: *"…ou por não juntar os seis
  participantes mínimos."*
- Its `reassurance` promised *"troca por caminhada guiada"*, which the operator
  does not offer. The document says another date or time, or a full refund —
  and now so does the field.

The other four scenarios read well and were left alone.

### The D115 rule found four more violations of itself, and one of mine

Sweeping the package for `dia N` in prose:

| where | before | after |
| --- | --- | --- |
| `acc.sarajevo.estudio.instructions` | "o código é liberado no **dia 22**" | **dia 10** |
| `acc.mostar.guesthouse.instructions` | "as 21:00 do **dia 26**" | **dia 14** |
| `planb.caiaque-dubrovnik.reassurance` | "a manhã do **dia 29**" | **dia 17** |
| `planb.caiaque-dubrovnik.steps[1].title` | "a manhã do **dia 29**" | **dia 17** |
| `attr.kotor.muralhas.practical.bestTime` | "o nascer do sol no **dia 18 de setembro**" | date removed |

Three of those five numbers are **greater than 20**, so on a screen headed
"Dia N de 20" they cannot be read as a day at all. The last was written in
phase 2 of this same migration — correct, since it said "de setembro", but
still a number offered to a reader who is counting days, and the sunrise hour
was the content.

### One dangling reference, found by an assertion rather than a validator

`planb.ksamil-checkin.relatedDocumentIds` named `doc.ticket.ferry-corfu-sarande`.
**That id has never existed** — the ferry is packaged per passenger, `.vinicius`
and `.erika`. `PlanBState` resolves the list with `mapNotNull`, so nothing
failed: the ferry ticket simply did not appear on the Plan B screen. Both real
ids are now listed.

`content_checks` validates `documentIds` on days, transports and stays but not
`relatedDocumentIds` on a plan B, which is why six validators passed over it.
Closing that gap is a change to `tools/`, which this session does not touch.

### Verified

- 6 validators rc=0; `check_repo.py` PASS; `test_validate_trip.py` 39 tests OK;
  `git diff --check` rc=0;
- an independent sweep of every id-bearing field the validator does not check
  (`relatedDocumentIds`, `attractionIds`, `storyIds`, `walkIds`, `transportIds`,
  `accommodationIds`, `usefulAppIds`, `criticalItemIds`) now reports **0
  dangling references**, against 1 before this commit;
- `content_preflight` **PASS 3 warning(s)** on `generated` and **PASS 8** on
  `assets/trip`, both unchanged;
- three copies identical except `contentStatus` (rc=0);
- Kotlin **357 tests, 0 failures**, from the 44 XML files in
  `app/build/test-results/testDebugUnitTest/`; `lintDebug` **0 errors, 33
  warnings**;
- both APKs carry **31 entries** under `assets/trip-production/`; release DEX
  `"Protótipo"` 0 and `"simular chegada"` 0;
- read back **from inside `app-release.apk`**: 12 plan Bs in the order screen 19
  draws them, 4 restaurants, all 9 new links, the phone action, the five date
  corrections and the repaired ferry reference;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## What the detailed itinerary carries that schema 1.1 has no field for

Recorded at the end of the migration so it becomes a schema decision after the
trip, not a forced field before it. Nothing below was invented into an
adjacent field.

- **Money, water, pharmacies, tipping, language, mines, smoking, street dogs,
  the Euronet trap, refusing DCC, what not to photograph.** All of it is
  per-country or trip-wide practical advice. `practicalInfo` exists **only
  inside `attraction`** — not on `city`, not at the top level — so there is
  nowhere to put a fact about Albania that is not about a particular sight.
- **The medical reference table by base**, including that Žabljak has only a
  clinic, with a hospital in Nikšić ~1h30 away and a full centre in Podgorica
  ~2h30. `emergencyProfile` is per country and carries contacts, not a note
  about how far the nearest hospital is from tonight's bed. No screen reads a
  profile note today either.
- **Photography**: which hour the light works at each place. Where it belongs
  to one attraction it went into `practical.bestTime`; the table as a whole,
  and `heroAssetId` for any of the 27, have no home — the package carries no
  image binaries at all.
- **The eighth useful app**, the bank's, which the document names without
  naming a bank (see D117).
- **The Bunski Kanali and the Fortica Sky Walk**, two stops the Herzegovina
  tour operator alone visits. They have no `city` in the package, and adding
  cities to carry two stops would change the 19-city shape for content the
  document gives two sentences.

## App identity: a launcher icon, and the name "Bálcãs" (2026-09-07)

No Kotlin, no dependency, no screen, no behaviour. `res/` and the manifest only.

### What changed

| file | change |
| --- | --- |
| `res/values/strings.xml` | `app_name`: "Travel Companion" → **Bálcãs** |
| `AndroidManifest.xml` | `+android:icon="@mipmap/ic_launcher"`, `+android:roundIcon="@mipmap/ic_launcher_round"` |
| `res/mipmap-anydpi-v26/ic_launcher.xml` | new, adaptive icon |
| `res/mipmap-anydpi-v26/ic_launcher_round.xml` | new, identical |
| `res/mipmap-{m,h,x,xx,xxx}dpi/ic_launcher_background.png` | new, 108/162/216/324/432 px |
| `res/mipmap-{m,h,x,xx,xxx}dpi/ic_launcher.png` | new legacy square, 48/72/96/144/192 px |
| `docs/design/app-icon-source.png` | the 3.2 MB master, now versioned (D121) |

`applicationId`, `namespace`, `versionCode` and `versionName` are untouched —
see D120 for why that is a data-safety decision and not a style one.

### Lint did not go 33 → 32. It went 33 → 40, and the eight are informative

`MissingApplicationIcon` is gone, which was the goal. Eight warnings arrived,
all of them direct consequences of path A and none of them errors:

| id | before | after | what it is |
| --- | --- | --- | --- |
| `MissingApplicationIcon` | 1 | **0** | the defect this commit fixes |
| `IconLauncherShape` | 0 | **5** | "Launcher icons should not fill every pixel of their square region" — once per legacy density |
| `MonochromeLauncherIcon` | 0 | **2** | no `<monochrome>` layer for Android 13 themed icons |
| `ObsoleteSdkInt` | 0 | **1** | `-v26` is redundant when `minSdkVersion` is 26 |
| everything else | 32 | 32 | unchanged |

**`IconLauncherShape` is worth reading as evidence, not as noise.** It is lint
saying, in its own words, what D121 measures: the art fills its square, and an
adaptive icon assumes it will not. It fires on the *legacy* PNG only; every
device that runs this app is API 26+ and resolves the XML instead.
`MonochromeLauncherIcon` needs a monochrome art layer, which is a commission
(path B). `ObsoleteSdkInt` is correct and one rename would silence it; the
`-v26` qualifier was kept because it is the universal convention and because
the brief specified that path.

### What the screenshot actually shows

Installed from `app-release.apk` on two emulators (Pixel-class, API 37, one at
1440×3120 and one at 720×1520, so two different mipmap densities are exercised).
Both render **the same circular mask**; no image on this SDK ships the
`com.android.theme.icon.*` overlays, so a squircle could not be tested on
device.

**The label** reads **Bálcãs** in the app drawer, one line, not truncated, with
the acute á and the tilde ã drawn correctly. `aapt2 dump badging` agrees:
`application-label:'Bálcãs'`.

**The crop, in words:**

- **The top of the pack is cut.** The circle slices across the pack's lid; the
  top edge of the flap is gone.
- **The upper-left shoulder strap is cut** at its outer end, mid-buckle.
- **The bedroll is cut**, and this is the most visible loss: only the upper
  right of the cream spiral survives, and it reads as a pale wedge rather than
  as a rolled blanket.
- **Both side edges of the pack body are gone**, so the pack has no silhouette
  — it runs off the mask on the left and the right.
- **The blue field survives as one sliver at the lower left**; the yellow halo
  as two slivers, left and upper right. The framing the painting was composed
  around is not in the icon.
- **The mug and the sprig survive intact** and are the two most legible objects.

**At size:** at 144 px and 96 px it reads clearly as a backpack. At 72 px it is
still a backpack. At **48 px it is a green mass with orange diagonal stripes and
a pale blob** — the pack's outline is what would carry it at that size, and the
outline is exactly what the mask removed.

The honest summary: it is unmistakably *this* app and unmistakably not the green
Android robot, which is the whole point of the commit. It is not a well-framed
icon, and the reason is structural, not a mistake in the crop. Path B (D121) is
the fix if the framing matters.

### Verified

- 6 validators rc=0; `check_repo.py` PASS; `test_validate_trip.py` 39 tests OK;
  `git diff --check` rc=0;
- Kotlin **357 tests, 0 failures**, from the 44 XML files in
  `app/build/test-results/testDebugUnitTest/`;
- `lintDebug` **0 errors**, 40 warnings (33 before; see the table above);
- both APKs still carry **31 entries** under `assets/trip-production/` — this
  commit touched `res/`, not `assets/`;
- release DEX `"Protótipo"` 0 and `"simular chegada"` 0;
- icon resources in the release APK, via `aapt2 dump resources`:
  `mipmap/ic_launcher` 5 PNG densities + `anydpi` XML,
  `mipmap/ic_launcher_background` 5 PNG densities,
  `mipmap/ic_launcher_round` `anydpi` XML — 10 PNGs and 2 XMLs;
  `application-icon-{120..640}` all resolve to the adaptive XML;
- `aapt2 dump badging`: `application-label:'Bálcãs'`, `versionCode='1'`,
  `versionName='0.1.0'`, `package: name='com.travelcompanion.app'`;
- `apksigner verify`: exit 0, `CN=Android Debug` (D110);
- `app/src/main/assets/trip/trip.json` and `trip-package/sample/sample-trip.json`
  both still `97627a8c8cda0c1126eacda352aff0b30e6427ce` — no content was touched.

## The icon art, recomposed, plus the themed-icon layer (2026-09-07)

No Kotlin, no dependency, no screen, no manifest, no content. `res/` and
`docs/design/` only. Follows 77e1832, which installed an icon whose art did not
fit the geometry.

### What changed

| | file | size |
| --- | --- | --- |
| replaced | `docs/design/app-icon-source.png` (1254², subject now 63%×60%) | 2,383,283 B |
| new | `docs/design/app-icon-monochrome-source.png` (1024², LA) | 139,950 B |
| replaced | `mipmap-{m,h,x,xx,xxx}dpi/ic_launcher_background.png` | 20,483 → 292,809 B |
| new | `mipmap-{m,h,x,xx,xxx}dpi/ic_launcher_monochrome.png` | 4,091 → 39,056 B |
| **deleted** | `mipmap-{m,h,x,xx,xxx}dpi/ic_launcher.png` | 6,681 → 82,408 B |
| edited | `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | 906 B each |

`AndroidManifest.xml` untouched. `applicationId`, `namespace`, `versionCode`,
`versionName`, `app_name` untouched.

### The premise that was wrong, and the number that proves the fix (D122)

An adaptive icon discards the outer third **before** masking: the guaranteed
area is the central 66dp of 108. The old art put the subject across 93% × 89%,
so the silhouette was not trimmed, it was removed. The new art puts it in
**63% × 60%**.

Measured on the packaged 432px layer:

| | old art | new art |
| --- | --- | --- |
| subject survives the 72/108 circle | ~71% | **99.86%** |
| subject survives a squircle | ~74% | **100%** |
| pixels the circle still clips | ~21,000 | **68** (0.036% of frame) |

Those 68 pixels graze the outer edge of the left shoulder strap in the
top-left and are invisible on the device.

### The legacy PNGs are gone, and the gate that allowed it

`minSdk` is 26, so `mipmap-anydpi-v26/` resolves everywhere this app can run.
Verified rather than assumed — with the five PNGs deleted:

```text
application-icon-120:'res/BW.xml'    application-icon-320:'res/BW.xml'
application-icon-160:'res/BW.xml'    application-icon-480:'res/BW.xml'
application-icon-240:'res/BW.xml'    application-icon-640:'res/BW.xml'
                                     application-icon-65534:'res/BW.xml'
```

All seven resolve to the adaptive XML. Those five files were the sole source of
the five `IconLauncherShape` warnings.

### Lint: 40 → 33, and only the two expected ids moved

| id | before | after |
| --- | --- | --- |
| `IconLauncherShape` | 5 | **0** |
| `MonochromeLauncherIcon` | 2 | **0** |
| `ObsoleteSdkInt` | 1 | 1 — kept on purpose (D122) |
| GradleDependency 11 · UseTomlInstead 9 · NewerVersionAvailable 6 · UseKtx 2 · AndroidGradlePluginVersion 2 · ModifierParameter 1 · InlinedApi 1 | 32 | 32 |

0 errors either way. No other id changed.

### What the screenshots showed

Release APK installed on two emulators (Pixel-class API 37, 1440×3120 and
720×1520, so two mipmap densities). Both render the same circular mask.

**Colour icon — the backpack is whole.** The top carry handle is complete and
clear of the mask. Both ends of the bedroll are inside: the spiral end at the
lower left and the strapped end at the lower right. The enamel mug hangs
complete on the left, the sprig is complete behind the flap, and both strap
runs with all four buckles are inside the circle. The pack has a silhouette
again — you can see where it ends and the background begins, which is exactly
what was missing before.

**The yellow halo became a frame.** It reads as a full ring behind the pack,
and it is the blue field outside it that the mask eats. Nothing of the subject
touches the boundary except the graze noted above.

**At size:** legible as a backpack at 48px — pack, straps, cream bedroll, halo,
blue ring all separate. In 77e1832 the same 48px was an unreadable green mass.

**Label** reads **Bálcãs**, one line, á and ã correct.
`aapt2 dump badging`: `application-label:'Bálcãs'`.

**Themed icon — tested, and it works.** Enabled through Wallpaper & style →
Home screen → Icons → Style → **Minimal** (this Android version's name for
themed icons). Confirmed applied: the dock dropped from ~0.7 mean saturation to
~0.21–0.31. Note that Pixel Launcher applies themed icons on the **home
screen** only — All Apps keeps colour icons — so the app was dragged to the home
screen to be seen.

The monochrome layer renders as a **tinted silhouette with its interior
drawn**, not a block: the carry handle, both strap runs, all the buckles, the
front pocket, the mug, the sprig leaves and the bedroll's spiral are each
readable as pale lines, because they are transparent in the alpha and the
system's light field shows through. That is the payoff of keeping detail in the
alpha rather than in opaque white.

**One thing left changed on the device:** themed icons are still enabled on
`emulator-5554`, and the app has a home-screen shortcut there. Blind `adb`
taps could not reliably drive the picker back to *Default*; it is one tap in
Wallpaper & style → Icons → Style. Nothing in the repository is affected.

### Verified

- 6 validators rc=0; `check_repo.py` PASS; `test_validate_trip.py` 39 tests OK;
  `git diff --check` rc=0;
- Kotlin **357 tests, 0 failures**, from the 44 XML files in
  `app/build/test-results/testDebugUnitTest/`;
- `lintDebug` **0 errors, 33 warnings** (40 before);
- both APKs still carry **31 entries** under `assets/trip-production/`;
- release DEX `"Protótipo"` 0 and `"simular chegada"` 0;
- release APK icon resources via `aapt2 dump resources`: `mipmap/ic_launcher`
  anydpi XML only, `ic_launcher_background` 5 PNG densities,
  `ic_launcher_monochrome` 5 PNG densities, `ic_launcher_round` anydpi XML;
- alpha re-checked on every packaged monochrome layer after the resize:
  min 0 / max 255, 77.8%–81.4% fully transparent — the channel survived;
- `apksigner verify` exit 0, `CN=Android Debug`;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Screen 17's "MOSTRE ESTA TELA" card, on six of seven profiles (2026-09-07)

Content only. No Kotlin, no `tools/`, no `res/`, no schema. The card was built
and tested and **no profile in the real package carried `showToSomeone`**, so it
would never have drawn on the trip. Six now do.

| profile | language | localLanguageText |
| --- | --- | --- |
| `emergency.ba` | Bosnian | Trebam pomoć. Molim vas, pozovite hitnu službu. |
| `emergency.me` | = Montenegrin | *identical* |
| `emergency.hr` | = Croatian | *identical* |
| `emergency.al` | Albanian | Kam nevojë për ndihmë. Ju lutem, telefononi shërbimin e urgjencës. |
| `emergency.gr` | Greek | Χρειάζομαι βοήθεια. Παρακαλώ, καλέστε τις υπηρεσίες έκτακτης ανάγκης. |
| `emergency.nl` | Dutch | Ik heb hulp nodig. Bel alstublieft de hulpdiensten. |
| `emergency.br` | — | **no card, deliberate (D123)** |

`translation` is the same Portuguese sentence on all six. Every card has
**exactly two fields**: `audioAssetId` is absent, not empty (D123).

### The identical trio is content, not the D102 defect

Bosnian, Montenegrin and Croatian are one language — the itinerary says so —
and the sentence a stranger reads is the same in Sarajevo, Kotor and Dubrovnik.
Three invented variants would be dialect added to decorate a data structure, on
the one screen where being understood is the point. **No validator or preflight
check fired on the repetition**, which was the thing to stop for; the recipe is
unchanged at 6 rc=0 / PASS 3 / PASS 8.

### One card that can never appear, and why it was written anyway

Screen 17 picks its profile from `day.baseCityId`. The countries that are ever
a base are AL, BA, BR, HR, ME, NL — **Greece is not one**: Corfu appears only in
Dia 3's `cityIds`, and that day's base is Ksamil. So the Greek card is
unreachable on screen, exactly as the Greek profile's own 112 and 166 already
were. Written because it is correct and costs nothing, not because it will be
seen.

### What the screenshots showed

Release APK on `emulator-5554`, date driven from Settings → Date & time (the
image has no root, so `date` from the shell is refused).

- **26/09, Dia 14, "Mostar · Bósnia e Herzegovina"** — the dark ink card is at
  the foot of the screen under the consular row. Teal eyebrow **MOSTRE ESTA
  TELA**, then the Bosnian sentence large and white, wrapping onto two lines,
  complete, with `ć` and `š` drawn correctly; under it the Portuguese in small
  muted type. Nothing truncated, no tofu boxes.
- **15/09, "Ksamil · Albânia"** — same card, Albanian sentence, all five `ë`
  rendering; two lines, complete.
- **29/09, "Čilipi · Croácia"** — **the same sentence as Bosnia**, which is the
  expected result and the point of D123.
- **13/09, "São Paulo · Brasil"** — **no card**. The screen ends after "Seguro
  viagem — assistência 24 h", as intended.
- **14/09, "Amsterdã · Países Baixos"** — added to the check because it costs
  one date change and covers a third language: the Dutch sentence renders in
  full.

Montenegro was not opened on the device; it carries the same string as Bosnia
and Croatia, both of which were seen, and it was read back from inside the APK.
Greece cannot be reached, per above.

### The three copies

Proved by breaking it: with `production` tampered to `"draft"` the comparison
reports `MISMATCH` and exits 1; restored, it exits 0.

```text
generated        contentStatus='draft'       expected='draft'       OK
production       contentStatus='production'  expected='production'  OK
trip-production  contentStatus='production'  expected='production'  OK
production       identical to generated (ignoring contentStatus): True
trip-production  identical to generated (ignoring contentStatus): True
rc=0
```

### Verified

- 6 validators rc=0; `check_repo.py` PASS; `test_validate_trip.py` 39 tests OK;
  `git diff --check` rc=0;
- `content_preflight` **PASS 3** on `generated` and **PASS 8** on `assets/trip`,
  both unchanged;
- Kotlin **357 tests, 0 failures**, from the 44 XML files in
  `app/build/test-results/testDebugUnitTest/`; `lintDebug` **0 errors, 33
  warnings**;
- both APKs carry **31 entries** under `assets/trip-production/`; release DEX
  `"Protótipo"` 0 and `"simular chegada"` 0;
- read back **from inside `app-release.apk`**: the trip.json there is
  byte-equal to `trip-package/production/trip.json`, 6 profiles carry the card,
  Brazil does not, and no `showToSomeone` carries `audioAssetId`;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`
  — the Bosnian sentence was copied *from* the prototype, never edited there.

## Screen 20 — "Comer aqui": schema, state, screen and route (2026-09-07)

No content. Not one dish is written into any package: the seventeen real dishes
are their own session, and everything here was developed against Kotlin
fixtures in `app/src/test/`.

### What was built

| layer | file |
| --- | --- |
| schema | `$defs/menu`, `$defs/meal`, `$defs/dish`; `menu` on `$defs/city`; `fallbackMenuCityId` at the top |
| models | `Menu`, `Meal`, `Dish` in `TripModels.kt`; `City.menu`; `TripPackage.fallbackMenuCityId` |
| state | `domain/food/MenuState.kt` — `cityOfDay`, `buildMenuState`, `buildFoodState`, `buildFoodShortcut`, `currentDayIndex` |
| screen | `feature/food/FoodScreen.kt` |
| icons | `TcIcons.Fork`, `TcIcons.ChevronLeft`, converted 1:1 from the handoff sprite |
| type | eight screen-20 styles in `TcType` (`mealTitle`, `dishName`, `pronunciation`, `priceTabular`, `metaTabular`, `historyBody`, `eyebrowSmall`, `orderPhrase`) |
| entry | `ShortcutUi.Kind.Food`, built in `TodayUseCase`, drawn in `TodayScreen` |
| route | `Routes.FOOD`, `FoodRoute` in `AppNavigation.kt` — `timelineDestination` untouched |
| tests | `MenuStateTest` (17), `MenuFixtures`; 8 new validator tests |

Signatures, and where the day resolves:

```kotlin
fun cityOfDay(content: TripContent, day: TripDay): City?          // baseCityId, then cityIds.first (D090)
fun buildMenuState(content: TripContent, cityId: String?): MenuUiState?
fun buildFoodState(content: TripContent, dayIndex: Int, locale: Locale): FoodUiState?
fun buildFoodShortcut(content: TripContent, date: LocalDate): FoodShortcutUi?
fun currentDayIndex(content: TripContent, date: LocalDate): Int
```

`buildFoodShortcut` takes a **date** and `buildFoodState` takes an **index**,
and they are deliberately not the same parameter (D089).

### The three states

1. the day's city has a menu → shown, no notice;
2. it has none and `fallbackMenuCityId` resolves to a city that does → that
   menu, with the amber notice, and with **its own** country and currency;
3. neither → header, working chevrons, and one sentence. The prototype does not
   draw this one (D129).

### Four corrections that preceded the handoff

- **Screen 20, not 11.** 11 is "Fim do passeio" (D124). `SCREEN-INDEX-v2.md`
  now lists 20.
- **No day numbers anywhere in the data.** The handoff's "dia 9", "dias 10–11",
  "Dia N de 21", Ohrid, MKD and RSD are all from another itinerary. The menu
  hangs off the city, so none of it needed correcting case by case (D125). The
  header total is `content.days.size`.
- **The fallback city is declared** (D126), not the first city that happens to
  carry a menu.
- **No photo, no photo area** (D128) — a deviation from the prototype, which
  always draws one.

### Two other deviations, both stated rather than silent

- **No bottom navigation.** The handoff §1 asks for one with "Explorar" active;
  in this app `showsBottomNav` is true only for the five root destinations, and
  screen 20 is a detail route reached from Today exactly like 05, 15, 16 and 17,
  none of which carry the bar. Adding one here would be a shell change, not a
  screen. Back returns to Today, which is what §1 actually needs.
- **Two colour substitutions.** The handoff's `#5C4114` notice text and
  `#3F6165` translation line have no exact token; the existing `GoldBody` and
  `TealDark` are used, which is what the brief asked for ("confira contra
  `FieldCompanionColors` e use os que já existem"). Everything else matched a
  token exactly, including `#F3E8D0`/`#E4D2AD`/`#684817` and
  `#E7EDE3`/`#3D5337`.

### Verified

- 6 validators rc=0, output unchanged — the schema only gained **optional**
  fields, so no existing package can regress;
- `check_repo.py` PASS, including its first-element guard over the new
  `domain/food/`;
- `test_validate_trip.py` **47 tests** (39 + 8 for the menu checks);
- Kotlin **374 tests, 0 failures** (357 + 17), counted from the 45 XML files in
  `app/build/test-results/testDebugUnitTest/`;
- `lintDebug` **0 errors, 33 warnings**, the same eight ids as before. One new
  hint appeared on the first run — `AutoboxingStateCreation` on the day cursor —
  and was fixed with `mutableIntStateOf` rather than left;
- `content_preflight` PASS 3 / PASS 8, unchanged;
- both APKs still carry **31 entries** under `assets/trip-production/`; release
  DEX `"Protótipo"` 0 and `"simular chegada"` 0;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

### What is left

- **The content.** Seventeen dishes for Sarajevo and Mostar, and a
  `fallbackMenuCityId`. Until one is written the screen is unreachable in the
  app — the shortcut only appears for a city with a menu — and that is the
  correct behaviour, not a gap.
- **The seventeen photographs.** Briefs exist in `photoCaption`; no binary
  does. The layout is already correct with and without them (D128).
- **The visual pass.** Not this session: with no content there is nothing to
  photograph on a device, and a screenshot of a fixture would be a picture of a
  test. It belongs to the content session, against the real menu.
- **A meal-order check.** The handoff fixes the order Café da manhã / Almoço /
  Jantar / Doce e café; the screen renders the order the content declares
  rather than re-sorting it, so the rule currently lives only in the content
  brief. A validator check is the right home for it and was left out rather
  than guessed at.

## A dish may have no ordering phrase (2026-09-07)

Four small changes, no content. `$defs/dish` required `phrase` and
`phraseTranslation`; the food guide has **two dishes of 75** that correctly have
neither — the half-board breakfast and lunch at the Bastasi rafting camp, where
the table arrives served and there is nothing to ask for. They could not be
represented at all, so the modelling was wrong, not the content (D130).

| # | change |
| --- | --- |
| 1 | schema: `phrase` and `phraseTranslation` leave `required`, keep their shape |
| 2 | `Dish.phrase` / `Dish.phraseTranslation` are `String? = null` |
| 3 | `DishUi` carries them nullable; `FoodScreen` draws no "Para pedir" box without a phrase — the card ends at the history |
| 4 | new guard: one half of the pair without the other is a content error |

The screen's rule is the one it already used for the absent photograph, and the
one `AssetResolver.packagedPathIfPresent` uses everywhere: absent data means an
absent element, never an empty box (D128).

### The guard, through the validator's exit code

Four packages built from `starter/` with `contentStatus: production`, so any
content finding is a hard error:

```text
both               rc=0
neither            rc=0
phrase-only        rc=1  city 'sarajevo' dish 'camp-cafe' declares phrase without
                         phraseTranslation; write both or neither
translation-only   rc=1  city 'sarajevo' dish 'camp-cafe' declares phraseTranslation
                         without phrase; write both or neither
```

### Verified

- 6 validators rc=0, output unchanged — a loosened `required` cannot make an
  existing package fail, and all six were re-run to confirm it;
- `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **51 tests** (47 + 4 for the pair);
- Kotlin **376 tests, 0 failures** (374 + 2), from the 45 XML files;
- `lintDebug` **0 errors, 33 warnings**, same eight ids;
- both APKs **31 entries** under `assets/trip-production/`; release DEX
  `"Protótipo"` 0 and `"simular chegada"` 0;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

No visual pass: with no menu content screen 20 is still unreachable in the app,
which is correct until the content lands.

## Menus, phase 1: Bosnia — and the first time screen 20 runs (2026-09-07)

Content only. Three menus, **20 dishes**, plus `fallbackMenuCityId: sarajevo`.
The food guide is now versioned at
`trip-package/source/itinerary/guia-gastronomico-balcas-2026.md` and is the
single source (D131).

| city | title | dishes | days that gain a menu |
| --- | --- | --- | --- |
| `sarajevo` | Nove pratos da mesa bósnia | 9 | Dia 12, Dia 13 |
| `mostar` | Nove pratos entre o rio Neretva e a herança turca | 9 | Dia 14, Dia 15 |
| `bastasi` | Dois pratos de meia pensão à beira do Tara | 2 | Dia 11 |

Nothing was retyped: the menus are parsed from the guide, and a verification
pass asserts every packaged string is a substring of the source. 75 ids are
unique across the package; zero `photoAssetId`.

### What the screenshots showed

Release APK on `emulator-5554`, clock set from Settings → Date & time.

**25/09, Dia 13, Sarajevo.** Screen 02 draws the row above "Gravar memória":
fork icon in teal, **"Comer em Sarajevo"**, **"9 pratos"**, chevron. Opening it:
header with a 48dp back arrow, "Comer em Sarajevo" over "Dia 13 de 20 · Sexta,
25 de setembro", both chevrons dark. Then the eyebrow **"CULINÁRIA LOCAL ·
BÓSNIA E HERZEGOVINA"** with the fork, the serif title "Nove pratos da mesa
bósnia", the intro, and **"Preços em marcos convertíveis (KM) · esta página fica
offline"**. Meal headings carry their time range right-aligned over a hairline.
**Every card starts at the dish name — there is no photo area at all**, which is
D128 on screen for the first time. Pronunciation renders in monospace between
slashes (`/bú-rek/`), and the two-pronunciation case renders as
`/sír-ni-tsa · ze-lia-ní-tsa/`. Each dish carries the teal "PARA PEDIR" box with
the sentence and its translation. The page ends on the prices note.

Meal headings seen directly: **Café da manhã**, **Jantar** and **Doce e café ·
qualquer hora**. The Almoço heading itself was never framed cleanly by the
scroll, but all three of its dishes (Ćevapi sarajevski, Begova čorba, Sarma)
appear in position between Café da manhã and Jantar.

**23/09, Dia 11, Bastasi — reached with the chevrons, not by resetting the
clock.** Two dishes, two meals, and **neither card carries a "PARA PEDIR"
block**: both end at the history, with no empty box and no pronunciation line.
The price pill reads "incluído na meia pensão". This is the case that motivated
`6c761b4`, drawn correctly the first time it was ever rendered.

**Dia 1, São Paulo — the fallback.** The amber notice is the first element of
the scroll: *"Cardápio próprio de São Paulo ainda não escrito. Mostrando os
pratos de Sarajevo como referência."* Below it the page is entirely Sarajevo's —
eyebrow "BÓSNIA E HERZEGOVINA", title "Nove pratos da mesa bósnia", and **"Preços
em marcos convertíveis (KM)"**, not Brazil and not reais. Only the header strip
names the real day. The **left chevron is greyed** at the first day, keeping its
space. That is D127 proved on a device.

**The cursor does not leak.** After walking from Dia 13 down to Dia 1 inside
screen 20 and pressing back, screen 02 still reads **"Comer em Sarajevo · 9
pratos"** — D089 held.

**Diacritics** render intact throughout: `Ćevapi`, `Begova čorba`, `Baščaršija`,
`Hurmašice`, `Sudžuk`, `manhã`. No empty boxes.

### Verified

- 6 validators rc=0 — including the checks added for menus: duplicate dish id,
  unknown or menu-less fallback city, and half a phrase pair;
- three copies identical except `contentStatus`, proved by tampering
  `production` to `"draft"` (rc=1) and restoring (rc=0);
- `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **51 tests OK**; Kotlin **376 tests, 0 failures** from
  45 XML files; `lintDebug` **0 errors, 33 warnings**;
- both APKs **31 entries** under `assets/trip-production/`; release DEX
  `"Protótipo"` 0 and `"simular chegada"` 0;
- read back **from inside `app-release.apk`**: 3 menus (sarajevo 9, mostar 9,
  bastasi 2), 20 dishes, `fallbackMenuCityId` = `sarajevo`, exactly 2 dishes
  without a `phrase` — both Bastasi — and 0 `photoAssetId`;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Menus, phase 2: Amsterdã, Ksamil, Kotor (2026-09-07)

Content only. Three more menus, **26 dishes**, taking the package to 6 menus and
46 dishes. Same parser, same verbatim check, no schema or code change.

| city | title | dishes | currency | days |
| --- | --- | --- | --- | --- |
| `amsterdam` | Oito pratos entre arenque cru e panqueca doce | 8 | euros (€) | Dias 2, 18, 19, 20 |
| `ksamil` | Nove pratos entre o Jônico e a herança otomana | 9 | lekë (ALL) | Dias 3, 4 |
| `kotor` | Nove pratos da baía veneziana | 9 | euros (€) | Dias 5, 6, 7 |

### What the screenshots showed

**14/09, Dia 2** — screen 02 draws "Comer em Amsterdã · 8 pratos". The page reads
"CULINÁRIA LOCAL · PAÍSES BAIXOS", "Oito pratos entre arenque cru e panqueca
doce", and **"Preços em euros (€) · esta página fica offline"**.

**16/09, Dia 4** — "Comer em Ksamil · 9 pratos"; the page reads "CULINÁRIA LOCAL
· ALBÂNIA" and **"Preços em lekë (ALL) · esta página fica offline"**, with the
`ë` intact.

**18/09, Dia 6** — "Comer em Kotor · 9 pratos"; "CULINÁRIA LOCAL · MONTENEGRO",
"Nove pratos da baía veneziana", **"Preços em euros (€)"**. Diacritics in the
intro (`pašticada`, `Njeguši`, `Lovćen`) all render.

Three countries, three currency lines, each taken from the menu on screen.

### Verified

- 6 validators rc=0; three copies identical except `contentStatus` (rc=0);
- `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **51 tests OK**; Kotlin **376 tests, 0 failures** from
  45 XML files; `lintDebug` **0 errors, 33 warnings**;
- both APKs **31 entries** under `assets/trip-production/`; release DEX clean;
- from inside `app-release.apk`: **6 menus, 46 dishes**, all ids unique, still
  exactly 2 dishes without a `phrase` (both Bastasi), 0 `photoAssetId`;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Menus, phase 3: Žabljak, Dubrovnik, Čilipi, Budva — and the set is complete (2026-09-07)

Content only. Four menus, **29 dishes**. The package now carries **10 menus and
all 75 dishes of the guide**, and **19 of the 20 days** have a menu of their own.

| city | title | dishes | days |
| --- | --- | --- | --- |
| `zabljak` | Nove pratos das terras altas de Durmitor | 9 | Dias 8, 9, 10 |
| `dubrovnik` | Nove pratos da antiga república marítima | 9 | Dia 16 |
| `cilipi` | Dois pratos da konoba rural de Konavle | 2 | Dia 17 |
| `budva` | Nove pratos entre a pesca da manhã e o café turco | 9 | **none — see D132** |

Only **Dia 1**, São Paulo on the evening of the flight out, falls back — and it
borrows Sarajevo, which is what `fallbackMenuCityId` declares.

### What the screenshots showed

**21/09 Dia 9** "Comer em Žabljak · 9 pratos"; **28/09 Dia 16** "Comer em
Dubrovnik · 9 pratos"; **29/09 Dia 17** "Comer em Čilipi · 2 pratos". `Ž` and
`Č` render in the row labels.

**Čilipi opened**: "CULINÁRIA LOCAL · CROÁCIA", "Dois pratos da konoba rural de
Konavle", "Preços em euros (€)", and then **only two meal headings — Almoço and
Jantar**. No empty Café da manhã and no empty Doce e café: a meal with no dish
is not drawn (handoff §4).

**Budva has no shortcut on any day, as expected.** On **17/09, Dia 5** — the day
the trip actually passes through Budva, arriving at 11:07 and leaving for Kotor
at 18:00 — screen 02 reads **"Comer em Kotor · 9 pratos"**, because Dia 5's
`baseCityId` is Kotor. Budva's nine dishes are in the package and reachable from
no day (D132).

### Verified

- 6 validators rc=0; three copies identical except `contentStatus` (rc=0);
- `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **51 tests OK**; Kotlin **376 tests, 0 failures** from
  45 XML files; `lintDebug` **0 errors, 33 warnings**;
- both APKs **31 entries** under `assets/trip-production/`; release DEX clean;
- from inside `app-release.apk`: **10 menus, 75 dishes**, all ids unique,
  `fallbackMenuCityId` = `sarajevo`, exactly **2** dishes without a `phrase`
  (both Bastasi), **0** `photoAssetId`, and **19 of 20 days** resolving to their
  own city's menu;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

### What is left

The **seventeen photographs** — now seventy-five briefs, one `photoCaption` per
dish, and no binary for any of them. The layout is already correct without them
(D128), and the day they arrive nothing above or below a card moves.

## Screen 05 draws its editorial sections (2026-09-07)

No content, no schema, no package. `attraction.historySections` had been parsed
and read by nobody since the models existed; screen 05 now carries it (D133).

Proof of the defect, before the change:

```text
$ grep -rn "historySections" app/src/main/java --include=*.kt
TripModels.kt:140:    val historySections: List<EditorialSection> = emptyList(),
TripModels.kt:213:    val historySections: List<EditorialSection> = emptyList(),
```

Two model declarations and no reader.

### What was built

- `EditorialSectionUi(title, paragraphs)` and
  `AttractionUiState.historySections: List<EditorialSectionUi>`, filled in
  `buildAttractionState` — empty list when absent, never a meaningful null;
- `paragraphsOf`, which splits a body on the blank line (`\r\n` as well as
  `\n`, runs of any length counting as one break) so a three-paragraph section
  is three `Text`s rather than one slab;
- `EditorialSectionBlock` in `AttractionScreen`, drawn **between the summary and
  the operational strip**, title in `TcType.sectionTitle` / `Ink` and each
  paragraph in `TcType.editorialBody` / `Neutral700`. No new type style, no new
  colour.

Order on screen: hero, chips, summary, **sections**, operational strip, actions,
what to observe, Plan B, fixed bar.

`SCREEN-INDEX-v2.md` was **not** touched: it carries a one-line purpose for
screen 05 and no block-level structure, so there was nothing to add a block to
and inventing one would have been worse than leaving it.

### Not touched, on purpose

`interestingFacts` is also parsed and read by nobody, and also has no place on
the approved sheet — left exactly as it was, and named in D133 so the next
reader finds a decision rather than a second discovery. `city.historySections`
and screen 04 likewise: same shape, no city content this round.

### Verified

- 6 validators rc=0, output unchanged; `check_repo.py` PASS;
  `test_validate_trip.py` **51 tests OK**, unchanged;
- Kotlin **381 tests, 0 failures** (376 + 5), from the 45 XML files;
- `lintDebug` **0 errors, 33 warnings**, the same eight ids;
- both APKs **31 entries** under `assets/trip-production/`; release DEX
  `"Protótipo"` 0 and `"simular chegada"` 0;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

**No visual pass.** All 27 attractions in the real package are section-less, so
screen 05 draws today exactly what it drew yesterday — which is what the
regression test asserts. The first look belongs to the content session.

## Audio guide, phase 1: coordinates on all 27 attractions (2026-09-07)

Content only. The audio guide is now versioned at
`trip-package/source/itinerary/audioguia-balcas-2026.md` and is the source for
this phase and the two that follow: 50 entries, of which **3 are marked
`**Não usar:**` and dropped whole** (D134), leaving 47 — 27 enriching existing
attractions and 20 becoming new ones.

**All 27 existing attractions gained `location.geo`.** `name` and `mapsQuery`
were left exactly as they were.

### The coordinate guard, first run against real content

```text
Coordinates: 6 city cluster(s) checked, 8 point(s) with nothing to anchor them to
- checked city 'amsterdam' (2 coordinates)
- checked city 'dubrovnik' (4 coordinates)
- checked city 'kotor' (4 coordinates)
- checked city 'mostar' (3 coordinates)
- checked city 'sarajevo' (7 coordinates)
- checked city 'zabljak' (3 coordinates)
- no coordinate check for city 'bastasi' … (single coordinate) ×8
```

**Nothing accused.** Note this is six cities checked and not the eight the brief
expected: eight cities hold exactly one point each, and a lone point has nothing
in its own city to be compared with. Phase 3 adds attractions to four of them.

### What the screenshot showed

**25/09, Dia 13** → Explorar → Sarajevo → Baščaršija. The card draws exactly what
it drew before: hero, summary, the 09:00 departure strip, the two map buttons,
"O que observar" with its three numbered notes, and the fixed "Iniciar passeio"
bar. **A coordinate changes no layout**, which is the point of checking.

What it does change is one of the two buttons, and only one:

| button | before | after |
| --- | --- | --- |
| Como chegar | `google.navigation:q=Bascarsija%20Sarajevo` | `google.navigation:q=43.8594,18.4326` |
| Abrir no mapa | `…/maps/search/?api=1&query=Bascarsija+Sarajevo` | **unchanged** |

"Abrir no mapa" reads the attraction's own `actions` array, which this phase did
not touch — recorded in D134 rather than fixed here.

### Verified

- 6 validators rc=0; three copies identical except `contentStatus`, proved by
  tampering `production` to `"draft"` (rc=1) and restoring (rc=0);
- `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **51 tests OK**; Kotlin **381 tests, 0 failures** from
  45 XML files; `lintDebug` **0 errors, 33 warnings**;
- both APKs **31 entries** under `assets/trip-production/`; release DEX clean;
- from inside `app-release.apk`: 27 attractions, **27 with `location.geo`**,
  3 with `historySections` (the three written in an earlier session), 3 sections;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Audio guide, phase 2: 76 editorial sections, and screen 05 shows them (2026-09-07)

Content only. All 27 existing attractions now carry `historySections` — **76
sections**, two or three each, verbatim from the audio guide. `summary`,
`whatToObserve`, `practical`, `actions` and `subtitle` were not touched.

Three attractions had a single section each from the detailed itinerary;
those are **replaced** by the audio guide's two or three (D135).

### The paragraph separation, checked after packaging

```text
sections total:                     76
sections with >1 paragraph:         50 of 76
bodies with a stray single newline:  0
```

Zero is the number that matters: a body joined with `\n` instead of a blank line
renders as one slab and no validator would notice (D135).

### What the screenshots showed — the first time a section has ever appeared

**Baščaršija (3 sections).** Directly under the summary card: **"O bazar que
fundou a cidade"** as a serif heading, then **three paragraphs with real vertical
space between them** — not one block. Below it "Incêndio de 1697, Áustria em
1878" and "Sob fogo no cerco, de pé hoje", each with its own paragraphs. Then,
**below all of it and clearly separated, the 09:00 departure strip** — no
editorial text touching the operational block, which is the D133 rule holding on
a real page.

**Lago Negro (2 sections).** Same shape with two headings; the second, "Parque de
1952, montanha do trabalhador", carries two paragraphs, again visibly apart.
This card has no walk, so the order runs sections → map buttons → "O que
observar". `Međed` renders with its đ.

### Verified

- 6 validators rc=0; three copies identical except `contentStatus` (rc=0);
- `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **51 tests OK**; Kotlin **381 tests, 0 failures** from
  45 XML files; `lintDebug` **0 errors, 33 warnings**;
- both APKs **31 entries** under `assets/trip-production/`; release DEX clean;
- from inside `app-release.apk`: 27 attractions, 27 with `location.geo`,
  **27 with `historySections`, 76 sections**, 50 multi-paragraph, 0 collapsible;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Audio guide, phase 3: twenty new attractions (2026-09-07)

Content only. The package goes from **27 to 47 attractions**, all 47 with a
coordinate and with editorial sections — **127 sections** in total, which is
every one the audio guide wrote.

| city | before | after | new ids |
| --- | --- | --- | --- |
| amsterdam | 2 | **6** | `rijksmuseum`, `museu-holocausto`, `museu-resistencia`, `museu-van-gogh` |
| kotor | 4 | **5** | `forte-sao-joao` |
| zabljak | 3 | **4** | `ledena-pecina` |
| bastasi | 1 | **2** | `sipcanica` |
| sarajevo | 3 | **7** | `morica-han`, `chama-eterna`, `catedral`, `rosas` |
| dubrovnik | 4 | **13** | `portao-pile`, `lokrum`, `porto-velho`, `forte-lovrijenac`, `palacio-do-reitor`, `museu-maritimo`, `mosteiro-franciscano`, `museu-rupe`, `forte-imperial` |

All prefixed `attr.<city>.`. No `practical` and no `subtitle` on any of the 20 —
the source has neither (D136).

### The coordinate guard, with 20 more points

```text
Coordinates: 7 city cluster(s) checked, 7 point(s) with nothing to anchor them to
- checked city 'amsterdam' (6 coordinates)
- checked city 'bastasi' (2 coordinates)
- checked city 'dubrovnik' (13 coordinates)
- checked city 'kotor' (5 coordinates)
- checked city 'mostar' (3 coordinates)
- checked city 'sarajevo' (11 coordinates)
- checked city 'zabljak' (4 coordinates)
```

Nothing accused, with Dubrovnik at 13 points and Sarajevo at 11.

### What the screenshots showed

**Explorar to Dubrovnik** lists 13 attractions. Screen 04 draws them in a
**horizontal carousel**, so thirteen is a sideways scroll and not a wall — the
ninth card takes eight swipes to reach. Recorded as a screen-04 question in
D136 rather than solved by cutting content.

**Palácio do Reitor** — hero with "DUBROVNIK · CROÁCIA" and the name and **no
strapline**, the summary card, then three sections ("Um chefe de Estado por
trinta dias", "Duas explosões e um terremoto", "Museu de História Cultural"),
each with its paragraphs apart, then the two map buttons, then "O que observar"
with exactly three numbered items. **No chip row**, because there is no
`practical` — the card is honest about what the source gave it.

**Ledena Pećina** (Žabljak, one of the three approximate coordinates) — the same
shape, two sections, `Ž` and `ć` intact.

### Verified

- 6 validators rc=0; three copies identical except `contentStatus` (rc=0);
- `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **51 tests OK**; Kotlin **381 tests, 0 failures** from
  45 XML files; `lintDebug` **0 errors, 33 warnings**;
- both APKs **31 entries** under `assets/trip-production/`; release DEX clean;
- from inside `app-release.apk`: **47 attractions, 47 with `location.geo`, 47
  with `historySections`, 127 sections**, all ids unique, 20 without
  `practical` or `subtitle`, and **no dangling `city.attractionIds`**;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Audio guide, phase 4: six bodies that carried the document (2026-09-07)

Content only, and a correction to phases 2 and 3. The section parser ended a
body at the next `### ` and the next `## ` but **not at `# `**, the guide's
country heading, so the **last section of every country block** ran past its own
end and swallowed the `---` rule, the country title after it and — in the last
block — the document footer, the whole `## Dúvidas` list and the whole
`## O que mudou` changelog. Six sections, always an attraction's last:

| attraction | section | before | after | source |
| --- | --- | --- | --- | --- |
| `attr.amsterdam.museu-van-gogh` | Rietveld, Kurokawa e a cronologia | 4 | **2** | 2 |
| `attr.butrinto.sitio` | Escavações italianas, quatro impérios numa trilha | 4 | **2** | 2 |
| `attr.sarajevo.vijecnica` | Vinte anos até reabrir em 2014 | 4 | **2** | 2 |
| `attr.pocitelj.vila` | Guerra de 1993, romãs em setembro | 4 | **2** | 2 |
| `attr.bastasi.sipcanica` | Banho, fotos e a escala do cânion | 3 | **1** | 1 |
| `attr.dubrovnik.forte-imperial` | Museu de 2008 e a vista | 9 | **2** | 2 |

Every body is truncated at its first paragraph beginning `---`. Nothing else in
the package changed. `attr.bastasi.sipcanica` leaves **one** paragraph, because
the guide writes that section as one (D137).

### The check that replaces the substring check

Phases 2 and 3 verified that every packaged paragraph was a substring of the
source, and **every contaminated paragraph was** — the changelog really is in
the guide, just not under that `###`. The new check compares, for all **127
sections**, the packaged paragraph list with the source's list under the same
heading, **count and text, element by element**. Run before the fix as well as
after, so it is known to fail on the defect:

```text
before:  121 identical, 6 mismatched, 0 unresolvable   (rc=1)
after:   127 identical, 0 mismatched, 0 unresolvable   (rc=0)
```

**Stray single newlines: 0** in every body of all three copies. The two that
existed were inside the swallowed markdown lists — list-item separators, not
paragraph breaks and not mid-sentence breaks — and the truncation removed them
with the lists.

### Two things the swallowed section was hiding

- **`attr.sarajevo.catedral` and `attr.sarajevo.rosas` share `43.8592, 18.4258`**
  — the package's only duplicated coordinate. It comes verbatim from source
  entries 26 and 27, and entry 27 explains it: *"a mais visível, diante da
  catedral"*. Left exactly as it is; **no coordinate invented**. Open for
  Vinícius (D138). The pair the source itself doubted is a different one —
  entries 24/25 — and entry 24 is a `Não usar`, so it never entered the package.
- **Tekke de Blagaj, Bektashi × Naqshbandi.** The guide doubts its own line;
  the text is packaged unchanged in `attr.blagaj.tekke`, section *"Um mosteiro
  onde o rio nasce"*. Open for Vinícius (D139).

### What the screenshot showed

Release APK on the emulator, clock at **28/09/2026** — Dia 16, Dubrovnik.
(29/09 is Dia 17 and resolves to **Čilipi**, which has no attractions.)
Explorar → Dubrovnik → thirteenth card → **Forte Imperial e Museu da Guerra da
Pátria**. The last section, *"Museu de 2008 e a vista"*, ends at *"…ver tudo
daqui era uma questão de vida ou morte para quem defendia a cidade lá embaixo."*
and the map buttons follow immediately. **No `---`, no `## Dúvidas`, no
changelog.**

### Verified

- 6 validators rc=0; three copies identical except `contentStatus` (rc=0);
- `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **51 tests OK**; Kotlin **381 tests, 0 failures** from
  45 XML files; `lintDebug` **0 errors, 33 warnings**;
- both APKs **31 entries** under `assets/trip-production/`;
- read back from inside `app-release.apk`: **47 attractions, 127 sections, 127
  identical to the source by count and text, 0 stray newlines, 0 bodies holding
  a rule or a heading**;
- both tracked `trip.json` blobs still `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Two defects from review: an alarm a day early, and a walk that never spoke (2026-09-07)

Content only. No Kotlin, no `tools/`, no schema, no `res/`. Two defects found in
review, both in the real package, both fixed in the three copies
(`trip-package/generated/`, `trip-package/production/`,
`app/src/main/assets/trip-production/`). D140, D141 and D142 record them; D078
and D116 are corrected in place.

### A1 — the Podgorica alarm rang twenty-four hours early

`ci.podgorica-conexao` (`actionByTime` **09:20**, *"Segunda perna sai de
Podgorica às 09:38"*) lived on `transport.ksamil-budva.night`. That transport is
named in `transportIds` of **Dia 4 (16/09) alone**, and
`TripRepository.criticalItemsFor` collects a day's items from
`day.criticalItems`, `day.transportIds` and `day.accommodationIds` — so the
connection was harvested on Dia 4 and `criticalAlerts` built the instant as
day-date + `actionByTime` + zone: **`2026-09-16T09:20` in `Europe/Tirane`**. The
boarding is on **17/09 às 09:38**. The critical card also stood on screen 02 of
the wrong day.

The item moved **whole** into Dia 5's `criticalItems` (`2026-09-17`,
`Europe/Podgorica`) and left the transport — no copy in both, because D012
merges duplicates rather than dropping them and the Dia 4 occurrence would have
survived as the earlier of the two. `ci.nightbus-ksamil` **stays on the
transport**: its 19:15 is genuinely a Dia 4 deadline. The Dia 5 timeline row
`d05.podgorica` at 05:00 was **not touched** and keeps its `criticalItemIds`.

### The arithmetic proof, replayed over the package

A script in the scratchpad replays `criticalAlerts`: for every day it unions
`day.criticalItems` with the items of its `transportIds` and
`accommodationIds`, merges by id, applies `zoneOverrides`, builds
`date + actionByTime + zone`, and keeps the earliest occurrence of each id.

Before:

```text
ci.anne-frank            2026-09-14T15:25+0200[Europe/Amsterdam]   Day(day02)
ci.gate-corfu            2026-09-15T06:45+0200[Europe/Amsterdam]   Transport(transport.ams-corfu.u2)
ci.checkin-ksamil        2026-09-15T15:30+0200[Europe/Tirane]      Stay(acc.ksamil.guesthouse)
ci.podgorica-conexao     2026-09-16T09:20+0200[Europe/Tirane]      Transport(transport.ksamil-budva.night)      <-- wrong day
ci.nightbus-ksamil       2026-09-16T19:15+0200[Europe/Tirane]      Transport(transport.ksamil-budva.night)
ci.checkin-kotor         2026-09-17T20:00+0200[Europe/Podgorica]   Stay(acc.kotor.suranj)
ci.bus-zabljak           2026-09-20T06:40+0200[Europe/Podgorica]   Transport(transport.kotor-zabljak.bus)
ci.canyoning             2026-09-22T10:15+0200[Europe/Podgorica]   Day(day10)
ci.train-mostar          2026-09-26T06:50+0200[Europe/Sarajevo]    Transport(transport.sarajevo-mostar.train)
ci.tour-herzegovina      2026-09-27T09:20+0200[Europe/Sarajevo]    Day(day15)
ci.bus-dubrovnik         2026-09-28T06:30+0200[Europe/Sarajevo]    Transport(transport.mostar-dubrovnik.bus)
ci.caiaque               2026-09-28T12:45+0200[Europe/Zagreb]      Day(day16)
ci.bagdrop-dubrovnik     2026-09-30T04:45+0200[Europe/Zagreb]      Transport(transport.dubrovnik-zagreb.ou661)
total: 13 prazos
```

After:

```text
ci.anne-frank            2026-09-14T15:25+0200[Europe/Amsterdam]   Day(day02)
ci.gate-corfu            2026-09-15T06:45+0200[Europe/Amsterdam]   Transport(transport.ams-corfu.u2)
ci.checkin-ksamil        2026-09-15T15:30+0200[Europe/Tirane]      Stay(acc.ksamil.guesthouse)
ci.nightbus-ksamil       2026-09-16T19:15+0200[Europe/Tirane]      Transport(transport.ksamil-budva.night)
ci.podgorica-conexao     2026-09-17T09:20+0200[Europe/Podgorica]   Day(day05)                                   <-- right day, target changed
ci.checkin-kotor         2026-09-17T20:00+0200[Europe/Podgorica]   Stay(acc.kotor.suranj)
ci.bus-zabljak           2026-09-20T06:40+0200[Europe/Podgorica]   Transport(transport.kotor-zabljak.bus)
ci.canyoning             2026-09-22T10:15+0200[Europe/Podgorica]   Day(day10)
ci.train-mostar          2026-09-26T06:50+0200[Europe/Sarajevo]    Transport(transport.sarajevo-mostar.train)
ci.tour-herzegovina      2026-09-27T09:20+0200[Europe/Sarajevo]    Day(day15)
ci.bus-dubrovnik         2026-09-28T06:30+0200[Europe/Sarajevo]    Transport(transport.mostar-dubrovnik.bus)
ci.caiaque               2026-09-28T12:45+0200[Europe/Zagreb]      Day(day16)
ci.bagdrop-dubrovnik     2026-09-30T04:45+0200[Europe/Zagreb]      Transport(transport.dubrovnik-zagreb.ou661)
total: 13 prazos
```

**Twelve of the thirteen keep their date and their minute exactly.** One moved,
and it is the one that was wrong.

### The cost, recorded rather than hidden

`ownersOf` maps only the items a day's transports and stays declare, so the item
now has no owner: the alert's target falls from
`AlertTarget.Transport(transport.ksamil-budva.night)` to
`AlertTarget.Day(day05)`, and the notification opens **the day** instead of the
transport sheet. Accepted (D140) — Dia 5 carries the 05:00 row that describes
the wait, the 4h38 and the second operator — but it is a behaviour change and it
is written down as one.

### The root cause, which is not a content fix (D142)

`CriticalAlerts.zoneOverrides` reads `timelineItem.criticalItemIds`;
`TripRepository.criticalItemsFor`, which decides what a day collects at all,
does not. So a timeline row can correct an item's **zone** and cannot correct
its **date**, and any deadline belonging to a different day than the entity
carrying it will repeat this defect silently. Fixing it is Kotlin plus tests —
an implementation session, not this one, seven days from departure. Named in
D142 so it is not rediscovered from a wrong alarm on the road.

### A2 — no story played by itself on the real walk

`StoryTriggering.kt:88` plays inside the walk only when
`walk.automaticStories && chosen.trigger?.autoPlayInWalk == true`. The walk
declares no `automaticStoriesDefault` and the schema default is **`true`**, so
the left side held; **no story declared `autoPlayInWalk`** and that field's
schema default is **`false`**, so the right side never did. Every real arrival
became a notification — which contradicts *"Walk Mode assumes headphones +
screen off"*, because a notification has to be taken out of a pocket and read.

`story.sarajevo.sebilj`, `story.sarajevo.encontro-de-culturas` and
`story.sarajevo.ponte-latina` now carry `"autoPlayInWalk": true` in their
`trigger`.

**The consequence, named:** with auto-play on, the offered case cannot happen in
the real package, so **screen 10 is unreachable again** — the content gap of
D065, and the correct behaviour. Screen 10 is reachable only through D031's
debug-only arrival scaffold, on an emulator, and never on the trip. **D078 is
corrected in place**: its "what is not reachable" sentence claimed every
packaged story declared `autoPlayInWalk`, which described a package that had
stopped existing — the note-that-describes-a-vanished-state defect, this time
inside DECISIONS itself. **D116(3) is corrected too**: it reasoned about the Dia
5 timeline row's zone and never about the critical item, which is the half that
was wrong.

### The three copies

Unchanged invariant, proved by breaking it: with `production`'s `contentStatus`
tampered to `"draft"` the comparison reports `MISMATCH` and exits 1; restored,
it exits 0.

```text
generated        contentStatus='draft'       expected='draft'       OK
production       contentStatus='production'  expected='production'  OK
trip-production  contentStatus='production'  expected='production'  OK
production       identical to generated (ignoring contentStatus): True
trip-production  identical to generated (ignoring contentStatus): True
rc=0
```

**None of the three is in the commit** — all are gitignored (D027, D028). This
commit is documentation only; the content changes live in the working copy and
in the APK built from it.

### What the screenshots showed

`app-release.apk` installed on `emulator-5554` (API 37, 1440×3120), clock driven
from Settings → Date & time — these images have no root, so `adb shell date` is
refused, and `cmd time_detector suggest_manual_time` is refused too, for want of
`SUGGEST_MANUAL_TIME_AND_ZONE`. Both screenshots are outside the repository, in
this session's scratchpad:

- **16/09/2026, 09:00 — `02-hoje-16set-0900-sem-podgorica.png`.** "Ksamil ·
  Albânia", *Dia 4 de 20*, "Ksamil → Butrinto → ônibus noturno". Exactly **one**
  NÃO PODE DAR ERRADO card: **19:30, "Ônibus noturno para Budva às 19:30"**.
  **No Podgorica card.**
- **17/09/2026, 09:00 — `02-hoje-17set-0900-com-podgorica.png`.** "Kotor ·
  Montenegro", *Dia 5 de 20*, "Budva → Kotor", the 05:00 "Troca de ônibus em
  Podgorica" row in the ink card, and **two** NÃO PODE DAR ERRADO cards — the
  first **09:38, "Segunda perna sai de Podgorica às 09:38"**, the check-in card
  below it.

**And the device agreed with the arithmetic.** With the emulator's time zone
moved to Europe/Podgorica and the clock at 17/09 09:00, D112's scheduler log
prints the instant itself:

```text
I TravelCompanion: alarm scheduled id=ci.podgorica-conexao at=2026-09-17T09:20:00+02:00[Europe/Podgorica] mode=exact
I TravelCompanion: alarms scheduled total=9 canBeExact=true
```

### Verified

- 6 validators rc=0; three copies identical except `contentStatus` (rc=0), and
  that check proved by breaking it (rc=1 tampered, rc=0 restored);
- `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **51 tests OK**; Kotlin **381 tests, 0 failures** from
  45 XML files; `lintDebug` **0 errors, 33 warnings**; `git diff --check` clean;
- both APKs **31 entries** under `assets/trip-production/`; release DEX
  `"Protótipo"` 0 and `"simular chegada"` 0;
- read back from inside both APKs: `transport.ksamil-budva.night.criticalItems`
  is `[ci.nightbus-ksamil]`, `days[4]` (2026-09-17, `Europe/Podgorica`) carries
  `[ci.podgorica-conexao]`, and all three stories report `autoPlayInWalk: true`;
- both tracked `trip.json` blobs still
  `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Audio guide, phase 5.1: the fifty are converted and measured (2026-09-07)

Content only, and no `trip.json` touched in this phase — conversion and
measurement alone. 50 WAVs of authoring input (mono, 24 kHz, 16 bit, 194 MB,
70.6 min) became 50 `.m4a` under `trip-package/generated/audio/`: **47** in
`attractions/`, **3** in `stories/`, over the three that were there.

```text
ffmpeg -i <in>.wav -c:a aac -b:a 64k -ac 1 -movflags +faststart <out>.m4a
```

**The bitrate is reported as measured, not as requested.** `-b:a 64k` is the
ask; FFmpeg's native AAC encoder overshoots it on this material, and the result
is **68.1–68.5 kbps of AAC stream, 68.6–70.0 kbps of file** (D143). The three
files being replaced were ~69 kbps too, so nothing about the package's audio
weight changes in kind.

**Every duration below was read from the final `.m4a`**, from its `mvhd` atom,
through `validate_trip.audio_duration_seconds` — the same reader the validator
uses to check the number back. Nothing estimated from the WAV, the word count,
or ffmpeg's own console line. WAV and m4a agree to **0.000 s on all fifty**, so
the conversion neither trimmed nor padded.

Filenames are `<cidade>-<slug>.m4a`, not `<slug>.m4a`: the bare slug yields 45
names for 47 attractions — `casco-antigo` in Budva and Kotor, `muralhas` in
Kotor and Dubrovnik — and would have silently overwritten two guides (D144).
The ids are unaffected and are exactly `ag.<cidade>.<slug>`.

### The fifty, measured

| # | destino | wav s | m4a s | kB |
| ---: | --- | ---: | ---: | ---: |
| 1 | `ag.amsterdam.jordaan` | 109.25 | 109.25 | 925 |
| 2 | `ag.amsterdam.anne-frank` | 124.65 | 124.65 | 1049 |
| 3 | `ag.amsterdam.rijksmuseum` | 97.28 | 97.28 | 825 |
| 4 | `ag.amsterdam.museu-holocausto` | 95.12 | 95.12 | 802 |
| 5 | `ag.amsterdam.museu-resistencia` | 87.28 | 87.28 | 737 |
| 6 | `ag.amsterdam.museu-van-gogh` | 83.78 | 83.78 | 707 |
| 7 | `ag.ksamil.ilhotas` | 99.88 | 99.88 | 845 |
| 8 | `ag.butrinto.sitio` | 101.15 | 101.15 | 859 |
| 9 | `ag.budva.casco-antigo` | 80.78 | 80.78 | 676 |
| 10 | `ag.kotor.muralhas` | 100.75 | 100.75 | 861 |
| 11 | `ag.kotor.forte-sao-joao` | 86.10 | 86.10 | 734 |
| 12 | `ag.kotor.casco-antigo` | 96.88 | 96.88 | 819 |
| 13 | `ag.kotor.ladder` | 89.58 | 89.58 | 763 |
| 14 | `ag.kotor.teleferico-lovcen` | 97.78 | 97.78 | 829 |
| 15 | `ag.zabljak.lago-negro` | 86.00 | 86.00 | 726 |
| 16 | `ag.zabljak.ledena-pecina` | 71.40 | 71.40 | 608 |
| 17 | `ag.zabljak.bobotov-kuk` | 107.80 | 107.80 | 909 |
| 18 | `ag.zabljak.nevidio` | 94.42 | 94.42 | 799 |
| 19 | `ag.bastasi.rafting-tara` | 100.38 | 100.38 | 849 |
| 20 | `ag.bastasi.sipcanica` | 65.92 | 65.92 | 557 |
| 21 | `ag.sarajevo.bascarsija` | 101.40 | 101.40 | 857 |
| 22 | `story.sarajevo.sebilj` | 67.38 | 67.38 | 576 |
| 23 | `ag.sarajevo.morica-han` | 66.95 | 66.95 | 567 |
| 24 | `story.sarajevo.encontro-de-culturas` | 75.05 | 75.05 | 632 |
| 25 | `ag.sarajevo.chama-eterna` | 71.80 | 71.80 | 608 |
| 26 | `ag.sarajevo.catedral` | 70.75 | 70.75 | 604 |
| 27 | `ag.sarajevo.rosas` | 81.15 | 81.15 | 688 |
| 28 | `story.sarajevo.ponte-latina` | 95.47 | 95.47 | 811 |
| 29 | `ag.sarajevo.war-childhood` | 63.92 | 63.92 | 542 |
| 30 | `ag.butmir.tunel` | 99.20 | 99.20 | 844 |
| 31 | `ag.sarajevo.vijecnica` | 99.22 | 99.22 | 842 |
| 32 | `ag.mostar.stari-most` | 103.58 | 103.58 | 875 |
| 33 | `ag.mostar.kujundziluk` | 65.75 | 65.75 | 557 |
| 34 | `ag.mostar.koski-mehmed-pasha` | 55.45 | 55.45 | 470 |
| 35 | `ag.blagaj.tekke` | 88.08 | 88.08 | 742 |
| 36 | `ag.kravice.cachoeiras` | 75.78 | 75.78 | 641 |
| 37 | `ag.pocitelj.vila` | 94.17 | 94.17 | 790 |
| 38 | `ag.dubrovnik.portao-pile` | 66.65 | 66.65 | 566 |
| 39 | `ag.dubrovnik.lokrum` | 85.17 | 85.17 | 720 |
| 40 | `ag.dubrovnik.caiaque` | 56.67 | 56.67 | 475 |
| 41 | `ag.dubrovnik.cidade-velha` | 84.97 | 84.97 | 720 |
| 42 | `ag.dubrovnik.porto-velho` | 87.40 | 87.40 | 742 |
| 43 | `ag.dubrovnik.muralhas` | 107.35 | 107.35 | 908 |
| 44 | `ag.dubrovnik.forte-lovrijenac` | 73.28 | 73.28 | 614 |
| 45 | `ag.dubrovnik.palacio-do-reitor` | 72.58 | 72.58 | 611 |
| 46 | `ag.dubrovnik.museu-maritimo` | 45.90 | 45.90 | 389 |
| 47 | `ag.dubrovnik.mosteiro-franciscano` | 67.47 | 67.47 | 572 |
| 48 | `ag.dubrovnik.museu-rupe` | 50.92 | 50.92 | 432 |
| 49 | `ag.dubrovnik.teleferico-srd` | 74.42 | 74.42 | 627 |
| 50 | `ag.dubrovnik.forte-imperial` | 110.22 | 110.22 | 930 |

**50 files · 34.99 MB · 70.6 min.** Shortest 45.90 s
(`ag.dubrovnik.museu-maritimo`), longest 124.65 s
(`ag.amsterdam.anne-frank`).

Entries **22, 24 and 28** are the three the document marks `**Não usar:** já
existe como história com áudio`; their audio replaces the three Sarajevo story
guides and creates no attraction. The new lengths — **67.38 s, 75.05 s,
95.47 s** against the 141/149/162 the package declares — are the whole point:
the old voice narrated at 70–81 words a minute, the new one at 130–180.

### The guard fired, with real data, for the first time

Overwriting `generated/audio/stories/` while `generated/trip.json` still
declares the old lengths is exactly the case D095 exists for, and it did not
have to be contrived:

```text
FAIL: 3 audio duration/chapter error(s) in trip-package/generated/trip.json
- audioGuide 'ag.sarajevo.sebilj': declares durationSeconds 141 but audio/stories/sarajevo-sebilj.m4a is 67.4s (off by 73.6s, tolerance 2s)
- audioGuide 'ag.sarajevo.encontro-de-culturas': declares durationSeconds 149 but audio/stories/sarajevo-encontro-de-culturas.m4a is 75.0s (off by 74.0s, tolerance 2s)
- audioGuide 'ag.sarajevo.ponte-latina': declares durationSeconds 162 but audio/stories/sarajevo-ponte-latina.m4a is 95.5s (off by 66.5s, tolerance 2s)
rc=1
```

`production` and `assets/trip-production` still hold the old files and still
pass; phase 5.2 promotes the new ones, watches all three go red, and then
closes them. **This is the expected state at the end of this phase, not a
regression** — the phase was defined as conversion without touching any
`trip.json`, and the only honest way to end it is with the validator saying so.

### Verified

- `check_repo.py` PASS; **5 of 6 validators rc=0**, `generated` rc=1 by design
  (above);
- `content_preflight` PASS 3 / PASS 8, unchanged; `test_validate_trip.py`
  **51 tests OK**; `git diff --check` clean;
- Kotlin **381 tests, 0 failures**; `lintDebug` **0 errors, 33 warnings**;
- both APKs **28 entries** under `assets/trip-production/` (27 PDF +
  `trip.json`); `app-release.apk` **26.8 MB**, `app-debug.apk` **31.9 MB** —
  unchanged from the start of the phase, because no audio is promoted yet and
  `assets/trip-production/audio/stories/` is still empty. *(The 31.8 MB this
  session was handed as the starting size is the **debug** APK; the release one
  has always been smaller. Both are measured here rather than carried over.)*
- release DEX `"Protótipo"` 0;
- `source-wav/` untouched: **50 WAVs**, and **no `.wav` anywhere under
  `production/` or `assets/trip-production/`**;
- both tracked `trip.json` blobs still
  `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Audio guide, phase 5.2: the three stories, and the guard's first real firing (2026-09-07)

Content only. The three new `.m4a` were promoted from `generated/` into
`production/audio/stories/` and into
`app/src/main/assets/trip-production/audio/stories/`, which had been left
**empty** on purpose before this session — so the three `Ouvir` buttons were
absent, by `AssetResolver` checking for the file and dropping the button in
silence (D021), and the APK carried 28 entries instead of 31.

### Proved by failing, on all three copies

The files were promoted **before** the numbers were corrected, which is the
whole point:

```text
--- trip-package/generated/trip.json
FAIL: 3 audio duration/chapter error(s) in trip-package/generated/trip.json
- audioGuide 'ag.sarajevo.sebilj': declares durationSeconds 141 but audio/stories/sarajevo-sebilj.m4a is 67.4s (off by 73.6s, tolerance 2s)
- audioGuide 'ag.sarajevo.encontro-de-culturas': declares durationSeconds 149 but audio/stories/sarajevo-encontro-de-culturas.m4a is 75.0s (off by 74.0s, tolerance 2s)
- audioGuide 'ag.sarajevo.ponte-latina': declares durationSeconds 162 but audio/stories/sarajevo-ponte-latina.m4a is 95.5s (off by 66.5s, tolerance 2s)
rc=1
--- trip-package/production/trip.json          (identical three findings)  rc=1
--- app/src/main/assets/trip-production/trip.json (identical three findings) rc=1
```

Then `durationSeconds` became **67, 75 and 95** — `round()` of the measured
value, not a target:

```text
--- trip-package/generated/trip.json
PASS: trip-package/generated/trip.json validates against trip-package/schema/trip.schema.json
Audio: 3 guide(s) timed against a packaged file, 0 not timed
rc=0
--- trip-package/production/trip.json             PASS · Audio: 3 timed, 0 not timed · rc=0
--- app/src/main/assets/trip-production/trip.json PASS · Audio: 3 timed, 0 not timed · rc=0
```

This is the first time D095's guard has fired on real content (D146). It was
written in September against a hypothetical and had never seen a declared
duration be wrong; here it was wrong by more than a minute on all three, and
`durationSeconds` — not the file — is what draws the progress bar and the
duration label.

### The body is now what the voice says

`autoPlayInWalk: true` since `a61fa8b` makes the audio the primary experience
of the walk, and the `body` was a different text about the same place. It is
now the audio guide entry, verbatim from `audioguia-balcas-2026.md`, entries
22, 24 and 28 — **every paragraph verified as an exact substring of the
source**, paragraphs separated by a blank line, **no stray single newlines**.
`title` and `hook` are untouched, and story title still differs from guide
title in all three. `readDurationMinutes` is 2, 2, 2 (was 2, 2, 3). The
replaced text is kept whole in D147; what it loses is the second-person
register written for someone standing in front of the thing.

| guia | antes | agora | arquivo medido | história | readDurationMinutes |
| --- | ---: | ---: | ---: | --- | ---: |
| `ag.sarajevo.sebilj` | 141 | **67** | 67.38 s | `story.sarajevo.sebilj` | 2 (era 2) |
| `ag.sarajevo.encontro-de-culturas` | 149 | **75** | 75.05 s | `story.sarajevo.encontro-de-culturas` | 2 (era 2) |
| `ag.sarajevo.ponte-latina` | 162 | **95** | 95.47 s | `story.sarajevo.ponte-latina` | 2 (era 3) |

### What was heard

**Nobody listened.** This session drives an emulator over `adb`, and there is no
audio path back from it, so nothing here claims how the new voice sounds, whether
it stumbles on `Baščaršija`, or whether it is an improvement. What follows is
what the device did, plus one identity that pins down *which* audio it was.

**Release APK on `emulator-5554`, clock at 25/09 (Dia 13, Sarajevo).**

- Explorar → Sarajevo → **HISTÓRIAS CURTAS**: three cards, each labelled
  **LEITURA DE 2 MIN** — *Ponte Latina* said 3 before — and each carrying
  **Ouvir** and **Ler**. The *Ouvir* buttons are back; they were absent while
  `assets/trip-production/audio/stories/` was empty, which is D021 working.
- **Ouvir** on the Sebilj story: the compact player shows **Sebilj,
  Baščaršija** over **O Sebilj tem 1891; a praça tem 1462** — the guide's title
  and the story's, two different lines, which is the pair D102 exists to keep
  apart — and the counter reads **00:03 / 01:07**, not 02:21. The media session
  reports `state=PLAYING(3)` with `buffered position=67375`, the file's own
  67.375 s to the millisecond. Left alone it reached **01:07 / 01:07**, so it
  decodes end to end.
- **Ler** expands the body in place, and what appears is the transcript: *"O
  Sebilj é o quiosque de madeira e pedra no centro da praça de Baščaršija, com
  um telhado em forma de cúpula e torneiras de bronze…"* — the same sentences
  the audio guide entry carries. Read and heard are one text now, which is the
  whole point of D147.
- **Airplane mode**, `airplane_mode_on=1` with a guide playing: position
  2974 → 8991 → 15001 ms, uninterrupted. Nothing about this audio was ever on
  the network, and now it is proved from the aeroplane state rather than from
  the architecture.

**Debug APK on `emulator-5556`, same date, for the arrival only.**

On this AVD the automatic arrival **cannot be produced on the release build**.
`appops set --uid 2000 android:mock_location allow` plus `cmd location providers
set-test-provider-location` does move the system provider — screen 07 goes from
*Procurando localização* to *Localização ativa* — but Play Services'
`FusedLocationProviderClient`, which `FusedLocationSource` uses, never delivers
it, and `adb emu geo fix` answers `OK` and changes nothing. So the arrival was
shown through D031's debug scaffold, which feeds the packaged coordinate into
the same `WalkModeController.onLocation` a GPS fix uses and therefore exercises
the real decision rather than a way around it.

- *Protótipo · simular chegada num ponto de história* → the media session goes
  to `state=PLAYING(3)` at `position=0`: **the story starts by itself**.
- **Screen 10 does not rise.** Screen 07 stays and advances to **CONTINUE · Do
  Sebilj, siga pela Sarači…**, which is D078's composition and D141's
  consequence, seen on the real package for the first time.
- **No notification is posted.** The app holds exactly two, before and after:
  id **1001**, `template=MediaStyle category=transport`, the
  `MediaSessionService`'s own (D018), and id **2001**, `category=navigation`
  `ONGOING_EVENT|SILENT`, the walk's. No third one appears — which is the
  difference between `PlayInWalk` and `Notify`, and the reason the invariant
  reads *headphones + screen off*.
- **Screen off**, `mWakefulness=Asleep`: position **27005 → 33011 ms** with the
  display asleep. The phone can go in a pocket.

**Which audio actually played.** The `sha256` of all three `.m4a` is the same in
`generated/`, in `production/`, in `assets/trip-production/` and **inside
`app-release.apk`** — `896ff4a7…`, `bd48cb16…`, `15e219f7…`. So the bytes that
played are the bytes converted from the WAV of entry 22, and what the voice says
is the audio guide entry by construction. How it sounds is Vinícius's to judge.

### Observed, not fixed

`app/src/debug/.../WalkArrivalScaffold.kt` carries D078's old sentence in its
KDoc — *"every story in it declares `autoPlayInWalk` and the walk declares
`automaticStoriesDefault`"* — which was false from the first real package until
`a61fa8b` and is true again now, but for a reason the comment does not give. It
is Kotlin and out of this session's scope; named here so an implementation
session can align it with the corrected D078.

### Verified

- 6 validators rc=0; three copies identical except `contentStatus` (rc=0);
- `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **51 tests OK**; Kotlin **381 tests, 0 failures** from 45 XML files; `lintDebug`
  **0 errors, 33 warnings**; `git diff --check` clean;
- both APKs **31 entries** under `assets/trip-production/` — 27 PDF +
  `trip.json` + **3 audio**, up from 28; `app-release.apk` **28.8 MB (was 26.8)**,
  `app-debug.apk` **33.9 MB (was 31.9)**;
- release DEX `"Protótipo"` 0;
- no `.wav` under `production/` or `assets/trip-production/`;
  `audio/scripts/` and `audio-manifest.json` promoted nowhere and absent from
  both APKs;
- both tracked `trip.json` blobs still
  `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Audio guide, phase 5.3: forty-seven attraction guides, and the manifest (2026-09-07)

Content only. Each of the 47 attractions gets an `asset`, an `audioGuide` and an
`audioGuideId`, and the 47 `.m4a` are promoted into `production/audio/attractions/`
and `app/src/main/assets/trip-production/audio/attractions/`. The package goes
from **3 audio guides to 50**, and from 3 audio assets to 50.

For each one, and nothing else:

```json
{ "id": "asset.ag.dubrovnik.palacio-do-reitor", "type": "audio",
  "path": "audio/attractions/dubrovnik-palacio-do-reitor.m4a",
  "mimeType": "audio/mp4" }

{ "id": "ag.dubrovnik.palacio-do-reitor", "title": "Palácio do Reitor",
  "audioAssetId": "asset.ag.dubrovnik.palacio-do-reitor",
  "durationSeconds": 73 }
```

`durationSeconds` is `round()` of the file's own measured length — 72.575 s
here — and the validator now reports **`Audio: 50 guide(s) timed against a
packaged file, 0 not timed`**, up from 3. **No guide has `chapters`** (D149).
Titles name the place, from the entry heading without its editorial annotation
(D148); two of them deliberately name something narrower than the attraction
they hang on, because the document does.

### The forty-seven

| # | `audioGuide` | title | s | atração |
| ---: | --- | --- | ---: | --- |
| 1 | `ag.amsterdam.jordaan` | Jordaan e os canais | 109 | `attr.amsterdam.jordaan` |
| 2 | `ag.amsterdam.anne-frank` | Casa de Anne Frank | 125 | `attr.amsterdam.anne-frank` |
| 3 | `ag.amsterdam.rijksmuseum` | Rijksmuseum | 97 | `attr.amsterdam.rijksmuseum` |
| 4 | `ag.amsterdam.museu-holocausto` | Museu Nacional do Holocausto | 95 | `attr.amsterdam.museu-holocausto` |
| 5 | `ag.amsterdam.museu-resistencia` | Museu da Resistência (Verzetsmuseum) | 87 | `attr.amsterdam.museu-resistencia` |
| 6 | `ag.amsterdam.museu-van-gogh` | Museu Van Gogh | 84 | `attr.amsterdam.museu-van-gogh` |
| 7 | `ag.ksamil.ilhotas` | Ksamil e as ilhotas | 100 | `attr.ksamil.ilhotas` |
| 8 | `ag.butrinto.sitio` | Butrinto | 101 | `attr.butrinto.sitio` |
| 9 | `ag.budva.casco-antigo` | Casco antigo de Budva | 81 | `attr.budva.casco-antigo` |
| 10 | `ag.kotor.muralhas` | Muralhas de Kotor | 101 | `attr.kotor.muralhas` |
| 11 | `ag.kotor.forte-sao-joao` | Forte de São João (Sveti Ivan) | 86 | `attr.kotor.forte-sao-joao` |
| 12 | `ag.kotor.casco-antigo` | Casco antigo de Kotor e Catedral de São Trifão | 97 | `attr.kotor.casco-antigo` |
| 13 | `ag.kotor.ladder` | Ladder of Kotor | 90 | `attr.kotor.ladder` |
| 14 | `ag.kotor.teleferico-lovcen` | Teleférico Kotor–Lovćen e Alpine Coaster | 98 | `attr.kotor.teleferico-lovcen` |
| 15 | `ag.zabljak.lago-negro` | Lago Negro (Crno Jezero) | 86 | `attr.zabljak.lago-negro` |
| 16 | `ag.zabljak.ledena-pecina` | Ledena Pećina, a caverna de gelo | 71 | `attr.zabljak.ledena-pecina` |
| 17 | `ag.zabljak.bobotov-kuk` | Bobotov Kuk | 108 | `attr.zabljak.bobotov-kuk` |
| 18 | `ag.zabljak.nevidio` | Cânion Nevidio | 94 | `attr.zabljak.nevidio` |
| 19 | `ag.bastasi.rafting-tara` | Rafting no cânion do Tara | 100 | `attr.bastasi.rafting-tara` |
| 20 | `ag.bastasi.sipcanica` | Cachoeira Šipčanica | 66 | `attr.bastasi.sipcanica` |
| 21 | `ag.sarajevo.bascarsija` | Baščaršija | 101 | `attr.sarajevo.bascarsija` |
| 23 | `ag.sarajevo.morica-han` | Morića Han | 67 | `attr.sarajevo.morica-han` |
| 25 | `ag.sarajevo.chama-eterna` | Chama Eterna (Vječna vatra) | 72 | `attr.sarajevo.chama-eterna` |
| 26 | `ag.sarajevo.catedral` | Catedral do Sagrado Coração | 71 | `attr.sarajevo.catedral` |
| 27 | `ag.sarajevo.rosas` | Rosas de Sarajevo | 81 | `attr.sarajevo.rosas` |
| 29 | `ag.sarajevo.war-childhood` | War Childhood Museum | 64 | `attr.sarajevo.war-childhood` |
| 30 | `ag.butmir.tunel` | Túnel da Guerra (Tunel spasa) | 99 | `attr.butmir.tunel` |
| 31 | `ag.sarajevo.vijecnica` | Vijećnica | 99 | `attr.sarajevo.vijecnica` |
| 32 | `ag.mostar.stari-most` | Stari Most | 104 | `attr.mostar.stari-most` |
| 33 | `ag.mostar.kujundziluk` | Bazar Kujundžiluk | 66 | `attr.mostar.kujundziluk` |
| 34 | `ag.mostar.koski-mehmed-pasha` | Mesquita Koski Mehmed Paxá e o minarete | 55 | `attr.mostar.koski-mehmed-pasha` |
| 35 | `ag.blagaj.tekke` | Tekke de Blagaj | 88 | `attr.blagaj.tekke` |
| 36 | `ag.kravice.cachoeiras` | Cachoeiras de Kravice | 76 | `attr.kravice.cachoeiras` |
| 37 | `ag.pocitelj.vila` | Počitelj | 94 | `attr.pocitelj.vila` |
| 38 | `ag.dubrovnik.portao-pile` | Portão Pile e a Baía de Pile | 67 | `attr.dubrovnik.portao-pile` |
| 39 | `ag.dubrovnik.lokrum` | Ilha de Lokrum | 85 | `attr.dubrovnik.lokrum` |
| 40 | `ag.dubrovnik.caiaque` | Caverna Betina | 57 | `attr.dubrovnik.caiaque` |
| 41 | `ag.dubrovnik.cidade-velha` | Stradun (Placa) | 85 | `attr.dubrovnik.cidade-velha` |
| 42 | `ag.dubrovnik.porto-velho` | Porto velho | 87 | `attr.dubrovnik.porto-velho` |
| 43 | `ag.dubrovnik.muralhas` | Muralhas da cidade velha | 107 | `attr.dubrovnik.muralhas` |
| 44 | `ag.dubrovnik.forte-lovrijenac` | Forte Lovrijenac | 73 | `attr.dubrovnik.forte-lovrijenac` |
| 45 | `ag.dubrovnik.palacio-do-reitor` | Palácio do Reitor | 73 | `attr.dubrovnik.palacio-do-reitor` |
| 46 | `ag.dubrovnik.museu-maritimo` | Forte de São João e Museu Marítimo | 46 | `attr.dubrovnik.museu-maritimo` |
| 47 | `ag.dubrovnik.mosteiro-franciscano` | Mosteiro Franciscano e a farmácia antiga | 67 | `attr.dubrovnik.mosteiro-franciscano` |
| 48 | `ag.dubrovnik.museu-rupe` | Museu Etnográfico Rupe | 51 | `attr.dubrovnik.museu-rupe` |
| 49 | `ag.dubrovnik.teleferico-srd` | Teleférico do Monte Srđ | 74 | `attr.dubrovnik.teleferico-srd` |
| 50 | `ag.dubrovnik.forte-imperial` | Forte Imperial e Museu da Guerra da Pátria | 110 | `attr.dubrovnik.forte-imperial` |

### The manifest

`audio-manifest.json` covered three story scripts with `ttsVoice: null`. It now
covers **all fifty**, and the voice is **`pm_alex`** — given by Vinícius in this
session and recorded exactly as given. The TTS provider was not stated and is
not invented; what a `.m4a` can prove about itself is still only the encoder
(FFmpeg), which is the step after the TTS. Per entry it records the source
document and entry number, the source WAV, the target path, `durationSeconds`
and `durationSecondsMeasured`, and the byte size; once, at the top, the exact
command, the ffmpeg build, the **requested 64 kbps against the measured 68.1–68.5
of stream and 68.6–70.0 of file**, and that every duration was read from the
final file's `mvhd` atom. The three story entries keep `supersededScriptPath`
pointing at the narration scripts they replaced, with a note that those are no
longer the origin of the audio (D150).

### What was heard

Same caveat as phase 5.2, and it is not a formality: **nobody listened.** There
is no audio path back from an emulator over `adb`. What follows is what the
device did.

**Release APK on `emulator-5554`, clock at 28/09 (Dia 16, Dubrovnik).**

- Explorar → Dubrovnik: every card in **O QUE VER** now carries an **Audioguia**
  badge beside **Offline** — badges that were absent an hour ago because no
  attraction had a guide.
- **Palácio do Reitor** (the ninth Dubrovnik card): the header reads **Audioguia
  2 min · Salvo offline**, and the action at the foot of the editorial sections
  is **▶ Ouvir audioguia · 2 min**. Tapping it plays: `state=PLAYING(3)`,
  `buffered position=72575` — the file's own 72.575 s to the millisecond — and
  the counter runs **00:06 / 01:12**.
- **The player prints the same line twice.** Under the pause button:
  **Palácio do Reitor** in bold, and **Palácio do Reitor** again in muted type
  beneath it. The media session agrees: `description=Palácio do Reitor, Palácio
  do Reitor`. This is D102's shape, arriving through a door D102 never guarded —
  the subtitle is the *attraction's* name, and an attraction is already named
  after its place, so it equals the guide's title on **40 of the 47**. Found by
  looking, not predicted; recorded as D151, and the fix is in the player, not in
  the content.

**Same build, clock at 14/09 (Dia 2, Amsterdã).**

- Explorar → Amsterdã → **Casa de Anne Frank**: **Audioguia 3 min · Salvo
  offline**, action **Ouvir audioguia · 3 min**, and it plays —
  `state=PLAYING(3)` on the longest file in the package (124.65 s).
- **Airplane mode** with it playing: `airplane_mode_on=1`, position
  3005 → 9014 → 15028 ms, uninterrupted. 50 guides, 35 MB, none of it on the
  network.

**What is still unheard.** Whether the narration is good, whether `pm_alex`
handles `Vijećnica` and `Šipčanica`, whether any of the 50 has a defect in the
middle that a duration check cannot see: none of that was checked, and none of
it is claimed. What is established is that all 50 files are in both APKs, that
each one's declared length matches its own bytes within D095's tolerance, and
that two of them decode and play on a device.

### Verified

- 6 validators rc=0, and `Audio: 50 guide(s) timed against a packaged file, 0
  not timed` on all three copies of the real package; three copies identical
  except `contentStatus` (rc=0);
- `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **51 tests OK**; Kotlin **381 tests, 0 failures** from 45 XML files**; `lintDebug`
  **0 errors, 33 warnings**; `git diff --check` clean;
- both APKs **78 entries** under `assets/trip-production/` — 27 PDF +
  `trip.json` + **50 audio**, from 31 at the end of phase 5.2 and 28 at the
  start of the session; `app-release.apk` **61.8 MB (was 28.8)**, `app-debug.apk`
  **66.9 MB (was 33.9)**;
- release DEX `"Protótipo"` 0;
- **47 of 47 attractions carry an `audioGuideId`**, 50 audio assets, 50 guides,
  **0 with `chapters`**, and every id unique;
- `source-wav/` untouched at **50 WAVs**, and nothing from it promoted: **no
  `.wav`, no `audio-manifest.json` and no `audio/scripts/` under
  `production/`, under `assets/trip-production/`, or anywhere under
  `assets/trip-production/` in either APK. The one `.wav` either APK does
  contain is `assets/trip/audio/attractions/bascarsija.prototype.wav`, the
  sample package's labelled synthetic placeholder (D019), which predates this
  session and is untouched;
- both tracked `trip.json` blobs still
  `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Explorar follows the day, not the bed (2026-09-07)

Kotlin and tests only. No package touched, no content written, and the six
validators say the same thing they said this morning.

### The defect

`Routes.EXPLORE` built screen 04 for `baseCityId ?: cityIds.first()` — the
resolution D090 fixed for screens 02, 03, 17 and 20, where the base is right
because it is where the traveller sleeps. Explorar asks a different question
and got the same answer:

| dia | dorme em | o dia é | Explorar mostrava |
| ---: | --- | --- | --- |
| 17 | Čilipi (0 atrações) | Dubrovnik, 13 atrações e as muralhas | Čilipi |
| 15 | Mostar | Blagaj, Kravice e Počitelj | Mostar |
| 12 | Sarajevo | o rafting no Tara, em Bastasi | Sarajevo |

**Twelve of the twenty days carry more than one city with content**, and
`cityIds` cannot break the tie: it includes the city the day *left* — day 3
lists `amsterdam` first, and Amsterdam is behind by the time the day lands.

### The rule

The timeline is the only field in the package that says where a day *happens*.
So, stopping at the first that resolves:

1. the city of the first timeline row whose `refId` names an attraction or a
   walk, if that city has content;
2. the base, if it has content;
3. the first city of the day that has any;
4. the base regardless — the floor, and what the screen did before.

A city "has content" when it declares any of `attractionIds`, `storyIds`,
`walkIds` or a `menu`. Corfu, Sarandë and Zagreb declare none and appear
nowhere. **Below two such cities no band is drawn at all** (D153).

### The whole trip, as the implementation resolves it

Asserted row by row in `ExplorePackagedDaysTest`, against the operational
package:

| dia | data | chips | abre em | dorme em | por qual regra |
| ---: | --- | ---: | --- | --- | --- |
| 1 | 13/09 | — | `sao-paulo` | `sao-paulo` | **piso** (nenhuma cidade com conteúdo) |
| 2 | 14/09 | — | `amsterdam` | `amsterdam` | timeline |
| 3 | 15/09 | 2 | `ksamil` | `ksamil` | base (nenhuma linha cita atração) |
| 4 | 16/09 | 2 | **`butrinto`** | `ksamil` | **timeline** |
| 5 | 17/09 | 2 | `kotor` | `kotor` | base |
| 6 | 18/09 | — | `kotor` | `kotor` | timeline |
| 7 | 19/09 | — | `kotor` | `kotor` | base |
| 8 | 20/09 | 2 | `zabljak` | `zabljak` | base |
| 9 | 21/09 | — | `zabljak` | `zabljak` | base |
| 10 | 22/09 | — | `zabljak` | `zabljak` | base |
| 11 | 23/09 | 2 | `bastasi` | `bastasi` | base |
| 12 | 24/09 | 2 | **`bastasi`** | `sarajevo` | **timeline** (09:00, o rafting) |
| 13 | 25/09 | 2 | `sarajevo` | `sarajevo` | timeline |
| 14 | 26/09 | 2 | `mostar` | `mostar` | base |
| 15 | 27/09 | 4 | **`blagaj`** | `mostar` | **timeline** (11:00, a primeira parada) |
| 16 | 28/09 | 2 | `dubrovnik` | `dubrovnik` | timeline |
| 17 | 29/09 | 2 | **`dubrovnik`** | `cilipi` | **timeline** (08:00, as muralhas) |
| 18 | 30/09 | 2 | `amsterdam` | `amsterdam` | base |
| 19 | 01/10 | — | `amsterdam` | `amsterdam` | base |
| 20 | 02/10 | — | `amsterdam` | `amsterdam` | base |

*chips* is what the band draws; **—** means no band at all. Eight days have
one city with content and get none.

Four days move: **4, 12, 15 and 17** — the ones that sleep somewhere other
than where they are spent. The other sixteen open exactly where they opened
before, which is what the table is for.

### Proved by failing, three times

**1 · The choice.** `exploreCitiesOf` was written first with today's rule —
the base, and nothing else — so the six cases could fail for the right reason
rather than fail to compile:

```text
FAIL  the timeline decides, and it outranks the base
      expected:<[dubrovnik]> but was:<[cilipi]>
FAIL  the first row that points at a place wins, not the first row
      expected:<[blagaj]> but was:<[mostar]>
FAIL  a walk row counts as a place, the same as an attraction row
      expected:<[butmir]> but was:<[sarajevo]>
FAIL  a base with nothing and no timeline falls to the first city with content
      expected:<[dubrovnik]> but was:<[cilipi]>
FAIL  the six days resolve the way the package says they should
      day 12 opens at expected:<[bastasi]> but was:<[sarajevo]>
FAIL  Explorar leaves the base on four days, and the other screens do not
      expected:<[4, 12, 15, 17]> but was:<[]>
9 of 17 failed
```

With the timeline rule in place: **17 of 17 pass.**

**2 · The cursor does not leak.** The regression this guards is someone
unifying the four resolvers later, so the red was produced by doing exactly
that — pointing `cityOfDay`, `TodayUseCase` and `buildEmergencyState` at
`exploreCitiesOf`:

```text
FAIL  ExploreCityBandTest > moving the cursor moves Explorar and nothing else
      expected:<[mostar]> but was:<[sarajevo]>
FAIL  ExploreCitiesTest > Explorar and the base disagree on purpose, and cityOfDay keeps the base
      expected:<[cilipi]> but was:<[dubrovnik]>
FAIL  ExplorePackagedDaysTest > Explorar leaves the base on four days, and the other screens do not
      expected:<[4, 12, 15, 17]> but was:<[]>
FAIL  MenuStateTest > walking the day cursor does not move screen 02's shortcut
      expected:<Comer em [Mostar]> but was:<Comer em [Sarajevo]>
FAIL  MenuStateTest > a borrowed menu keeps the country and currency of the menu, not of the day
FAIL  MenuStateTest > no shortcut on a day whose city has no menu, even with a fallback
FAIL  MenuStateTest > with no declared fallback the screen says the menu is not written
FAIL  MenuStateTest > a city with its own menu shows no notice
8 of 402 failed
```

Three of those are new and five were already there since D089 — the older
guard and the new one catch the same unification. The three files were
restored to their exact bytes (`git diff` empty on all three), never with
`git checkout --`.

**3 · One city, no band.** Loosening the threshold to `>= 1`, in the state and
in the composition:

```text
FAIL  one city with content draws no band at all           expected:<[]> but was:<[zabljak]>
FAIL  an unknown city id in cityIds is not a chip …        expected:<[]> but was:<[sarajevo]>
FAIL  twelve of the twenty days carry more than one city   expected:<12> but was:<19>
FAIL  the six days resolve the way the package says …      day 9 chips expected:<0> but was:<1>
FAIL  a day with one city draws no band at all             Failed to assert count of nodes.
5 failed
```

Restored to `>= 2`: green. The composition test also asserts that the rest of
screen 04 is untouched on those days — hero, intro, **O QUE VER** and
**HISTÓRIAS CURTAS** all still drawn.

### What the screenshots showed

Release APK on `emulator-5554` (API 37, 1440×3120), clock moved from Settings
→ Date & time. Screenshots are outside the repository, in this session's
scratchpad.

- **29/09, Dia 17.** Screen 02 reads **Čilipi · Croácia** — the base, unmoved.
  Explorar opens on **Dubrovnik**, the band under the hero holding **Dubrovnik**
  in teal and **Čilipi** neutral with its 1dp border, and the thirteen cards
  below. Before this change the same screen was Čilipi and its zero attractions.
- **The chip works, and it works alone.** Tapping **Čilipi** switches screen 04
  to Čilipi; going back to **Hoje** still reads **Čilipi · Croácia, Dia 17 de
  20**; returning to Explorar **restarts on Dubrovnik** — the cursor did not
  survive the trip through another tab, which is the whole point of using
  `remember` rather than `rememberSaveable`.
- **27/09, Dia 15.** Screen 02 reads **Mostar**. Explorar opens on **Blagaj**,
  with four chips: **Mostar · Blagaj · Kravice · Počitelj**.
- **21/09, Dia 9.** Screen 02 and Explorar both read **Žabljak**, and there is
  **no band**: the intro starts at y=1149 where day 17's starts at y=1359,
  which is the band's own height. The eight single-city days are untouched.

### Verified

- 6 validators rc=0, `Audio: 50 guide(s) timed against a packaged file, 0 not
  timed`, unchanged; `check_repo.py` PASS; `content_preflight` PASS 3 / PASS 8;
- `test_validate_trip.py` **51 tests OK**; `git diff --check` clean;
- Kotlin **403 tests, 0 failures** from 48 XML files — 381 before, **22
  new**; `lintDebug` **0 errors, 33 warnings**;
- both APKs **78 entries** under `assets/trip-production/`; `app-release.apk`
  **61.8 MB**, `app-debug.apk` **66.9 MB**; release DEX `"Protótipo"` 0 and
  `"simular chegada"` 0;
- no file under `trip-package/`, `app/src/main/assets/` or `tools/` changed;
- both tracked `trip.json` blobs still
  `97627a8c8cda0c1126eacda352aff0b30e6427ce`.


## The doubled line, and the reference nobody checks (2026-09-08)

Two defects of very different size. One prints the same words twice on the
screen the app is actually used through; the other turned out, when measured,
not to be a defect at all.

### D151 — the lock screen printed one line twice, on forty of forty-seven

`AttractionViewModel` passed `subtitle = state?.name` into `audioGuideRequest`,
which sets `title = guide.title`. Counted over the package that ships: **47**
attractions carry an `audioGuideId`, and in **40** of them `guide.title` **is**
`attraction.name`, word for word. Only seven differed — Muralhas de Kotor,
Casco antigo de Kotor, Lago Negro, Túnel da Guerra, Koski Mehmed Pasha,
Caverna Betina, Stradun. So the lock screen — headphones in, screen dark, the
whole interface by this app's own invariant — read:

```
Muralhas de Kotor
Muralhas de Kotor
```

This is D102's defect through the door D102 never guarded: that one watched
story against guide, this is attraction against guide. D148 found it and named
the slot; it is closed now.

**Two fixes, deliberately separate.**

- **The floor, in `audioGuideRequest`.** A subtitle equal to the title is
  dropped there, once, for all **six** call sites that hand one in (screen 05;
  screen 04's city guide at `AppNavigation.kt:667` and `:674`; the story at
  `:690`; `TogetherRoute.kt:61`; `WalkModeController.kt:231`) and for the city
  guides still to come. Compared **trimmed and case-insensitively**, because
  the package carries story titles ending in a space.
- **The information, in `AttractionViewModel`.** Dropping alone leaves 40
  second lines blank, and blank is only *less wrong* than repeated. The
  subtitle is now `cityLine`, which is what the lock screen was missing:
  `Muralhas de Kotor` over `Kotor · Montenegro`.

**One rule for all forty-seven.** The seven that already differed lose their
old pair and gain the city line like everybody else. The alternative is an `if`
whose branch turns on two strings happening to be equal — the D089 shape.

**No content moved.** Not one guide, attraction, city or story was renamed.
The story↔guide guard in `validate_trip.py` **stays**; the matching
attraction↔guide guard is **deliberately not added**, because after these two
fixes equal titles no longer print twice and a new guard would fail all six
validators on content that is correct.

### D142 — measured, and it was not a runtime defect

D142 stood recorded as "`criticalItemsFor` does not read
`timelineItem.criticalItemIds`", which reads as critical items being lost.
Counted over `assets/trip-production/trip.json` by replaying `criticalItemsFor`
in full:

| | |
|---|---|
| critical items declared | **13** |
| orphans (declared, never reached) | **0** |
| references in `timelineItem.criticalItemIds` | **13** |
| of those, not reaching `criticalItemsFor` | **0** |

And it is structural, not luck: `trip.schema.json` declares `criticalItems` in
exactly three places — `$defs/day`, `$defs/transport`, `$defs/accommodation` —
and there is **no global array**. `criticalItemIds` is a list of ids, not of
objects, so an id none of those three declares has no object behind it and no
change to `criticalItemsFor` could materialise one. **`criticalItemsFor` was
not touched.**

The real risk is a **dangling reference**: a timeline row naming an id its own
day cannot reach. It would vanish silently in two places —
`TodayUseCase.kt:189` would leave the row without its critical emphasis, and
`WalkFinishedState.kt:80` would stop filtering the row it should hide. So the
fix became an **authoring guard**, not a runtime change:
`validate_trip.dangling_critical_reference_problems` requires every id in
`timelineItem.criticalItemIds` to be declared on that **same day**, or on a
`transport` or `accommodation` that day lists. Reported through
`content_checks` — a warning while the package is being authored, a hard error
once `contentStatus` is `production`. The rule of D095, for the reason of D095.
All six packages stay `rc=0`, exactly as the four numbers predicted.

### Proved by failing, three times

```
AttractionLockScreenPackagedTest > no attraction of the real trip prints the same line twice FAILED
AttractionLockScreenPackagedTest > the second line names the city and the country FAILED
AttractionLockScreenTest > a guide named after its attraction still gets two different lines FAILED
AttractionLockScreenTest > a guide named differently gets the same second line FAILED
AudioGuideSubtitleTest > aSubtitleThatDiffersOnlyBySpacingOrCaseIsAlsoDiscarded FAILED
AudioGuideSubtitleTest > aSubtitleEqualToTheTitleIsDiscarded FAILED
9 tests completed, 6 failed
```

The packaged test names every collision it finds, and the red run listed
**exactly 40** — the number counted from the package, counted back out of the
app. After both fixes: **9 tests, 0 failed.**

1. **The 40.** `AttractionLockScreenPackagedTest` walks all 47 attractions
   through the real `AttractionViewModel` into
   `FakeAudioEngine.preparedTitle/preparedSubtitle` — the exact strings the
   media session receives — and asserts each pair differs. `assumeTrue` like
   `ExplorePackagedDaysTest`, since `trip-production/` is not in the
   repository; `AttractionLockScreenTest` builds the same collision by hand and
   runs everywhere.
2. **The subtitle says where.** The packaged test asserts
   `"Muralhas de Kotor"` over `"Kotor · Montenegro"`, and a third case asserts
   no attraction is left with a blank second line — which is what the discard
   alone would have produced 40 times.
3. **The dangling reference.** `tools/test_validate_trip.py` gains a fixture
   naming an id nobody declares: **rc=1** with `contentStatus: "production"`,
   **rc=0 with a WARNING** without it, and a control run proves the extra
   finding is the guard's and not the sample's two unpackaged documents. With
   the guard unwired the same fixture passed **completely silently**, which is
   the point.

### What the screenshots showed

Release APK on `emulator-5554` (API 37), clock moved from Settings → Date &
time, a PIN set so the real lock screen draws. Screenshots are outside the
repository, in this session's scratchpad.

- **28/09, Dia 16, Dubrovnik.** **Muralhas da cidade velha** over **Dubrovnik ·
  Croácia** — one of the 40, which before this printed its own name twice.
- **Palácio do Reitor** over **Dubrovnik · Croácia** — also one of the 40.
- **Caiaque pelas muralhas**, one of the seven, whose guide is titled *Caverna
  Betina*: **Caverna Betina** over **Dubrovnik · Croácia**. Title and subtitle
  still differ, and the second line now names the city.
- **25/09, Dia 13, Sarajevo.** The story keeps the pair it always had:
  **Sebilj, Baščaršija** over **O Sebilj tem 1891; a praça tem 1462**. All
  three packaged story titles differ from their guide's, so the discard fires
  on none of them and nothing on that path moved.

### Verified

- 6 validators rc=0, `Audio: 50 guide(s) timed against a packaged file, 0 not
  timed` on the three production copies, unchanged; `check_repo.py` PASS;
  `content_preflight` PASS 3 / PASS 8;
- `test_validate_trip.py` **60 tests OK** — 51 before, **9 new**;
  `git diff --check` clean;
- Kotlin **412 tests, 0 failures, 0 skipped** from 51 XML files — 403 before,
  **9 new**. Nothing skipped means `assumeTrue` did not fire and the
  47-attraction walk really ran;
- `lintDebug` **0 errors, 33 warnings** — the same 33, with the same breakdown
  (11 GradleDependency, 9 UseTomlInstead, 6 NewerVersionAvailable, 2
  AndroidGradlePluginVersion, 2 UseKtx, 1 InlinedApi, 1 ModifierParameter, 1
  ObsoleteSdkInt). No new warning;
- both APKs **78 entries** under `assets/trip-production/`; `app-release.apk`
  **61.8 MiB**, `app-debug.apk` **66.9 MiB**; release DEX `"Protótipo"` 0 and
  `"simular chegada"` 0;
- no file under `trip-package/` or `app/src/main/assets/` changed; no content
  text of any kind changed;
- both tracked `trip.json` blobs still
  `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## The sentence that did not fit the pill, and the wrong leg of the ticket (2026-09-08)

Three findings from a review. Two were real and are fixed; the third was
measured and did not exist, and the measuring is recorded so nobody repairs it
later. No content text changed — not a price, not an hour, not a package.

### D154 — ten practical strings were being cut, and the cut half was the instruction

`AttractionState` put `price`, `openingHours` and the derived duration into one
chip list, and `TcChip` draws one line with `TextOverflow.Ellipsis`. The clause
is right (D055) and was not touched; what was wrong is what was put inside it.

Counted over `assets/trip-production/trip.json`: **22 practical strings on 47
attractions — 17 `price` and 5 `openingHours` — of which 10 run past 45
characters**, the longest at **141**. In a one-line pill each became forty-odd
letters and an ellipsis, and the half that got cut was always the second, which
is the instruction. Screen 05 drew `practical` in exactly one place, so there
was no full-text strip underneath: the truncated sentence was all a traveller
got at the ticket window.

The rule now: **the pill keeps what the app derives, the source's text gets a
line.** Screen 05's chips are `Audioguia N min`, `Salvo offline`, `Visita ~N
min` — three strings the app composes from a number, none over twenty
characters. `price` and `openingHours` are drawn whole in a `TcCard` in the
operational layer, labelled **ENTRADA** (the plate's own word) and **HORÁRIOS**.
Screen 04's carousel card loses the price and keeps `Audioguia` / `Offline`.
No length threshold decides anything anywhere; the rule turns on which side
wrote the string. Recorded as a deliberate addition to screens 04 and 05, the
way D153 was — the plate is not wrong, it was drawn before the source supplied
a 141-character sentence for a field it had drawn as `Entrada livre`.

### D155 — the ticket home drew the flight out, and the connection drew the leg already flown

`DocumentState.kt:105` took the first transport naming the document, with no
criterion — D097's shape. **Two of the 27 documents are named by two transports
each, and the element it happened to return was the wrong one in both.** The
LATAM ticket holds LA 8078 (13/09 18:00, Guarulhos → Schiphol, index 0) and LA
8079 (02/10 13:10, Schiphol → Guarulhos, index 8), so on **02/10** it drew the
outbound. The Croatia ticket holds OU 661 (30/09 06:15, Dubrovnik → Zagreb,
index 6) and OU 450 (30/09 08:25, Zagreb → Amsterdã, index 7) — **1h15 apart on
one morning**, so at the Zagreb gate it drew the flight already flown.

`journey` is now the list of transports naming the document, sorted by
`origin.dateTime`, and screen 14 draws one block per leg. **The other 25
documents are untouched**: 5 are named by one transport and draw one block, 20
are named by none and draw none. `price` and `locator` were not repaired,
because they were not broken — all four transports declare a null price and each
pair shares one booking reference, which the document's own locator already
carries. Nothing picks a leg by today's date: that needs date plumbing screen 14
does not have, and it gets 30/09 wrong, where both legs are relevant.

### D156 — measured, and it was not there

The review recorded that screen 03 hides day 16's 06:30 bus behind the 13:00
kayak. Four measurements say otherwise, and all four were re-checked here:

1. `TodayUseCase.kt:38` already sorts the timeline by `startTime`, and
   `FullDayUseCase` builds from that state;
2. `sectionsOf` groups by `periodOf(it.item.startTime)` and `filter` preserves
   order inside each period;
3. all **61 timeline rows over the 20 packaged days are already in ascending
   order, and none omits `startTime`**;
4. day 16 is **06:30 · 07:00 · 11:00 · 13:00 · 16:00**, and the kayak is 13:00,
   not 12:45.

Nothing in the sort, `sectionsOf` or `periodOf` was touched. The test written
for this **starts green on purpose** and guards against the repair rather than
the defect: the cheapest way for day 16 to break is for someone to take this
review at its word and re-sort a sorted list.

### Proved by failing, three times

- **the chips.** A test walking all 47 attractions of the real package asserted
  no chip over 45 characters. **Red, naming 10 offenders by id and length**, the
  worst at 141. Green after. A second test asserts all 22 practical strings
  reach the state character for character, and a hand-built companion in
  `AttractionStateTest` carries the real 141-character Dubrovnik Pass string so
  the guarantee holds on machines without the package;
- **the legs.** Against the real package, the Croatia ticket's destination read
  `Zagreb · aeroporto` where `Amsterdã · Schiphol` was expected, and the LATAM
  ticket's departure read `18:00` where `13:10` was expected. **Both red**, both
  green after. A third case asserts the leg-count split across all 27 documents
  as 20 with none, 5 with one and 2 with two, which is the guarantee that the
  other 25 did not move;
- **the order.** `FullDayTimelineOrderPackagedTest` asserts all 61 rows of the
  20 days leave `FullDayUseCase` in clock order, and day 16 by the hour. **Born
  green, and said so in its own comment.**

### What the screenshots showed

Release APK on **`emulator-5554`, an emulator and not a phone** — a Pixel_10 AVD
on a `google_apis_playstore` API 37.1 image, so `adb root` is refused and the
clock was moved through Settings → Date & time, as in the previous pass.
**D110 therefore stays open: the release build has still never run on a real
telephone.** Screenshots are outside the repository, in this session's
scratchpad.

- **28/09, Dia 16, Dubrovnik, screen 04.** Every card in the carousel draws
  `Audioguia` and `Offline` and nothing else — including **Muralhas da cidade
  velha** and **Caiaque pelas muralhas e Lokrum**, whose prices are 141 and 60
  characters;
- **28/09, screen 05, Muralhas da cidade velha.** Chips are `Audioguia 2 min`,
  `Salvo offline`, `Visita ~120 min`. **ENTRADA** carries the whole 141
  characters, ending in *mas não cobre o teleférico*;
- **25/09, Dia 13, Butmir, Túnel da Guerra.** **ENTRADA** *20 marcos por pessoa,
  só em dinheiro: não aceitam cartão nem euro* and **HORÁRIOS** *8h30 às 17h, de
  1º de abril a 31 de outubro; última entrada às 16h30*, both legible to the
  end. *(The review placed this attraction in Kotor on 17/09; it is Butmir's, on
  day 13.)*
- **17/09, Dia 5, Kotor, Muralhas de Kotor e forte de São João.** **ENTRADA**
  the 122-character *As fontes divergem entre €8 e €15…* and **HORÁRIOS** the
  89-character *As guaritas abrem às 7h…*, both whole;
- **30/09, Carteira, Voos Dubrovnik → Zagreb → Amsterdã.** Two blocks, one over
  the other: **06:15 Dubrovnik · aeroporto → 07:10 Zagreb · aeroporto** and
  **08:25 Zagreb · aeroporto → 10:30 Amsterdã · Schiphol**, Croatia Airlines on
  each, one **LOCALIZADOR 96LE2K**, and no **VALOR** field;
- **02/10, Carteira, Voos São Paulo ⇄ Amsterdã.** **18:00 Guarulhos → 11:00
  Schiphol** and **13:10 Schiphol → 20:15 Guarulhos**. On the day of the flight
  home the leg home is on screen, which is the whole of the defect;
- **28/09, screen 03, Dia completo.** **MANHÃ 06:30 · 07:00 · 11:00**, **TARDE
  13:00 · 16:00**, in order — exactly as it already was.

### Verified

- 6 validators rc=0, `Audio: 50 guide(s) timed against a packaged file, 0 not
  timed` on the three production copies, unchanged; `check_repo.py` PASS;
  `content_preflight` PASS 3 / PASS 8;
- `test_validate_trip.py` **60 tests OK** — unchanged, no validator was touched;
  `git diff --check` clean;
- Kotlin **427 tests, 0 failures, 0 skipped** from 54 XML files — 412 before,
  **15 new**. Nothing skipped means `assumeTrue` did not fire and the three
  package-reading tests really ran;
- `lintDebug` **0 errors, 33 warnings** — the same 33 with the same breakdown
  (11 GradleDependency, 9 UseTomlInstead, 6 NewerVersionAvailable, 2
  AndroidGradlePluginVersion, 2 UseKtx, 1 InlinedApi, 1 ModifierParameter, 1
  ObsoleteSdkInt). No new warning; `ModifierParameter` is still the one it was;
- both APKs **78 entries** under `assets/trip-production/`; `app-release.apk`
  **61.8 MiB**, `app-debug.apk` **66.9 MiB**; release DEX `Protótipo` 0 and
  `simular chegada` 0;
- no file under `trip-package/`, `app/src/main/assets/` or `tools/` changed; no
  content text of any kind changed;
- both tracked `trip.json` blobs still
  `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## Six declared fields nothing reads, decided one at a time (2026-09-08)

Six fields the package declares and no screen reads. The work was not to wire
six things: it was to decide each one and write the decision down, because an
undecided dead field gets rediscovered every six weeks. **Two wired, four
closed with a reason.** All six counts were re-measured against
`assets/trip-production/trip.json` before anything was touched, and all six
matched. No content text changed; nothing under `trip-package/`,
`app/src/main/assets/` or `tools/` was touched.

| field | filled | read | decision |
|---|---|---|---|
| `city.historySections` | 0 / 19 cities | nowhere | **wired** (D157) |
| `generalEmergency.note` | 3 / 7 profiles | nowhere | **wired** (D160) |
| `attraction.interestingFacts` | 0 / 47 | nowhere | closed (D158) |
| `showToSomeone.audioAssetId` | 0 / 7 profiles | nowhere | closed (D159) |
| `audioGuide.transcriptAssetId` | 0 / 50 guides | nowhere | closed (D161) |
| `document.sensitive` / `asset.sensitive` | 27 / 27 and 27 / 77 | nowhere | **reserved** (D162) |

The 27 sensitive assets are exactly the 27 of type `document`. "Read: nowhere"
was checked rather than assumed: each of the six appears only in
`TripModels.kt` and in no state builder or composable.

### D157 — the city's editorial sections, on the surface that already existed

The twin of this field has been drawn since D133. `paragraphsOf` and
`EditorialSectionUi` moved to `domain/editorial/EditorialSections.kt` so both
screens call one copy — the blank-line split is the fix from `8e66bc3`, and a
second copy would drift the day either is corrected. The precedent is
`domain/operations/Contacts.kt`, shared by screens 17 and 19.

It draws zero rows in this build, which is acceptable **only** because the cost
is five lines onto a surface that exists. The same argument does not carry to
D159, and that difference is the point of the pair.

**Wiring it found a collision in the sample package.** `assets/trip/` gives
Sarajevo two `historySections` titled **"Período otomano"** and **"Século
XX"** — word for word two of the three chapter titles of `ag.sarajevo.city`.
Drawn, they put the same strings on screen 04 twice, and `CityChapterTest`
went red because "Século XX" stopped naming one node. That test now selects
the chapter by its click action, since a chapter row is a control and an
editorial heading is not. **Nothing ships with the collision** — no production
city fills the field — but it is the obvious shape for a real package to take,
and it is named in D157 for whoever fills these 19.

### D160 — the emergency note, and why `note` is wired one field at a time

Three profiles carry `generalEmergency.note` and it is operational, not
commentary: in Bosnia 112 may not answer, and the note is what sends the
traveller to the 122 and 124 drawn below it. It now sits inside the general
emergency block, under the number, at secondary weight, no new component and
no colour outside the tokens. It is kept out of `PhoneUi.note`, which already
carries the sentence shown *in place of* a number withheld as mock content —
the two would collide exactly where both apply — so `EmergencyUiState` gained
its own `generalNote`.

**`note` does not mean the same thing on every contact**, which is why only
this one field was wired. In `trip-production` the notes are traveller-facing;
in the sample package at `assets/trip/` the insurer's reads **"Substituir pelo
contato real"** and the consulate's **"Substituir por informação verificada"**
— instructions to an author. Wiring `note` generically would have printed one
of those on screen 17. Montenegro's consular note is traveller-facing and
still unread; named in D160 rather than left to be rediscovered.

🚨 **Open content dependency, before the release goes on a telephone.** See
"What the screenshots showed" below: Bosnia's note asserts the screen shows
"122, 123 e 124" and the screen shows 122 and 124. `$defs/emergencyProfile`
declares no fire-brigade contact and `EmergencyProfile` matches it, so 123 has
nowhere to come from. This session did not touch the text — it is content,
under `trip-package/`, and belongs to a content session.

### D162 — `sensitive` is reserved, not dead

The only one of the six that is full, at both levels, coherently. Before
deciding, the question was whether anything leaks today, because a leak would
make it a defect. Four things were searched and the search is recorded in
D162 so it is not repeated: **outbound paths** (only `FirebaseGroupSyncRepository`,
which writes `seenAt` and a playback node of `mediaId`, `positionMs`,
`isPlaying`, `anchorServerMs`, `updatedBy` — no document data); **the
FileProvider** (`exported="false"`, exposing only `files/memories/` and
`cache/documents/`); **how a document reaches a viewer**
(`PackagedDocumentFile.materialise` copies one file into `cacheDir/documents/`
after wiping the directory, and `openDocument` hands it over with `ACTION_VIEW`
and an expiring read grant — no chooser, no `ACTION_SEND`); and **logging**
(five `Log.i` calls in all of `main`, all geofence and alarm, none touching a
document, locator or QR). **Nothing leaks.** The field has two named future
consumers — the `dataExtractionRules` that `allowBackup="true"` still lacks,
and any approved "ocultar documentos pessoais" — and a field with a named
consumer waiting is reserved rather than dead.

### Proved by failing, twice

- **the city sections.** A test composing screen 04 with two sections of two
  paragraphs each: **red** — "could not find any node that satisfies … 'A
  cidade sob o império'" — because the state did not carry the field. Green
  after. Its companion asserts a city with no sections draws no heading and no
  reserved space, which is all 19 packaged cities and therefore the guarantee
  that nothing moved for anybody;
- **the emergency note.** A test composing screen 17 with Bosnia's real note:
  **red** — "could not find any node that satisfies … 'O 112 ainda está em
  implantação na Bósnia…'". Green after. Its companion asserts that a profile
  with no note draws nothing in its place while the button and its own
  sentence stay.

The four closed fields have **no tests**, deliberately: they change no
behaviour. Their product is the four entries in `docs/DECISIONS.md`.

### What the screenshots showed

Release APK on **`emulator-5554`, an emulator and not a phone** — the Pixel_10
AVD on a `google_apis_playstore` API 37.1 image, so `adb root` is refused and
the clock moved through Settings → Date & time. **D110 stays open: the release
build has still never run on a real telephone.** Screenshots are outside the
repository, in this session's scratchpad.

- **25/09, Sarajevo, screen 17.** The note is drawn under the 112 button and
  under "Funciona sem crédito e sem chip local.", transcribed from the screen
  word for word:

  > O 112 ainda está em implantação na Bósnia; as páginas oficiais do país
  > publicam 122, 123 e 124, e são esses que a tela mostra ao lado.

  **The sentence is wrong, and drawing it is what makes that visible.** The
  only dialable three-digit numbers on the screen are **122** (Polícia) and
  **124** (Ambulância); the sole occurrence of "123" anywhere on screen 17 is
  inside the note's own text. There is no fire-brigade field in the schema, so
  123 cannot be drawn. A traveller reading this in Sarajevo would look for a
  third number that is not there. Needs a content session — either the
  sentence names only the two numbers, or the schema gains a contact, and the
  second is not a seven-days-out change;
- **13/09, São Paulo, screen 17.** Brazil's note under **Ligar 190**, and
  correctly no "Funciona sem crédito" line, which is a property of 112 (D088);
- **19/09, Kotor, screen 17.** Montenegro carries no note and nothing is drawn
  in its place: the block is the one it always was, and Polícia/Ambulância sit
  at y=1113 against Bosnia's y=1300 — the note's own height, and nothing else;
- **28/09, Dubrovnik, screen 04.** No city editorial block, because no
  packaged city has one. The intro sits at y=1471 and "O QUE VER" at y=1671 —
  the same coordinates as before the change, so there is no empty heading and
  no reserved gap.

### Verified

- 6 validators rc=0, `Audio: 50 guide(s) timed against a packaged file, 0 not
  timed` on the three production copies, unchanged; `check_repo.py` PASS;
  `content_preflight` PASS 3 / PASS 8;
- `test_validate_trip.py` **60 tests OK** — unchanged, no validator was
  touched; `git diff --check` clean;
- Kotlin **431 tests, 0 failures, 0 skipped** from 56 XML files — 427 before,
  **4 new**;
- `lintDebug` **0 errors, 33 warnings** — the same 33 with the same breakdown
  (11 GradleDependency, 9 UseTomlInstead, 6 NewerVersionAvailable, 2
  AndroidGradlePluginVersion, 2 UseKtx, 1 InlinedApi, 1 ModifierParameter, 1
  ObsoleteSdkInt). No new warning;
- both APKs **78 entries** under `assets/trip-production/`; `app-release.apk`
  **61.8 MiB**, `app-debug.apk` **66.9 MiB**; release DEX `Protótipo` 0 and
  `simular chegada` 0;
- no file under `trip-package/`, `app/src/main/assets/` or `tools/` changed; no
  content text of any kind changed; no field removed from `TripModels.kt`;
- both tracked `trip.json` blobs still
  `97627a8c8cda0c1126eacda352aff0b30e6427ce`.

## The Bosnian note stops promising a number the screen has not got (2026-09-08)

A content session. One field, three packages, no code. `generalEmergency.note`
became visible in D160 six days before departure, and the Bosnian sentence it
put on screen 17 ended by claiming the screen shows "122, 123 e 124". It shows
122 and 124. D163 records the change.

### What changed

`emergencyProfiles[BA].generalEmergency.note`, from:

> O 112 ainda está em implantação na Bósnia; as páginas oficiais do país
> publicam 122, 123 e 124, e são esses que a tela mostra ao lado.

to:

> O 112 ainda está em implantação na Bósnia. A polícia é 122 e a ambulância
> 124, logo abaixo; os bombeiros são 123, que esta tela não disca.

134 characters to 138. Nothing else in any package moved.

`$defs/emergencyProfile` has **no fire-brigade contact** — `fire`, `bombeiro`
and `fireBrigade` occur nowhere in `trip.schema.json` — so 123 has nowhere to
be drawn from in any package. The schema was **not** changed: adding a contact
across three packages six days out is not the cheap fix; the sentence not
lying is.

**123 is kept as an unverified assertion of the package, and is not claimed to
be verified.** There is a versioned source for these numbers —
`trip-package/source/private/itinerario-detalhado.md:1511-1519`, whose columns
are *País · Emergência geral · Ambulância · Polícia* — and for Bósnia it reads
**112 / 124 / 122**, matching the package and the screen exactly. It has no
fire column, and no versioned source in the repository mentions a fire brigade
in any language. So the source confirms what the app dials and is silent on
123.

### The other two notes: checked, not changed

- **Albania** — "A Polícia do Estado integrou polícia, bombeiros e polícia
  rodoviária no 112; 129, 128 e 126 saíram de uso." The screen draws 112 and
  the ambulance 127 and has no police row, which the note explains rather than
  contradicts; the numbers it names are named as withdrawn. No claim about the
  screen. **Unchanged.**
- **Brazil** — "No Brasil não há número único: 190 é a Polícia Militar, 192 o
  SAMU e 193 o Corpo de Bombeiros. De celular, 112 e 911 caem no 190." The
  screen draws 190 and the SAMU 192. 193 is not on it and the sentence never
  says it is. **Unchanged**, and the precedent the new Bosnian sentence
  follows.
- **Left alone, and not about the screen:** the source table gives Albania's
  police as 129 while the note says 129 is withdrawn and the package declares
  no Albanian police contact. The package agrees with the note. A content
  question about Albania, named for a future session.

### What the screenshots showed

Release APK on **`emulator-5554`, an emulator and not a phone** — the Pixel_10
AVD on a `google_apis_playstore` API 37.1 image, so `adb root` is refused and
the clock moved through Settings → Date & time. **D110 stays open: the release
build has still never run on a real telephone.** Screenshots are outside the
repository, in this session's scratchpad.

- **25/09, Sarajevo, screen 17.** Transcribed from the screen, not from the
  file:

  > O 112 ainda está em implantação na Bósnia. A polícia é 122 e a ambulância
  > 124, logo abaixo; os bombeiros são 123, que esta tela não disca.

  The dialable numbers on the screen are **112** (the 88dp button, "Ligar
  112"), **122** (Polícia) and **124** (Ambulância) — three, counted off the
  view hierarchy. **"123" occurs exactly once on the whole screen**, inside the
  note's own text, in the clause that says the screen does not dial it. The
  note sits at the same offset the old one did and the police and ambulance
  buttons are in the same place as before, so the four extra characters cost no
  line and the block is not dominated;
- **13/09, São Paulo.** Brazil's note unchanged under **Ligar 190**, still with
  no "funciona sem crédito" line, which is a property of 112 (D088);
- **15/09, Ksamil.** Albania's note unchanged under **Ligar 112**, with the
  ambulance 127 and no police row;
- **19/09, Kotor.** Montenegro still carries no note: the block runs straight
  from "Funciona sem crédito e sem chip local." to Polícia/Ambulância, exactly
  as before.

### ⚠️ Open, and deliberately not fixed here *(closed 2026-09-08 — see D165)*

`app/src/test/java/com/travelcompanion/app/feature/emergency/EmergencyNoteTest.kt`
holds the **old** sentence as a literal — `private val bosniaNote`, **lines
36-38** — under a KDoc at **line 35** calling it "the real note on its 112".
The class KDoc at **lines 20-22** is stale for the same reason: it paraphrases
the removed clause. **The test does not fail** — it injects its own string and
never reads the real note, and all 431 tests stayed green — but the constant is
now a copy of a sentence that exists in no package. This is Kotlin, so a
content session does not touch it; it needs an implementation session.

### Verified

- 6 validators rc=0, `Audio: 50 guide(s) timed against a packaged file, 0 not
  timed` on the three production copies, unchanged; `check_repo.py` PASS;
  `content_preflight` PASS 3 / PASS 8;
- `test_validate_trip.py` **60 tests OK**, unchanged; `git diff --check` clean;
- Kotlin **431 tests, 0 failures, 0 skipped** from 56 XML files — the same 431
  as `b267eaa`. No test guarded the old sentence;
- `lintDebug` **0 errors, 33 warnings** — the same 33 with the same breakdown
  (11 GradleDependency, 9 UseTomlInstead, 6 NewerVersionAvailable, 2
  AndroidGradlePluginVersion, 2 UseKtx, 1 InlinedApi, 1 ModifierParameter, 1
  ObsoleteSdkInt);
- both APKs **78 entries** under `assets/trip-production/`; `app-release.apk`
  **61.8 MiB**, `app-debug.apk` **66.9 MiB**; release DEX `Protótipo` 0 and
  `simular chegada` 0; the new sentence read back out of
  `assets/trip-production/trip.json` **inside the release APK**;
- the three copies are byte-identical to one another apart from
  `metadata.contentStatus`, proved by command: **IDENTICAS**;
- no Kotlin, no test and nothing under `tools/` was touched; no other package
  text changed;
- both tracked `trip.json` blobs still
  `97627a8c8cda0c1126eacda352aff0b30e6427ce` — neither file was opened.

## Live weather, and two sentences that had fallen behind — 2026-09-08

Five days before departure. Three things, one commit: the last unbuilt item of
§32, and two pieces of prose that stopped being true in the two commits before
this one. **No content text changed and no package was opened.**

### Live weather (D164)

`Live · Cached · FallbackFromTrip · Unavailable`, degrading in that order.
Before this the app had **two** of the four and no network call in it at all.

- **Open-Meteo, no key, no registration.** That property decided the provider:
  a key belongs either in a versioned file, which this repository will not
  carry, or in a `local.properties` outside git, which is one more thing to
  keep working five days out. The request is
  `…/v1/forecast?latitude=&longitude=&daily=temperature_2m_max,temperature_2m_min,precipitation_sum&timezone=<the day's zone>&forecast_days=1`,
  and `OpenMeteoRequestTest` asserts that string **without sending it**;
- **no dependency was added**, and this was checked rather than assumed:
  `app/build.gradle.kts` and `gradle/libs.versions.toml` are byte-identical to
  `74d8cf8`. `HttpURLConnection`, `kotlinx.serialization`,
  `kotlinx-coroutines-android` and `datastore-preferences` were already there,
  and `INTERNET` was already declared at `AndroidManifest.xml:4`;
- **the coordinate is the centroid of the attractions of the city the day
  happens in** — `exploreCitiesOf(...).openAtCityId`, the same answer D152
  already built and tested, and not the base: day 17 sleeps in Čilipi and is
  spent inside Dubrovnik's walls. It is the mean and not the first element,
  because weather is regional and `attractionIds.first()` is the unearned
  choice D097 names;
- **nothing is logged.** The app still has exactly five `Log.i` calls, none of
  them in the new code and none touching traveller data.

#### The twenty days, and what each one asks about

Computed from `assets/trip-production/trip.json`; rounded to four decimals,
about eleven metres.

| Dia | Cidade | Atrações no centroide | Coordenada |
|----:|--------|----------------------:|------------|
| 1  | sao-paulo | 0  | — sem atração, sem chamada |
| 2  | amsterdam | 6  | 52.3669, 4.8925 |
| 3  | ksamil    | 1  | 39.7717, 20.0122 |
| 4  | butrinto  | 1  | 39.7444, 20.0241 |
| 5  | kotor     | 5  | 42.4236, 18.7714 |
| 6  | kotor     | 5  | 42.4236, 18.7714 |
| 7  | kotor     | 5  | 42.4236, 18.7714 |
| 8  | zabljak   | 4  | 43.1620, 19.0506 |
| 9  | zabljak   | 4  | 43.1620, 19.0506 |
| 10 | zabljak   | 4  | 43.1620, 19.0506 |
| 11 | bastasi   | 2  | 43.2580, 18.9640 |
| 12 | bastasi   | 2  | 43.2580, 18.9640 |
| 13 | sarajevo  | 7  | 43.8595, 18.4285 |
| 14 | mostar    | 3  | 43.3378, 17.8151 |
| 15 | blagaj    | 1  | 43.2531, 17.8908 |
| 16 | dubrovnik | 13 | 42.6400, 18.1109 |
| 17 | dubrovnik | 13 | 42.6400, 18.1109 |
| 18 | amsterdam | 6  | 52.3669, 4.8925 |
| 19 | amsterdam | 6  | 52.3669, 4.8925 |
| 20 | amsterdam | 6  | 52.3669, 4.8925 |

**Nineteen of twenty.** All forty-seven packaged attractions carry
`location.geo`; cities and accommodations carry none. Day 1's city packages no
attraction, so there is no coordinate, so there is no call — the right answer
for a day that starts at home and ends on a night flight.

#### The trap, and the guard

A reading taken in Kotor and drawn in Dubrovnik is a wrong forecast wearing a
right one's face, and it would look right nearly always, because consecutive
days are usually spent in one city. So the cache stores the coordinate and the
instant beside the reading and refuses itself when the day's coordinate is not
the same. Run without that guard, the test accepts Kotor's 27° for a Dubrovnik
day — which is what was watched happening before the guard was written.

#### Proved failing first, three times

1. **The four states.** With `Live` and `Cached` drawing the packaged badge
   instead of their own hour: `a live reading says it is live and says when`
   and `a cached reading says when it was taken and is never called live` both
   FAILED — *There are no existing nodes for that selector*;
2. **The cache refuses another place.** With the coordinate half of `isAbout`
   removed: `aCachedReadingTakenSomewhereElseIsRefused` FAILED —
   *expected same:<FallbackFromTrip(…)> was not:<Cached(minC=19.0, maxC=27.0,
   …)>*, which is Kotor's forecast being drawn in Dubrovnik;
3. **Offline disturbs nothing.** With the orchestrator's failure guard removed:
   `neitherAnExceptionNorATimeoutDisturbsThePackagedForecast` and `with the
   provider failing, the whole of screen 02 still draws` both FAILED. This one
   is nearly green the day it is written and says so in its own comment — it is
   there to fail out loud the day somebody makes the card block on an answer.

### The test that guarded a sentence that no longer exists (D165)

`EmergencyNoteTest` still carried the pre-`74d8cf8` Bosnian note as a literal.
It never failed, because it injects the string it asserts. The constant now
carries the packaged sentence **word for word** — compared against
`emergencyProfiles[BA].generalEmergency.note` in the operational package,
character for character identical — and both KDocs describe what the sentence
says now.

### D163's "versioned source" (corrected in place)

D163 called `trip-package/source/private/itinerario-detalhado.md` *a versioned
source*. It is not: `git ls-files trip-package/source/private/` returns exactly
one path, its own `.gitignore`. The file is on the packaging machine and in no
commit, deliberately — `source/private/` holds personal documents. The
correction is written in place with the `*(Corrected: …)*` marker D037 and
D142 use, with the original text kept: the *Saúde e emergência* table is a
**local, untracked** source, nobody who clones this repository has it, and so
**123 still has no versioned verification of any kind** — which is exactly why
the new note does not claim it was verified.

### Verified

- 6 validators rc=0, `Audio: 50 guide(s) timed against a packaged file, 0 not
  timed` on the three production copies, unchanged; `check_repo.py` PASS;
  `content_preflight` PASS 3 / PASS 8, unchanged;
- `test_validate_trip.py` **60 tests OK**, unchanged; `git diff --check` clean;
- Kotlin **451 tests, 0 failures, 0 skipped** from **60 XML files** — 431 from
  56 before, so **+20 tests in 4 new classes**, and no existing test changed
  its result;
- `lintDebug` **0 errors, 33 warnings** — the same 33 with the same breakdown
  (11 GradleDependency, 9 UseTomlInstead, 6 NewerVersionAvailable, 2
  AndroidGradlePluginVersion, 2 UseKtx, 1 InlinedApi, 1 ModifierParameter, 1
  ObsoleteSdkInt). **New networking woke nothing**;
- both APKs **78 entries** under `assets/trip-production/`; `app-release.apk`
  **61.8 MiB**, `app-debug.apk` **66.9 MiB**; release DEX `Protótipo` 0 and
  `simular chegada` 0, with `api.open-meteo.com` present exactly once;
- `app/build.gradle.kts` and `gradle/libs.versions.toml` **unchanged**;
- both tracked `trip.json` blobs still
  `97627a8c8cda0c1126eacda352aff0b30e6427ce`; no package and nothing under
  `tools/` was touched.

### ⚠️ Not verified: this has never been on a screen

**No device and no emulator was attached to this machine** — `adb devices`
listed none — so the release APK was built and never installed, and **no
screenshot was taken**. What *was* proved is narrower and is stated as such:
the endpoint was called once from the packaging machine with Dubrovnik's
centroid and answered **HTTP 200, 29.8° / 24.6°, 0.0 mm for 2026-09-08**, in
the exact shape the parser expects. That proves the URL and the parse. It does
not prove the card.

The five checks that still want an eye, in order:

1. **wi-fi, clock at 28/09** — the card shows a live reading for Dubrovnik;
   note the maximum and the minimum;
2. **aeroplane mode, back to screen 02** — the card becomes `Cached` with the
   hour of that earlier reading on it, and the rest of screen 02 is intact;
3. **clear the app's data, still in aeroplane mode** — the card shows the day's
   `weatherFallback` with the packaged text;
4. **clock at 13/09 (day 1), wi-fi on** — São Paulo has no attraction, so no
   call is made; the card shows the fallback and nothing hangs;
5. **clock at 25/09** — the whole Sarajevo walk, confirming audio and the
   geofence are unchanged.

**D110 remains open**, and this is now the item that most wants a real
telephone.

### Also open, and outside this session's files

`CLAUDE.md` still says "§32's Definition of Done stands at 12 of 13". With
D164 it is 13 of 13. This session's scope was `app/src/main/java/`,
`app/src/test/` and `docs/`, so the root file was not touched; named here so
it is corrected deliberately rather than found as a contradiction.
