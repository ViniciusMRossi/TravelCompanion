#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = [
    "CLAUDE.md",
    "docs/START-HERE.md",
    "docs/design/SOURCE-OF-TRUTH.md",
    "docs/design/DESIGN-HANDOFF-v2.md",
    "docs/design/TELAS-E-FUNCIONALIDADES-APPROVED.md",
    "docs/design/prototype/Travel Companion - Fluxo Principal.dc.html",
    "docs/design/prototype/Travel Companion - Telas Secundárias.dc.html",
    "docs/technical/TECHNICAL-IMPLEMENTATION-BRIEF-v1.md",
    "trip-package/schema/trip.schema.json",
    "app/src/main/assets/trip/trip.json",
    "tools/validate_trip.py",
    # Content pipeline: source -> generated -> human review -> production.
    # The folders are part of the contract, so losing one is a failure, not a
    # detail. private/.gitignore is what keeps the traveller's own documents
    # out of the repository; production/.gitkeep is the single versioned file
    # in a folder whose contents are ignored.
    "trip-package/source/itinerary/.gitkeep",
    "trip-package/source/user-notes/.gitkeep",
    "trip-package/source/private/.gitignore",
    "trip-package/source/README.md",
    "trip-package/source/SOURCE-INVENTORY.template.md",
    "trip-package/generated/.gitkeep",
    "trip-package/production/.gitkeep",
    # AI content-authoring workflow: the rules an AI follows to turn source
    # material into a candidate package, and the tool that checks its output
    # before human review. Losing one silently turns generation freeform.
    "tools/content_preflight.py",
    "content/README.md",
    "content/CONTENT-GENERATOR.md",
    "content/CONTENT-STYLE-GUIDE.md",
    "content/RESEARCH-AND-FACT-CHECK-RULES.md",
    "content/CONTENT-PACKAGE-SPEC.md",
    "content/SHORT-PROMPT.md",
    "content/templates/GENERATION-REPORT.template.md",
    "content/templates/FACT-CHECK.template.md",
    "content/templates/MISSING-INFORMATION.template.md",
    "content/templates/SOURCES.template.md",
    "content/templates/asset-manifest.example.json",
    "content/templates/audio-manifest.example.json",
    "content/templates/audio-guide.template.md",
    "content/templates/story.template.md",
]

missing = [p for p in REQUIRED if not (ROOT / p).exists()]
if missing:
    print("FAIL: missing required repository files:")
    for p in missing:
        print(f"- {p}")
    raise SystemExit(1)

trip = json.loads((ROOT / "app/src/main/assets/trip/trip.json").read_text(encoding="utf-8"))
participants = trip.get("trip", {}).get("participants", [])
if not participants:
    raise SystemExit("FAIL: starter trip has no participants")

# A package left behind on an older schemaVersion still parses and still looks
# fine on screen; it just stops carrying whatever the newer version added.
schema = json.loads((ROOT / "trip-package/schema/trip.schema.json").read_text(encoding="utf-8"))
expected_version = schema["properties"]["schemaVersion"]["const"]
stale = [
    p
    for p in (
        "app/src/main/assets/trip/trip.json",
        "trip-package/sample/sample-trip.json",
        "trip-package/starter/trip.json",
    )
    if json.loads((ROOT / p).read_text(encoding="utf-8")).get("schemaVersion") != expected_version
]
if stale:
    print(f"FAIL: trip packages not on schemaVersion {expected_version}:")
    for p in stale:
        print(f"- {p}")
    raise SystemExit(1)

print("PASS: repository skeleton is structurally complete")
print(f"Schema version: {expected_version}")
print("Participants:", ", ".join(p.get("name", "?") for p in participants))
