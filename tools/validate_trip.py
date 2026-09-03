#!/usr/bin/env python3
"""Validate a trip package against the schema, then against the rules the
schema cannot express.

JSON Schema cannot tell us that an ID points at something real, that a document
promising offline access actually has its file packaged, that a day numbered
9 really is the ninth day of the trip, or that `Europe/Sarajevo` is a zone the
tz database has heard of. Those are exactly the mistakes that only show up on
the road, so they are checked here.

An unknown time zone always fails, no matter how finished the package is.
Every other finding is a warning while `metadata.contentStatus` is
prototype/draft, and an error once the package claims to be production
content.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import sys
from datetime import date
from zoneinfo import available_timezones

try:
    import jsonschema
except ImportError:
    print("Missing jsonschema. Install with: python -m pip install -r tools/requirements.txt", file=sys.stderr)
    raise SystemExit(2)


def known_timezones() -> set[str]:
    """Every zone name the tz database on this machine can resolve.

    The schema's pattern only rejects malformed names; `Europe/Sarayevo` would
    pass it and then silently shift a bus departure. Windows ships no tz
    database at all, so this can legitimately come back empty — that is a
    broken check, not a clean package, and the caller stops rather than
    reporting a pass it did not earn.
    """
    try:
        return available_timezones()
    except Exception:
        return set()


def _iso(value):
    try:
        return date.fromisoformat(value)
    except (TypeError, ValueError):
        return None


def timezone_problems(trip: dict, timezones: set[str]) -> list[str]:
    """Every declared IANA zone that the tz database does not recognise.

    `Europe/Sarayevo` would pass the schema's pattern and then silently shift
    a bus departure, so this is checked separately from the completeness
    findings below and is always a hard failure — a package that is still
    prototype/draft is not exempt from carrying real time zones.
    """
    problems: list[str] = []

    def tz(value, where):
        if value is not None and value not in timezones:
            problems.append(f"{where}: '{value}' is not an IANA time zone name")

    for city in trip.get("cities", []):
        tz(city.get("timeZone"), f"city '{city['id']}'")

    # A leg can cross two zones, so the endpoints are checked independently.
    for transport in trip.get("transports", []):
        for end in ("origin", "destination"):
            tz(transport.get(end, {}).get("timeZone"), f"transport '{transport['id']}' {end}")

    for day in trip.get("days", []):
        where = f"day '{day['id']}'"
        tz(day.get("timeZone"), where)
        for item in day.get("timeline", []):
            tz(item.get("timeZone"), f"{where} timeline '{item['id']}'")

    return problems


def content_checks(trip: dict, assets_root: Path) -> list[str]:
    """Integrity rules that JSON Schema cannot enforce (time zones excepted;
    see `timezone_problems`)."""
    problems: list[str] = []

    assets = {a["id"]: a for a in trip.get("assets", [])}
    documents = {d["id"]: d for d in trip.get("documents", [])}
    attractions = {a["id"]: a for a in trip.get("attractions", [])}
    walks = {w["id"]: w for w in trip.get("walks", [])}
    transports = {t["id"]: t for t in trip.get("transports", [])}
    accommodations = {a["id"]: a for a in trip.get("accommodations", [])}
    audio_guides = {g["id"]: g for g in trip.get("audioGuides", [])}
    plan_bs = {p["id"]: p for p in trip.get("planBs", [])}
    stories = {s["id"]: s for s in trip.get("stories", [])}

    # Duplicate IDs inside a registry silently shadow content.
    for name, items in (
        ("assets", trip.get("assets", [])),
        ("attractions", trip.get("attractions", [])),
        ("walks", trip.get("walks", [])),
        ("documents", trip.get("documents", [])),
        ("audioGuides", trip.get("audioGuides", [])),
        ("planBs", trip.get("planBs", [])),
        ("stories", trip.get("stories", [])),
        ("days", trip.get("days", [])),
    ):
        seen, dupes = set(), set()
        for item in items:
            if item["id"] in seen:
                dupes.add(item["id"])
            seen.add(item["id"])
        for dupe in sorted(dupes):
            problems.append(f"{name}: duplicate id '{dupe}'")

    def ref(kind, registry, value, where):
        if value is not None and value not in registry:
            problems.append(f"{where}: unknown {kind} '{value}'")

    # A document that promises offline access must have its file packaged.
    for doc in documents.values():
        ref("asset", assets, doc.get("assetId"), f"document '{doc['id']}'")
        asset = assets.get(doc.get("assetId"))
        if doc.get("availableOffline") and asset is not None:
            path = assets_root / asset["path"]
            if not path.is_file():
                problems.append(
                    f"document '{doc['id']}' declares availableOffline but its asset file is "
                    f"missing: {asset['path']}"
                )

    for guide in audio_guides.values():
        ref("asset", assets, guide.get("audioAssetId"), f"audioGuide '{guide['id']}'")

    for attraction in attractions.values():
        where = f"attraction '{attraction['id']}'"
        ref("asset", assets, attraction.get("heroAssetId"), where)
        ref("audioGuide", audio_guides, attraction.get("audioGuideId"), where)
        ref("planB", plan_bs, attraction.get("planBId"), where)

    for walk in walks.values():
        where = f"walk '{walk['id']}'"
        ref("attraction", attractions, walk.get("startAttractionId"), where)
        for stop in walk.get("stops", []):
            ref("story", stories, stop.get("storyId"), where)

    kind_registry = {
        "attraction": attractions,
        "walk": walks,
        "transport": transports,
        "accommodation": accommodations,
    }

    trip_start = _iso(trip.get("trip", {}).get("startDate"))
    trip_end = _iso(trip.get("trip", {}).get("endDate"))
    if trip_start and trip_end and trip_end < trip_start:
        problems.append("trip: endDate is before startDate")

    for day in trip.get("days", []):
        where = f"day '{day['id']}'"
        ref("planB", plan_bs, day.get("planBId"), where)
        for doc_id in day.get("documentIds", []):
            ref("document", documents, doc_id, where)
        for item in day.get("timeline", []):
            item_where = f"{where} timeline '{item['id']}'"
            registry = kind_registry.get(item.get("kind"))
            if registry is not None and item.get("refId") is not None:
                ref(item["kind"], registry, item["refId"], item_where)

        # "Dia 9 de 21" is derived from the trip window, so a dayNumber that
        # disagrees with the calendar would silently mislabel the whole screen.
        day_date = _iso(day.get("date"))
        if trip_start and day_date:
            expected = (day_date - trip_start).days + 1
            if day.get("dayNumber") != expected:
                problems.append(
                    f"{where}: dayNumber {day.get('dayNumber')} does not match its date "
                    f"({day.get('date')} is day {expected} of the trip)"
                )
            if trip_end and not (trip_start <= day_date <= trip_end):
                problems.append(f"{where}: date {day.get('date')} is outside the trip window")

    return problems


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("trip", nargs="?", default="trip-package/starter/trip.json")
    parser.add_argument("--schema", default="trip-package/schema/trip.schema.json")
    parser.add_argument(
        "--assets-root",
        default=None,
        help="Directory the asset paths are relative to (default: the trip file's own folder).",
    )
    args = parser.parse_args()

    trip_path = Path(args.trip)
    trip = json.loads(trip_path.read_text(encoding="utf-8"))
    schema = json.loads(Path(args.schema).read_text(encoding="utf-8"))

    validator_cls = jsonschema.validators.validator_for(schema)
    validator_cls.check_schema(schema)
    validator = validator_cls(schema, format_checker=jsonschema.FormatChecker())
    errors = sorted(validator.iter_errors(trip), key=lambda e: list(e.absolute_path))

    if errors:
        print(f"FAIL: {len(errors)} schema error(s)")
        for error in errors[:50]:
            path = ".".join(str(p) for p in error.absolute_path) or "<root>"
            print(f"- {path}: {error.message}")
        return 1

    timezones = known_timezones()
    if not timezones:
        print(
            "FAIL: no IANA tz database available, so schema 1.1 time zones cannot be "
            "verified. Install it with: python -m pip install -r tools/requirements.txt",
            file=sys.stderr,
        )
        return 2

    # A wrong time zone silently shifts a wall-clock time the first time a
    # leg crosses one; that is real regardless of how finished the package is.
    tz_problems = timezone_problems(trip, timezones)
    if tz_problems:
        print(f"FAIL: {len(tz_problems)} time zone error(s) in {args.trip}")
        for problem in tz_problems:
            print(f"- {problem}")
        return 1

    assets_root = Path(args.assets_root) if args.assets_root else trip_path.parent
    problems = content_checks(trip, assets_root)

    # Production content must be complete; prototype/draft content is still
    # being produced, so the same findings are reported as warnings.
    is_production = trip.get("metadata", {}).get("contentStatus") == "production"

    if problems and is_production:
        print(f"FAIL: {len(problems)} content error(s) in {args.trip}")
        for problem in problems:
            print(f"- {problem}")
        return 1

    print(f"PASS: {args.trip} validates against {args.schema}")
    if problems:
        print(f"WARNING: {len(problems)} content issue(s) (contentStatus is not 'production')")
        for problem in problems:
            print(f"- {problem}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
