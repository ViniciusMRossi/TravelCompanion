#!/usr/bin/env python3
"""Preflight checks for a generated authoring package, before it is reviewed
for promotion to `trip-package/production/`.

Time zone validity is delegated to `validate_trip.known_timezones()` so this
script and `validate_trip.py` resolve every IANA name against the same tz
database instead of two independently-written checks silently drifting apart.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from validate_trip import known_timezones

FORBIDDEN_OPERATIONAL_MARKERS = (
    "MOCK-",
    "PLACEHOLDER",
    "TODO",
    "TBD",
    "A CONFIRMAR",
    "+000000",
)

REQUIRED_AUTHORING_FILES = (
    "trip.json",
    "GENERATION-REPORT.md",
    "FACT-CHECK.md",
    "MISSING-INFORMATION.md",
    "assets/asset-manifest.json",
    "audio/audio-manifest.json",
    "research/SOURCES.md",
    "validation/VALIDATION-REPORT.md",
)

def walk_strings(value, path="<root>"):
    if isinstance(value, dict):
        for key, child in value.items():
            yield from walk_strings(child, f"{path}.{key}")
    elif isinstance(value, list):
        for i, child in enumerate(value):
            yield from walk_strings(child, f"{path}[{i}]")
    elif isinstance(value, str):
        yield path, value

def collect_timezones(trip):
    zones = []
    for city in trip.get("cities", []):
        if city.get("timeZone"):
            zones.append((f"cities.{city.get('id')}.timeZone", city["timeZone"]))
    for day in trip.get("days", []):
        if day.get("timeZone"):
            zones.append((f"days.{day.get('id')}.timeZone", day["timeZone"]))
        for item in day.get("timeline", []):
            if item.get("timeZone"):
                zones.append((f"timeline.{item.get('id')}.timeZone", item["timeZone"]))
    for transport in trip.get("transports", []):
        for endpoint in ("origin", "destination"):
            ep = transport.get(endpoint) or {}
            if ep.get("timeZone"):
                zones.append((
                    f"transports.{transport.get('id')}.{endpoint}.timeZone",
                    ep["timeZone"],
                ))
    return zones

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "package",
        nargs="?",
        default="trip-package/generated",
        help="Generated authoring package directory",
    )
    parser.add_argument(
        "--allow-incomplete-authoring-files",
        action="store_true",
        help="Do not fail when standard authoring report files are absent.",
    )
    args = parser.parse_args()

    root = Path(args.package)
    errors = []
    warnings = []

    trip_path = root / "trip.json"
    if not trip_path.exists():
        errors.append(f"Missing {trip_path}")
    else:
        trip = json.loads(trip_path.read_text(encoding="utf-8"))

        if trip.get("schemaVersion") != "1.1":
            errors.append(
                f"Expected schemaVersion 1.1, got {trip.get('schemaVersion')!r}"
            )

        meta = trip.get("metadata", {})
        if meta.get("contentStatus") == "production" and meta.get("isMockContent"):
            errors.append("Production content cannot have isMockContent=true")

        for path, value in walk_strings(trip):
            upper = value.upper()
            for marker in FORBIDDEN_OPERATIONAL_MARKERS:
                if marker in upper:
                    warnings.append(f"Possible unresolved placeholder at {path}: {value!r}")

        zones = collect_timezones(trip)
        if zones:
            timezones = known_timezones()
            if not timezones:
                print(
                    "FAIL: no IANA tz database available, so declared time zones cannot "
                    "be verified. Install it with: python -m pip install -r tools/requirements.txt",
                    file=sys.stderr,
                )
                return 2
            for path, zone in zones:
                if zone not in timezones:
                    errors.append(f"Invalid/unavailable IANA timezone at {path}: {zone}")

    if not args.allow_incomplete_authoring_files:
        for rel in REQUIRED_AUTHORING_FILES:
            if not (root / rel).exists():
                errors.append(f"Missing standard authoring output: {rel}")

    print("Content preflight")
    print("=================")

    for warning in warnings:
        print("WARNING:", warning)

    for error in errors:
        print("ERROR:", error)

    if errors:
        print(f"\nFAIL: {len(errors)} error(s), {len(warnings)} warning(s)")
        return 1

    print(f"\nPASS: {len(warnings)} warning(s)")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
