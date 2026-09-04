#!/usr/bin/env python3
"""Preflight checks for a generated authoring package, before it is reviewed
for promotion to `trip-package/production/`.

Schema validation and time zone validity are delegated to `validate_trip.py`,
so a package this script passes is one that script would also accept. Two
independently-written checks would drift apart, and a preflight that passes a
schema-invalid package is worse than no preflight at all.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from validate_trip import known_timezones, schema_errors, timezone_problem

# Word-shaped markers need boundaries. As a bare substring "TODO" matches
# "todos" and "método" in Portuguese copy, and real editorial content would
# fill the warning list with noise until nobody read it.
WORD_MARKERS = ("PLACEHOLDER", "TBD", "A CONFIRMAR")

# "TODO" is the one marker whose letters spell an ordinary Portuguese word.
# Boundaries fixed "todos", "toda", "todas" and "método", but not the bare
# "todo" of "todo o percurso", "em todo caso", "todo mundo" — which matched
# because the value was upper-cased before the search. A marker is written in
# capitals and the Portuguese word is not, so this one is matched with the
# case it was authored in. The cost is a sentence-initial "Todo" that is really
# a marker, which nobody writes; the gain is that the warning list stays worth
# reading.
CASE_SENSITIVE_WORD_MARKERS = ("TODO",)

# Punctuated markers are distinctive on their own, and must keep matching
# inside a longer run: "+000000" is how "+000000000" gives itself away.
LITERAL_MARKERS = ("MOCK-", "+000000")

# Searched against the upper-cased value, so these are case-insensitive.
MARKER_PATTERNS = tuple(
    re.compile(rf"(?<!\w){re.escape(m)}(?!\w)") for m in WORD_MARKERS
) + tuple(re.compile(re.escape(m)) for m in LITERAL_MARKERS)

# Searched against the value as authored.
CASED_MARKER_PATTERNS = tuple(
    re.compile(rf"(?<!\w){re.escape(m)}(?!\w)") for m in CASE_SENSITIVE_WORD_MARKERS
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
        "--schema",
        default="trip-package/schema/trip.schema.json",
        help="Schema the package is validated against.",
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

    schema = json.loads(Path(args.schema).read_text(encoding="utf-8"))
    # Derived, never a literal: a hardcoded version diverges from the schema at
    # the next bump and the preflight starts rejecting valid packages.
    expected_version = schema["properties"]["schemaVersion"]["const"]

    trip_path = root / "trip.json"
    if not trip_path.exists():
        errors.append(f"Missing {trip_path}")
    else:
        trip = json.loads(trip_path.read_text(encoding="utf-8"))

        if trip.get("schemaVersion") != expected_version:
            errors.append(
                f"Expected schemaVersion {expected_version}, "
                f"got {trip.get('schemaVersion')!r}"
            )

        # Before anything of our own: a package that does not satisfy the
        # schema cannot be judged on completeness.
        for problem in schema_errors(trip, schema)[:50]:
            errors.append(f"Schema: {problem}")

        meta = trip.get("metadata", {})
        if meta.get("contentStatus") == "production" and meta.get("isMockContent"):
            errors.append("Production content cannot have isMockContent=true")

        for path, value in walk_strings(trip):
            upper = value.upper()
            hit = any(pattern.search(upper) for pattern in MARKER_PATTERNS) or any(
                pattern.search(value) for pattern in CASED_MARKER_PATTERNS
            )
            if hit:
                warnings.append(f"Possible unresolved placeholder at {path}: {value!r}")

        # Unconditional: a package declaring no zone at all is the case schema
        # 1.1 exists to prevent, so it must not pass quietly for lack of
        # anything to check.
        timezones = known_timezones()
        if not timezones:
            print(
                "FAIL: no IANA tz database available, so declared time zones cannot "
                "be verified. Install it with: python -m pip install -r tools/requirements.txt",
                file=sys.stderr,
            )
            return 2

        zones = collect_timezones(trip)
        if not zones:
            errors.append(
                "No time zone is declared anywhere in the package; schema 1.1 "
                "requires one on every city, day and transport endpoint"
            )
        for path, zone in zones:
            problem = timezone_problem(zone, timezones)
            if problem:
                errors.append(f"Time zone at {path}: {problem}")

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
