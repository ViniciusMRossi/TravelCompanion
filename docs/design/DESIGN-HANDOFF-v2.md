# Travel Companion — Design Handoff v2

Status: **LOCKED FOR IMPLEMENTATION**
Date: 2026-09-01

This document bridges the approved UX/UI into engineering.

It does **not** reopen design exploration.

---

## 1. Approved visual language

Design system: **Field Companion**

Core palette:

- Ink `#16232E`
- Paper `#F5F1E8`
- Surface `#FFFDF8`
- Teal `#1F6F78`
- Oxblood `#7A2E2E`
- Gold `#B8863B`
- Moss `#4C6444`

Typography:

- **Fraunces 600** — editorial titles, cities, attractions, story names
- **Roboto** — operational UI, actions, metadata, timing
- Tabular numerals for time, duration, price, ticket locator

Geometry:

- 14dp cards/buttons
- 20dp hero/editorial blocks
- pill radius for chips/avatars
- minimum 48dp touch target
- 52–56dp primary travel actions
- 76–88dp movement/pressure controls where specified

---

## 2. Interaction principles

### Offline-first

Render local trip content immediately.

Never block the itinerary while waiting for:

- weather;
- synchronization;
- network;
- participant presence.

When online data is stale, explicitly state freshness.

### Synchronization

Synchronization is infrastructure, not a user workflow.

UI terms:

- Sincronizado
- Por perto
- Conectando
- Sincronizando novamente
- Offline

Do not expose:

- Firebase;
- room;
- peer;
- host;
- client;
- latency;
- timestamp.

### Audio

Audio must:

- continue in background;
- continue with screen off;
- survive screen navigation;
- continue locally if sync fails.

### Walk Mode

Walk Mode assumes headphones and limited screen attention.

Priorities:

1. current story;
2. next walking instruction;
3. next story;
4. progress;
5. audio controls;
6. participant presence.

### Critical information

Critical timing is a separate semantic dimension.

A booking can simultaneously be:

- Reserved
- Critical

Never replace status with criticality.

---

## 3. Canonical screens

See `design/SCREEN-INDEX-v2.md`.

The approved design has 19 screens.

The old 18-screen count is superseded.

---

## 4. Runtime content vs runtime state

### Packaged content

Comes from `trip.json` + assets:

- participants available for “Quem é você?”
- days and timeline
- cities
- attractions
- walks
- stories
- audio-guide metadata
- accommodation
- transports
- documents
- Plan B
- emergency info
- useful apps
- fallback weather/clothing guidance

### Device/runtime state

Must **not** live inside `trip.json`:

- selected participant on this device
- completed timeline items
- visited attractions
- current walk state
- audio playback position
- current participant presence
- group sync state
- current live weather
- recorded memories
- granted permissions
- notification preferences

This separation is deliberate.

---

## 5. Assets

All essential assets are local.

Expected categories:

```text
trip/
├── trip.json
├── images/
│   ├── cities/
│   ├── attractions/
│   ├── accommodations/
│   └── walks/
├── audio/
│   ├── cities/
│   ├── attractions/
│   ├── stories/
│   └── walks/
└── documents/
    ├── tickets/
    ├── accommodations/
    ├── insurance/
    └── personal/
```

References in JSON use stable asset IDs, not arbitrary filesystem paths repeated across entities.

---

## 6. Implementation invariants

Engineering must preserve these behaviors:

1. Local itinerary opens without internet.
2. Document/ticket availability does not depend on the network.
3. Audio playback continues if group sync fails.
4. Group reconnection is automatic.
5. No login flow is introduced.
6. No pairing flow is introduced.
7. “Quem é você?” remains first-run identity selection.
8. Walk audio remains usable with screen off.
9. Story trigger failure cannot break the walk.
10. Weather failure cannot break Today.
11. External deep links return the user to the prior app context.
12. Critical-item timing remains visually separate from nominal schedule time.
13. Voice memory requires no mandatory title or text.
14. Emergency screen remains visually direct and low-decoration.
15. Plan B remains calm warning/recovery, not emergency styling.

---

## 7. First implementation slice

Build the approved primary flow end-to-end before secondary modules:

```text
01 → 02 → 05 → 06 → 07 → 08 → 09 → 10 → 11 → 12 → 02
```

This vertical slice should exercise:

- JSON content loading
- navigation
- design tokens/components
- local audio
- background playback
- participant identity
- group sync mock/real adapter boundary
- location/walk state
- story trigger
- voice recording
- local persistence

Do not wait for every real Balkans asset before implementing this slice.

---

## 8. Non-blocking design-content placeholders

The approved prototype still intentionally contains:

- placeholder photography;
- generated/non-real QR patterns;
- representative Day 9 content instead of the entire trip.

These are **content-production tasks**, not reasons to redesign the UI.

Real QR content should be derived from actual ticket data or documents, never invented.

---

## 9. Data contract

`data/trip.schema.json` is the structural contract for packaged travel content.

`data/sample-trip.json` is a prototype-quality example.

The future Trip Validator must additionally check what JSON Schema cannot reliably enforce, including:

- every referenced ID exists;
- every referenced local asset exists;
- no duplicate IDs across relevant registries;
- coordinates are usable;
- audio duration metadata is plausible;
- dates belong to the trip window;
- timeline references point to the correct day/entity;
- critical items contain an actionable deadline/instruction;
- documents marked offline have local assets.

---

## 10. Design change policy

During implementation:

### Allowed without design review

- internal code organization;
- data-loading strategy;
- test architecture;
- dependency injection;
- caching implementation;
- synchronization adapter internals.

### Requires explicit design/product review

- new screens;
- removing screens;
- changing navigation;
- changing primary action hierarchy;
- changing semantic colors;
- changing component geometry materially;
- adding login/pairing;
- changing Walk Mode interaction;
- changing voice memory workflow;
- hiding critical information;
- changing offline behavior visible to users.

---

## 11. Definition of design handoff complete

The design handoff is considered complete when engineering has:

- approved prototype;
- Field Companion design system;
- canonical 19-screen index;
- S1–S5 state definitions;
- implementation invariants;
- trip JSON schema;
- sample trip;
- final QA report.

No additional visual exploration is required before implementation.
