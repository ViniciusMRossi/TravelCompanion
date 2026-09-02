# Travel Companion — Technical Implementation Brief v1

Status: **Ready for implementation**  
Date: 2026-09-01  
Platform: **Android**  
Design status: **v1.0 locked**

This document is intentionally lightweight.

It is not an SDD.

Its purpose is to give an implementation agent enough technical direction to build the approved product without reopening product or design decisions.

---

# 1. Goal

Build an Android application that loads one packaged travel dataset and provides:

- day-by-day itinerary;
- contextual Today screen;
- city and attraction guides;
- local audioguides;
- location-aware Walk Mode;
- synchronized group playback;
- offline tickets/documents;
- accommodation and transport details;
- Plan B;
- emergency information;
- weather and outfit guidance;
- voice memories;
- contextual deep links into external apps.

The app is built for a specific trip from:

```text
trip.json
+
local images
+
local audio
+
local documents
```

A future trip should require replacing the travel package and building again, not changing app code.

---

# 2. Non-goals

Do not build:

- CMS;
- itinerary editor;
- web admin;
- traditional authentication;
- invite/join flows;
- chat;
- social features;
- public accounts;
- remote content authoring;
- audio streaming;
- custom turn-by-turn navigation;
- generic travel marketplace features.

---

# 3. Technical principles

## 3.1 Local content is authoritative

All essential travel content is packaged inside the app.

The app must open and remain useful with no network.

Online services may enrich the experience but must not become dependencies for:

- itinerary;
- attraction information;
- documents;
- audioguides;
- emergency information;
- Plan B.

---

## 3.2 Runtime state is separate from trip content

`trip.json` is immutable packaged content.

Runtime/device state is persisted separately.

Examples:

```text
selected participant
completed items
visited attractions
current walk
audio position
sync state
weather cache
voice memories
permissions
notification preferences
```

Never write runtime state back into the packaged `trip.json`.

---

## 3.3 Sync must never block local behavior

If synchronization fails:

- playback continues;
- the walk continues;
- the itinerary continues;
- only group state is temporarily unavailable.

This is a core invariant.

---

## 3.4 Design is already decided

The approved Claude prototype is the visual source of truth.

Engineering may not reinterpret:

- screen structure;
- navigation;
- visual hierarchy;
- button hierarchy;
- semantic colors;
- Walk Mode behavior;
- voice-memory flow;
- participant UX.

---

# 4. Recommended stack

## Language

**Kotlin**

No Java-first implementation.

---

## UI

**Jetpack Compose**

Use Compose for all new UI.

Recommended structure:

```text
Material primitives
+
Travel Companion design tokens
+
Travel Companion components
+
screens
```

Do not rely on default Material styling as the final visual language.

Material components may be used as implementation primitives only.

---

## Navigation

**Navigation Compose**

Use route-based navigation with typed route arguments where practical.

Bottom navigation roots:

```text
Today
Trip
Explore
Wallet
More
```

Immersive/detail screens may sit above these roots.

---

## Serialization

**kotlinx.serialization**

Use it for:

- `trip.json`;
- schema-compatible data models;
- persisted lightweight app state if appropriate.

---

## Local persistence

Use:

**Room** for structured runtime state.

Use:

**DataStore** for small preferences/settings.

### Room

Recommended for:

- participant selection;
- completed timeline state;
- visited attractions;
- memory metadata;
- current/resumable walk metadata;
- notification history;
- locally cached live-data snapshots.

### DataStore

Recommended for:

- permission explanation acknowledged;
- notification category settings;
- autoplay preference;
- audio behavior preference;
- last selected tab;
- simple feature flags.

Do not store large audio/document files in Room.

---

## Audio playback

Use:

**AndroidX Media3 / ExoPlayer**

Required behaviors:

- background playback;
- lock-screen controls;
- notification media controls;
- headset controls;
- seek;
- chapters;
- resume position;
- AudioFocus handling.

Recommended architecture:

```text
MediaSessionService
    ↓
PlaybackController
    ↓
ExoPlayer
```

Screens observe player state rather than owning playback.

---

## Voice recording

Use Android platform audio recording APIs.

Recommended output:

**M4A / AAC**

Goals:

- good speech quality;
- small file size;
- broad playback compatibility.

Each memory should have:

```text
memory id
participant id
recorded timestamp
day id
city id?
attraction id?
walk id?
approximate location?
duration
local file path
```

Do not require a title.

---

## Location

Use Android fused location APIs.

Two different modes:

### Passive story discovery

Use geofence/background-capable location mechanisms.

Purpose:

- nearby story notification;
- low power;
- approximate trigger.

### Active Walk Mode

Use a foreground location strategy while a walk is active.

Purpose:

- more responsive progress;
- next-story awareness;
- route context.

Walk Mode must have a persistent notification when required by Android background execution rules.

Do not continuously request high-accuracy GPS outside Walk Mode.

---

## Notifications

Use Android local notifications.

Categories/channels:

```text
Operational
Stories
Walk
Memory
```

Operational notifications must remain independent from network access.

Examples:

- leave now;
- check-in deadline;
- transport deadline;
- evening memory prompt.

---

## Group synchronization

### Recommended V1

**Firebase Realtime Database**

Use only for lightweight live state.

Do not upload trip content.

Do not upload audio.

Do not build chat.

Typical shared state:

```text
trip/group
├── participants
│   ├── vinicius
│   └── erika
└── playback
    ├── walkId
    ├── trackId
    ├── state
    ├── positionMs
    ├── startedAt
    └── updatedBy
```

Authentication can be invisible to the user.

The app UI still asks only:

> Quem é você?

No account/login UI should be introduced.

---

# 5. Group playback model

Each phone already contains the same audio file.

Synchronization sends playback state, not audio bytes.

## Start flow

Controller/device requests:

```text
track = X
startAt = server time + short delay
position = 0
state = playing
```

Both devices preload the local audio.

They start at the agreed timestamp.

A short UI countdown:

```text
3
2
1
```

is acceptable and matches the approved design.

---

## Drift correction

Each device periodically computes expected playback position.

Conceptually:

```text
expected =
    initialPosition
    + elapsedSinceStart
```

Small drift may be ignored.

Larger drift may trigger:

```text
seekTo(expected)
```

Do not expose drift milliseconds to users.

---

## Reconnect

When reconnecting:

1. retrieve latest playback state;
2. calculate expected position;
3. seek local player;
4. transition UI from:
   - reconnecting
   - synchronized

Local playback must continue throughout.

---

# 6. Time synchronization

Do not trust device wall clocks blindly for synchronized start.

Use a server-derived timestamp reference.

Firebase server timestamps are sufficient for V1.

Implementation should maintain a rough server/device clock offset.

No sub-millisecond precision is required.

This is synchronized narration, not multi-speaker music reproduction.

---

# 7. Content package

Expected build content:

```text
app/src/main/assets/trip/
├── trip.json
├── images/
├── audio/
└── documents/
```

Alternative Android resource placement is acceptable if it preserves:

- stable IDs;
- offline access;
- simple replacement for a future trip.

Use:

`trip.schema.json`

as the structural contract.

---

# 8. Content loading

Recommended startup flow:

```text
App start
   ↓
Load packaged trip.json
   ↓
Deserialize
   ↓
Validate required runtime assumptions
   ↓
Expose TripRepository
```

Do not parse `trip.json` independently in each screen.

Use one source of truth:

```text
TripRepository
```

Recommended responsibilities:

- entity lookup by ID;
- day lookup;
- current trip day;
- city/attraction relations;
- asset resolution;
- document resolution;
- walk/story relations.

---

# 9. Asset resolution

JSON references stable IDs.

Implement:

```kotlin
AssetResolver
```

Conceptually:

```text
asset id
   ↓
asset registry
   ↓
packaged file URI / stream
```

Screens should never manually construct asset filesystem paths.

---

# 10. Suggested project structure

Keep it simple.

```text
app/
  src/main/java/.../
    app/
      TravelCompanionApp.kt
      AppNavigation.kt

    design/
      ColorTokens.kt
      TypeTokens.kt
      Spacing.kt
      Shapes.kt
      Components.kt

    data/
      trip/
        TripModels.kt
        TripRepository.kt
        TripLoader.kt
        AssetResolver.kt

      local/
        AppDatabase.kt
        entities/
        dao/

      sync/
        GroupSyncRepository.kt
        FirebaseGroupSyncRepository.kt

      weather/
        WeatherRepository.kt

    domain/
      today/
      walk/
      audio/
      memory/

    feature/
      whoareyou/
      today/
      trip/
      explore/
      city/
      attraction/
      walk/
      audio/
      wallet/
      transport/
      accommodation/
      emergency/
      planb/
      memory/
      more/

    service/
      playback/
      location/
      notification/
      recording/
```

Avoid premature multi-module Gradle architecture.

A single app module is acceptable for V1.

Split into modules only if complexity later justifies it.

---

# 11. State management

Use screen-level ViewModels.

Recommended pattern:

```text
Repository / service
        ↓
ViewModel
        ↓
StateFlow<UiState>
        ↓
Compose
```

Avoid putting business/runtime logic directly into Composables.

Each feature should expose:

```text
UiState
UiEvent
```

only where useful.

Do not introduce a heavyweight Redux/MVI framework unless implementation complexity actually demands it.

---

# 12. Current day logic

Today screen depends on:

```text
current local date
trip date range
timeline
completion state
current time
weather cache
critical items
```

Compute:

```text
Now item
Next item
Remaining timeline
Critical items
Relevant documents
Relevant Plan B
```

inside a Today domain/use-case layer, not inside UI composables.

---

# 13. Critical items

Criticality must remain independent from booking status.

Represent runtime UI as:

```text
bookingStatus = reserved
critical = true
```

not:

```text
bookingStatus = critical
```

Where available, preserve both:

```text
nominalTime
actionByTime
```

Example:

```text
Bus departure: 19:30
Be there by: 19:00
```

---

# 14. Weather

Use a small repository interface:

```kotlin
interface WeatherRepository
```

Implementation may use a public weather API chosen later.

UI states must support:

```text
Live
Cached
FallbackFromTrip
Unavailable
```

The weather provider is not architecturally important enough to couple to the UI.

Store last successful result locally.

If live weather fails:

```text
Cached → fallback
```

Today must remain functional.

---

# 15. Outfit recommendations

V1 should be deterministic, not AI-dependent.

Inputs:

```text
temperature
rain
wind
activity
altitude
trip fallback
```

Output:

```text
wear[]
carry[]
special[]
```

Rules can live locally.

Do not introduce an LLM dependency for outfit suggestions.

---

# 16. Deep links

Implement an external-action abstraction:

```text
ExternalActionLauncher
```

Support:

- Maps;
- browser;
- phone;
- WhatsApp;
- external app URI;
- email;
- local document.

Always provide fallback behavior where reasonable.

Example:

```text
try app URI
↓
fallback web URI
```

---

# 17. Documents

Documents marked offline must resolve locally.

Recommended support:

```text
PDF
image
QR
```

Document viewer:

- may use native/embedded PDF rendering;
- must not upload documents to a cloud viewer.

Sensitive documents may later receive:

- encrypted local storage;
- biometric gating.

For V1, design the repository API so this can be added without changing screens.

---

# 18. QR mode

When QR mode opens:

- increase brightness;
- keep screen awake;
- show minimal UI;
- restore previous brightness behavior when closing.

QR must come from:

- actual document QR;
- actual encoded ticket data.

Never use visual placeholder QR in production.

---

# 19. Walk Mode state machine

Keep Walk Mode explicit.

Recommended conceptual state:

```text
Idle
Preparing
Active
Paused
Finishing
Completed
```

Associated state:

```text
walkId
currentStop
previousStops
nextStop
locationQuality
audioState
participants
```

Persistence should allow sensible recovery if the Activity is recreated.

---

# 20. Story trigger logic

Story triggers must be idempotent.

Recommended runtime record:

```text
storyId
triggeredAt
playedAt?
dismissedAt?
```

`notifyOncePerTrip` should be enforced locally.

During active Walk Mode:

- current route context may override passive geofence behavior;
- auto-play follows walk preferences.

Outside Walk Mode:

- trigger a normal nearby-story notification.

---

# 21. Participant identity

First app launch:

```text
No participant selected
↓
Who Are You screen
↓
Persist participant ID
↓
Today
```

Changing participant later may live in Settings.

Do not add:

- account creation;
- password;
- email;
- QR;
- invite.

---

# 22. Memory recording

Recommended local layout:

```text
files/memories/
  <memory-id>.m4a
```

Metadata in Room.

Flow:

```text
Tap Record
↓
start immediately
↓
pause/resume optional
↓
finish
↓
persist metadata
↓
show confirmation
```

If recording fails:

- preserve understandable error;
- never lose an already finalized recording.

---

# 23. Permissions

Request only when needed.

Recommended timing:

### Notifications
During onboarding or before first scheduled alert feature.

### Location
Immediately before enabling location-aware stories / Walk Mode.

### Background location
Explain value first, then follow Android-required permission flow.

### Microphone
When user first records a memory.

Avoid requesting every permission on first launch.

---

# 24. Security / privacy

The product is personal.

Still:

- do not log document contents;
- do not log sensitive URLs;
- do not log voice recording contents;
- avoid sending location history to remote services unless needed for sync;
- group sync should contain only minimal operational state;
- do not upload local trip documents to Firebase;
- do not upload audio guides to Firebase;
- do not upload voice memories automatically.

---

# 25. Logging

Use structured local logging in debug builds.

Important events:

```text
trip loaded
participant selected
walk started
walk paused
story triggered
story played
group sync connected
group sync lost
group sync restored
audio playback failure
voice recording started/stopped
document resolution failure
notification scheduled/fired
```

Never log sensitive document payloads.

---

# 26. Build configuration

Keep trip content separate from code.

Recommended:

```text
src/main/assets/trip/
```

For future builds:

```text
./gradlew assembleTrip
```

or a small pre-build script may:

1. validate the trip package;
2. copy JSON/assets;
3. fail on validation errors;
4. build APK.

The exact task name is not important.

The separation is.

---

# 27. Trip Validator

Create after the core app works.

Input:

```text
trip folder
```

Output:

```text
PASS
warnings
errors
```

Must check at minimum:

- JSON Schema validation;
- duplicate IDs;
- missing refs;
- missing assets;
- invalid coordinates;
- missing offline documents;
- audio duration/chapter inconsistencies;
- invalid date ranges;
- bad timeline refs;
- bad walk ordering;
- unresolved mock data in production mode.

Validator can be implemented as:

- Kotlin CLI;
- Node script;
- Python script.

Prefer the simplest maintainable option.

It does not need to share Android runtime code.

---

# 28. Testing strategy

Focus on real risks.

## Unit tests

Prioritize:

- trip parsing;
- current-day calculation;
- Now/Next selection;
- critical-item rules;
- asset resolution;
- story trigger state;
- walk progression;
- expected audio sync position;
- outfit rules.

---

## Instrumented/UI tests

Prioritize:

- Who Are You persistence;
- primary flow navigation;
- Wallet document opening;
- QR mode;
- recording permission;
- voice recording lifecycle;
- notification interaction.

---

## Real-device tests

Mandatory.

Especially:

- screen off during Walk Mode;
- background audio;
- Android killing/recreating UI;
- Bluetooth/headphone controls;
- incoming call interrupts audio;
- network loss;
- network restore;
- two phones reconnecting;
- GPS weak;
- GPS disabled;
- airplane mode;
- voice recording interruptions;
- screen brightness restore after QR;
- large font;
- sunlight usability;
- low battery mode.

---

# 29. Implementation order

## Phase 0 — Project foundation

Implement:

- Compose app;
- navigation shell;
- design tokens;
- `trip.json` models;
- loader;
- repository;
- local database shell;
- sample trip loading.

Done when:

> app opens sample trip and can navigate basic placeholder screens.

---

## Phase 1 — Primary visual foundation

Implement:

- 01 Who Are You
- 02 Today
- 05 Attraction
- shared Field Companion components

Done when:

> screens visually match approved prototype using local sample data.

---

## Phase 2 — Local audio

Implement:

- Media3 service;
- compact/full player;
- chapters;
- background playback;
- headset controls.

Done when:

> attraction audio plays with screen off and survives screen navigation.

---

## Phase 3 — Walk Mode

Implement:

- 06 pre-start
- 07 active walk
- foreground location
- story progression
- 10 story trigger
- 11 end walk

Done when:

> one sample walk works end-to-end on one device.

---

## Phase 4 — Group audio sync

Implement:

- participant presence;
- shared playback state;
- 08 synchronized-start transition;
- 09 shared player;
- reconnect logic.

Done when:

> two real devices can start, pause, resume and reconnect to the same local audio.

---

## Phase 5 — Voice memories

Implement:

- microphone permission;
- recorder;
- Room metadata;
- local file storage;
- 12 memory flow.

Done when:

> a memory can be recorded, saved and replayed locally.

---

## Phase 6 — Operational modules

Implement:

- 03 Full Day
- 04 City
- 13 Wallet
- 14 Document/QR
- 15 Transport
- 16 Accommodation
- 17 Emergency
- 18 Plan B
- 19 More

Done when:

> all canonical screens are functional with sample data.

---

## Phase 7 — Weather + notifications

Implement:

- weather repository;
- cache;
- stale/fallback states;
- local operational notifications;
- nearby-story notification.

Done when:

> network loss does not reduce core trip usefulness.

---

## Phase 8 — Real Balkans package

Replace sample content with real content.

Include:

- all days;
- cities;
- stays;
- transports;
- documents;
- critical items;
- emergency profiles;
- Plan Bs;
- images;
- audio where available.

Run Trip Validator.

---

## Phase 9 — Real-device QA

Run the mandatory field test matrix.

Fix:

- lifecycle;
- battery;
- background;
- permission;
- sync;
- audio;
- location issues.

---

# 30. First vertical slice

The first meaningful release target is:

```text
Who Are You
↓
Today
↓
Attraction
↓
Walk Pre-start
↓
Walk Active
↓
Synchronized Start
↓
Shared Audio
↓
Location Story
↓
Walk End
↓
Voice Memory
↓
Today
```

Use:

- one city;
- one attraction;
- one walk;
- two stories;
- one audio guide;
- two participants.

This slice should be implemented before building the full trip.

---

# 31. Definition of Done — vertical slice

The vertical slice is done when:

- app installs on two Android phones;
- each phone selects a participant;
- selection persists;
- sample trip loads locally;
- Today works offline;
- attraction page works offline;
- audio works with screen off;
- Walk Mode receives location;
- story progression works;
- shared start works on both phones;
- pause/resume propagates;
- network loss does not stop audio;
- reconnect restores shared state;
- memory recording works;
- memory file persists;
- app returns to Today;
- approved design is visually respected.

---

# 32. Definition of Done — V1

V1 is done when:

- all 19 canonical screens are implemented;
- S1–S5 states are supported;
- whole trip works offline except live services;
- real travel documents resolve locally;
- critical notifications are scheduled locally;
- weather supports cache/fallback;
- Walk Mode works with screen off;
- two-device shared playback works reliably;
- voice memories work;
- emergency information is available offline;
- Plan B is available per configured day/entity;
- Trip Validator passes production content;
- real-device QA is complete.

---

# 33. Open technical choices

These may be decided during implementation without a new product/design cycle:

- DI library vs manual dependency wiring;
- exact Room schema;
- exact weather provider;
- PDF rendering implementation;
- Firebase SDK wrapper structure;
- exact audio drift threshold;
- exact active-location interval;
- exact geofence radius tuning;
- exact logging library.

Choose the simplest stable option.

Document only decisions that materially affect maintainability or user behavior.

---

# 34. Rules for the implementation agent

Do:

- use approved visual source;
- keep code simple;
- implement the vertical slice first;
- keep services behind small interfaces;
- test real background behavior early;
- preserve offline behavior;
- validate content automatically.

Do not:

- redesign screens;
- add login;
- add pairing;
- move trip content into Firebase;
- stream bundled audio;
- add generic travel-planner features;
- add architectural complexity because it “might be useful later”;
- block local use while waiting for the network.

---

# 35. Recommended next action

Create the Android repository and implement **Phase 0**.

Before writing screen-specific logic, establish:

```text
Compose shell
Field Companion tokens
Navigation
Trip models
TripRepository
sample-trip.json loading
Room/DataStore skeleton
```

Then immediately proceed to the first three screens:

```text
01 Who Are You
02 Today
05 Attraction
```

The goal is to reach the vertical slice quickly rather than fully engineering every subsystem up front.
