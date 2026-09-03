# Travel Companion — Content Authoring Package Specification

The authoring package is the auditable workspace between source itinerary material and the Android runtime package.

---

# 1. Directory model

```text
trip-package/
├── schema/
│   └── trip.schema.json
│
├── source/
│   ├── itinerary/
│   ├── user-notes/
│   └── private/
│
├── generated/
│   ├── trip.json
│   ├── GENERATION-REPORT.md
│   ├── FACT-CHECK.md
│   ├── MISSING-INFORMATION.md
│   ├── assets/
│   │   └── asset-manifest.json
│   ├── audio/
│   │   ├── audio-manifest.json
│   │   └── scripts/
│   ├── research/
│   │   └── SOURCES.md
│   └── validation/
│       └── VALIDATION-REPORT.md
│
└── production/
    ├── trip.json
    ├── images/
    ├── audio/
    └── documents/
```

---

# 2. `source/`

Contains authoritative inputs.

## `source/itinerary/`

Suitable for version-controlled itinerary files.

Examples:

- itinerary HTML;
- itinerary Markdown;
- manually curated itinerary JSON;
- transport summary.

## `source/user-notes/`

Optional non-sensitive user preferences/notes used for authoring.

## `source/private/`

Local-only sensitive or non-versioned source documents.

Examples:

- tickets;
- vouchers;
- insurance;
- booking PDFs;
- passport copies.

This directory is ignored by Git by default.

AI may read these files when explicitly available in the execution environment.

Do not copy their sensitive contents into general research reports.

---

# 3. `generated/`

This is AI output.

It is a review workspace, not production.

## `trip.json`

Candidate runtime JSON.

Must validate against the repository schema.

## `GENERATION-REPORT.md`

Executive authoring summary:

- input sources used;
- scope generated;
- coverage;
- known limitations;
- items requiring closer-to-trip re-check;
- production readiness assessment.

## `FACT-CHECK.md`

Contains:

- conflicts;
- important evidence decisions;
- operational facts needing review;
- volatile items;
- high-risk factual notes.

## `MISSING-INFORMATION.md`

A human action list.

Prioritize:

- BLOCKING;
- IMPORTANT;
- OPTIONAL.

## `research/SOURCES.md`

Full authoring provenance.

Not shipped to the app.

## `assets/asset-manifest.json`

Tracks required runtime assets.

## `audio/scripts/`

Editable source scripts before TTS.

## `audio/audio-manifest.json`

Tracks scripts and generated audio files.

## `validation/VALIDATION-REPORT.md`

Records automated + editorial validation.

---

# 4. Production promotion

Human approval is required.

Promotion means copying approved runtime artifacts to:

`trip-package/production/`

The production package should contain only runtime-required assets.

Research notes should not be shipped in the APK.

---

# 5. Asset manifest statuses

Recommended statuses:

- `supplied`
- `needs-user-document`
- `needs-source`
- `needs-generation`
- `script-ready`
- `ready`
- `optional`
- `blocked`

Never use `ready` for a placeholder.

---

# 6. Asset provenance categories

Recommended:

- `user-document`
- `user-photo`
- `official-source`
- `licensed-source`
- `generated`
- `tts`
- `unknown`

Production image/audio licensing responsibility must be clear.

---

# 7. Stable filenames

Prefer human-readable stable asset paths.

Examples:

```text
images/cities/sarajevo-hero.jpg
images/attractions/latin-bridge-hero.jpg
audio/cities/sarajevo-overview.m4a
audio/stories/latin-bridge.m4a
documents/tickets/sarajevo-mostar.pdf
```

The JSON still references asset IDs rather than hard-coding paths throughout entities.

---

# 8. Authoring metadata is not runtime metadata

Confidence, source URLs and research notes normally remain outside `trip.json`.

This keeps:

- runtime JSON small;
- app model clean;
- research auditable;
- sensitive source detail out of the APK.

If runtime needs freshness or display labels, model only those product-required fields explicitly in the schema.
