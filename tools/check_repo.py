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

print("PASS: repository skeleton is structurally complete")
print("Participants:", ", ".join(p.get("name", "?") for p in participants))
