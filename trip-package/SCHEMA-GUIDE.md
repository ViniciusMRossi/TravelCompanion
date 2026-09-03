# Travel Companion — `trip.json` Guide v1

## Purpose

`trip.json` is the **static content package** for one Travel Companion build.

It is not a database of runtime state.

A new trip should be possible by replacing:

- `trip.json`;
- images;
- audio;
- documents.

and producing a new app build.

---

## What belongs in `trip.json`

- trip metadata;
- participants available in “Quem é você?”;
- days;
- timeline content;
- cities;
- attractions;
- walking tours;
- location-triggered stories;
- audio-guide metadata;
- accommodations;
- transports;
- documents;
- Plan B;
- emergency profiles;
- useful-app deep links;
- weather fallback;
- outfit/packing guidance.

## What does NOT belong in `trip.json`

Persist separately on-device or in sync state:

- selected participant;
- playback position;
- current walk;
- online presence;
- synchronization status;
- live weather;
- completed checklist/timeline;
- notification preferences;
- permissions;
- voice memories.

---

## Stable IDs

Every reusable entity uses a stable string ID.

Recommended style:

```text
sarajevo
latin-bridge
walk.sarajevo.historical
story.latin-bridge
ag.latin-bridge
transport.sarajevo-mostar.bus
ticket.sarajevo-mostar
planb.day09
```

Do not use array position as identity.

---

## Asset references

Entities reference assets by `assetId`.

Example:

```json
{
  "heroAssetId": "img.latin-bridge.hero"
}
```

The asset registry maps that ID to the packaged file:

```json
{
  "id": "img.latin-bridge.hero",
  "type": "image",
  "path": "images/attractions/latin-bridge.jpg"
}
```

This allows file paths to change without rewriting every entity.

---

## Timeline model

A day owns ordered timeline items.

A timeline item can refer to:

- attraction;
- transport;
- accommodation;
- meal;
- free time;
- walk;
- custom activity.

`refId` links to the detailed entity where applicable.

Critical information remains separately modeled.

Example:

```json
{
  "startTime": "19:30",
  "kind": "transport",
  "title": "Ônibus para Mostar",
  "refId": "transport.sarajevo-mostar.bus",
  "criticalItemIds": ["critical.bus.sarajevo-mostar"]
}
```

---

## Critical items

Criticality is independent from booking status.

A transport may be:

```text
reserved + critical
```

Critical items support both:

- nominal time;
- actionable deadline.

Example:

```json
{
  "nominalTime": "19:30",
  "actionByTime": "19:00",
  "instruction": "Esteja na estação até 19:00."
}
```

This maps directly to the approved UI.

---

## Walks and stories

A walk contains an ordered list of story IDs.

A story may have a location trigger:

```json
{
  "geo": {
    "latitude": 43.8578,
    "longitude": 18.4289
  },
  "radiusMeters": 100,
  "notifyOncePerTrip": true,
  "autoPlayInWalk": true
}
```

The final Android implementation decides how to realize these triggers.

The content model does not encode Android-specific geofence APIs.

---

## Audio

Audio-guide content references a local audio asset.

Group playback synchronization is **runtime state** and is not stored in trip content.

Audio metadata includes:

- duration;
- chapters;
- optional transcript.

---

## Documents

A document marked `availableOffline: true` must have a local packaged asset.

Sensitive documents may set:

```json
"sensitive": true
```

The future implementation can use this to apply biometric/encryption behavior.

QR supports:

- `none`;
- `embedded`;
- `generated-from-text`.

Never populate production QR data from a visual mock.

---

## Plan B

Plan B is structured as:

- human scenario;
- reassuring summary;
- ordered steps;
- optional deadlines;
- actions.

This deliberately matches the approved calm recovery UI.

---

## JSON Schema vs Trip Validator

`trip.schema.json` validates structure and value types.

It cannot reliably prove every semantic relationship.

A future **Trip Validator** should additionally verify:

- unique IDs;
- every referenced entity exists;
- every asset exists on disk;
- offline documents have local assets;
- referenced audio files exist;
- chapter start times fit within audio duration;
- dates and day numbers are consistent;
- walk stop ordering is valid;
- story coordinates are present when trigger is configured;
- transport arrival is after departure;
- critical action deadlines make sense;
- no unresolved mock values remain in a production package.

---

## Schema 1.1 — every local time names its zone

`schemaVersion` is now `"1.1"`.

Every `time` and `localDateTime` in the package is a **wall-clock time**, never
an instant. Until 1.1 nothing said *whose* wall clock, which is only harmless
while a trip stays inside one zone. The Balkans itinerary does not: a bus that
leaves Sarajevo at 19:30 and arrives in a different zone would otherwise be
rendered against the phone's zone and quietly move.

1.1 adds one field, in four places:

| Where | Required | Meaning |
| --- | --- | --- |
| `city.timeZone` | yes | The zone the city lives in. |
| `day.timeZone` | yes | The zone every timeline time on that day is written in. |
| `transportEndpoint.timeZone` | yes | The zone that endpoint's `dateTime` is written in — declared on **both** ends, because one leg can cross two zones. |
| `timelineItem.timeZone` | no | Override for an item that does not happen in its day's zone. |

The value is an IANA name such as `Europe/Sarajevo`.

### Inheritance

A timeline item **inherits its day**. Omit `timeZone` unless the item genuinely
leaves the day's zone — a night bus crossing a border is the case it exists
for. An override that merely repeats the day is redundant but valid; it is
not flagged by `validate_trip.py`.

### The name is checked, not just its shape

The schema's `$defs/timeZone` pattern only rejects malformed names;
`Europe/Sarayevo` matches it perfectly. `tools/validate_trip.py` resolves every
declared zone against the tz database and **fails on the ones that do not
exist, regardless of `contentStatus`** — unlike the other content checks,
an unknown zone is never merely a prototype/draft warning.
`tools/content_preflight.py` performs the same check, through the same
function, so the two scripts cannot disagree.

That database is not present on Windows by default, so `tools/requirements.txt`
declares a minimum `tzdata` version. If it is missing, the validator **stops**
rather than reporting a pass it did not earn.

### Migrating a 1.0 package

1. `schemaVersion` → `"1.1"`.
2. Add `timeZone` to every city, every day and both endpoints of every
   transport.
3. Add nothing to timeline items that stay in their day's zone.

No other field changes; 1.0 content is otherwise valid 1.1 content.

---

## Prototype sample

`sample-trip.json` intentionally contains prototype/mock data.

It is designed to validate:

- structure;
- UI integration;
- vertical-slice development.

Do not treat its operational details as verified travel facts.
