# Travel Companion Content Authoring

This directory defines the AI-assisted workflow used to turn a verified itinerary and travel source material into the runtime content package consumed by the Android app.

## Important separation

There are two different packages:

### Authoring package

Designed for humans and AI agents.

Contains:

- research;
- sources;
- confidence;
- conflicts;
- missing information;
- editorial scripts;
- asset requests;
- generation reports.

### Runtime package

Designed for the Android application.

Contains only:

- `trip.json`;
- local images;
- local audio;
- local documents.

The Android app must not depend on authoring reports.

## Read order for a content-generation agent

1. `CONTENT-GENERATOR.md`
2. `RESEARCH-AND-FACT-CHECK-RULES.md`
3. `CONTENT-STYLE-GUIDE.md`
4. `CONTENT-PACKAGE-SPEC.md`
5. `../trip-package/schema/trip.schema.json`
6. `../trip-package/SCHEMA-GUIDE.md`
7. source material made available for the trip

## Short command

Once the AI has access to the trip sources, the user should be able to say:

> Gere o pacote de conteúdo do Travel Companion para esta viagem.

The agent must then follow `CONTENT-GENERATOR.md`.

## Promotion rule

AI writes to:

`trip-package/generated/`

That directory is not versioned — only its `.gitkeep` is (D028) — because the generated `trip.json` carries booking locators, QR payloads and document references derived from `source/private/`.

A human reviews it, on the local files.

Only explicitly approved content is promoted to:

`trip-package/production/`

The content-generation agent must never silently promote its own work to production.
