#!/usr/bin/env python3
"""Validate a trip package against the schema, then against the rules the
schema cannot express.

JSON Schema cannot tell us that an ID points at something real, that a document
promising offline access actually has its file packaged, that a day numbered
9 really is the ninth day of the trip, or that `Europe/Sarajevo` is a zone the
tz database has heard of. Those are exactly the mistakes that only show up on
the road, so they are checked here.

An unknown time zone always fails, no matter how finished the package is, and
so does a declared audio duration that the packaged file contradicts. Every
other finding is a warning while `metadata.contentStatus` is prototype/draft,
and an error once the package claims to be production content.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import sys
import struct
import wave
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


# In the tz database but not regions: every name under `Etc/` and these
# top-level aliases is a fixed offset with no DST rule, which is the mistake
# schema 1.1 exists to prevent (CONTENT-GENERATOR.md §8). `Etc/GMT+2` also
# has its sign inverted from what most people expect.
FIXED_OFFSET_ZONE_NAMES = frozenset(
    {"UTC", "UCT", "GMT", "GMT0", "GMT+0", "GMT-0", "Greenwich", "Universal", "Zulu"}
)


def timezone_problem(value: str, timezones: set[str]) -> str | None:
    """Why this zone name cannot carry a wall-clock time, or None if it can.

    Shared with `content_preflight.py` so the two scripts cannot drift into
    disagreeing about what a usable zone is.
    """
    if value not in timezones:
        return f"'{value}' is not an IANA time zone name"
    if value.startswith("Etc/") or value in FIXED_OFFSET_ZONE_NAMES:
        return (
            f"'{value}' is a fixed UTC offset, not a region. Schema 1.1 needs the "
            "region whose clock the time is written in, so daylight saving is "
            "applied (for example Europe/Sarajevo)"
        )
    return None


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
        if value is None:
            return
        problem = timezone_problem(value, timezones)
        if problem:
            problems.append(f"{where}: {problem}")

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


# How far a declared `durationSeconds` may sit from the file it describes.
#
# Two seconds, and the two are not the same second. One is rounding: the
# declared number is whole seconds and the file's is not, so a 719.6s recording
# is honestly written as either 719 or 720. The other is the encoder: AAC codes
# in 1024-sample frames and pads the last one, so a re-encode of the same
# narration moves the end by a fraction of a second, and an `edts` this parser
# does not read can move it again.
#
# Nothing real lives above that. The defect this check exists for is a number
# typed from the wrong take, measured before the last edit, or carried over from
# another guide, and those are wrong by tens of seconds or by minutes. A
# tolerance loose enough to swallow a thirty-second error would swallow the only
# error there is.
AUDIO_DURATION_TOLERANCE_SECONDS = 2.0

# `mvhd` marks an unknown duration with an all-ones field. Fragmented MP4 does
# that and keeps the real length in the fragments, which this parser does not
# walk - an unknown duration is no duration, not a duration of 2^32 timescale
# units.
_MVHD_UNKNOWN = {0: 0xFFFFFFFF, 1: 0xFFFFFFFFFFFFFFFF}


def _mp4_atoms(handle, start: int, end: int):
    """Yield `(fourcc, payload_start, atom_end)` for the atoms spanning start..end.

    MP4 is a tree of length-prefixed boxes, so walking it needs no library:
    four bytes of size, four of name, then either the payload or more boxes.
    A size of 1 means the real 64-bit size follows the name; a size of 0 means
    the box runs to the end of its parent.
    """
    offset = start
    while offset + 8 <= end:
        handle.seek(offset)
        header = handle.read(8)
        if len(header) < 8:
            return
        size = struct.unpack(">I", header[:4])[0]
        name = header[4:8]
        payload = offset + 8
        if size == 1:
            extended = handle.read(8)
            if len(extended) < 8:
                return
            size = struct.unpack(">Q", extended)[0]
            payload = offset + 16
        elif size == 0:
            size = end - offset
        # A size that does not cover its own header, or that runs past the
        # parent, means this is not the file it claims to be. Stop rather than
        # seek somewhere arbitrary.
        if size < payload - offset or offset + size > end:
            return
        yield name, payload, offset + size
        offset += size


def _mp4_duration_seconds(path: Path) -> float | None:
    """Seconds from the `mvhd` atom of an MP4/m4a file, or None if unreadable.

    This is the format the real audio guides will be in - `targetAudioPath` ends
    in `.m4a` - and the movie header carries the length as a timescale and a
    duration in those units. Pure stdlib on purpose: a validator that needs
    ffmpeg installed is a validator that stops being run.
    """
    try:
        size = path.stat().st_size
        with path.open("rb") as handle:
            for name, start, end in _mp4_atoms(handle, 0, size):
                if name != b"moov":
                    continue
                for child, child_start, child_end in _mp4_atoms(handle, start, end):
                    if child != b"mvhd":
                        continue
                    handle.seek(child_start)
                    body = handle.read(child_end - child_start)
                    if not body:
                        return None
                    version = body[0]
                    if version == 0 and len(body) >= 20:
                        timescale, duration = struct.unpack(">II", body[12:20])
                    elif version == 1 and len(body) >= 32:
                        timescale, duration = struct.unpack(">IQ", body[20:32])
                    else:
                        return None
                    if timescale <= 0 or duration in (0, _MVHD_UNKNOWN.get(version)):
                        return None
                    return duration / timescale
    except OSError:
        return None
    return None


def _wav_duration_seconds(path: Path) -> float | None:
    """Seconds from a RIFF/WAVE header, or None if `wave` cannot read it."""
    try:
        with wave.open(str(path), "rb") as handle:
            rate = handle.getframerate()
            frames = handle.getnframes()
    except (wave.Error, OSError, EOFError):
        return None
    if rate <= 0 or frames <= 0:
        return None
    return frames / rate


# Extensions this script can time. Anything else - `.mp3` above all, which needs
# its frames counted - is reported as a check that did not happen.
AUDIO_DURATION_READERS = {
    ".wav": _wav_duration_seconds,
    ".m4a": _mp4_duration_seconds,
    ".m4b": _mp4_duration_seconds,
    ".mp4": _mp4_duration_seconds,
}


def audio_duration_seconds(path: Path) -> float | None:
    """The file's real length in seconds, or None if it cannot be read here."""
    reader = AUDIO_DURATION_READERS.get(path.suffix.lower())
    return reader(path) if reader else None


def audio_duration_problems(trip: dict, assets_root: Path):
    """Declared audio lengths and chapter marks against the packaged files.

    `durationSeconds` - not the file - drives the progress bar and the duration
    label on screen, so a number that disagrees with the recording produces a
    bar that fills early and then sits at the end while the narration continues,
    or one that never arrives. Nothing else in the pipeline compares the two.

    Always an error, prototype/draft included, on the same reasoning as the time
    zone check and not the offline-document one: those findings are about
    content that is *not there yet* and will be, while this one fires only when
    both facts are already present and contradict each other. A progress bar
    that lies is a defect at every stage, and no later step resolves it.

    Returns `(problems, checked, skipped)`, where `skipped` names each guide
    whose file could not be timed and why. A format this script cannot read is
    an absent check, never a failure - it must say so rather than reject a file
    for being an `.mp3`.
    """
    assets = {a["id"]: a for a in trip.get("assets", [])}
    problems: list[str] = []
    checked: list[str] = []
    skipped: list[tuple[str, str]] = []

    for guide in trip.get("audioGuides", []):
        where = f"audioGuide '{guide['id']}'"
        declared = guide.get("durationSeconds")
        chapters = guide.get("chapters") or []

        # Ordering needs no file: a chapter list that goes backwards is a
        # contradiction inside the JSON, and the sheet is drawn in list order.
        previous = None
        for index, chapter in enumerate(chapters, start=1):
            start = chapter.get("startSeconds")
            if start is None:
                continue
            if previous is not None and start <= previous:
                problems.append(
                    f"{where}: chapter {index} starts at {start}s, at or before the "
                    f"chapter before it at {previous}s"
                )
            previous = start

        asset = assets.get(guide.get("audioAssetId"))
        actual = None
        if asset is None:
            # The unresolved reference is already reported by content_checks.
            skipped.append((guide["id"], "no audio asset to measure"))
        else:
            path = assets_root / asset["path"]
            if not path.is_file():
                skipped.append((guide["id"], f"file not packaged: {asset['path']}"))
            elif path.suffix.lower() not in AUDIO_DURATION_READERS:
                skipped.append(
                    (guide["id"], f"no duration reader for '{path.suffix}': {asset['path']}")
                )
            else:
                actual = audio_duration_seconds(path)
                if actual is None:
                    skipped.append(
                        (guide["id"], f"could not read a duration from {asset['path']}")
                    )
                else:
                    checked.append(guide["id"])
                    if (
                        declared is not None
                        and abs(declared - actual) > AUDIO_DURATION_TOLERANCE_SECONDS
                    ):
                        problems.append(
                            f"{where}: declares durationSeconds {declared} but "
                            f"{asset['path']} is {actual:.1f}s (off by "
                            f"{abs(declared - actual):.1f}s, tolerance "
                            f"{AUDIO_DURATION_TOLERANCE_SECONDS:g}s)"
                        )

        # A chapter at or past the end is a button that leads nowhere. Measured
        # against the file when it can be read and against the declared length
        # otherwise, so the mark is still checked before the audio is produced.
        end = actual if actual is not None else declared
        source = "the file" if actual is not None else "the declared duration"
        if end is not None:
            for index, chapter in enumerate(chapters, start=1):
                start = chapter.get("startSeconds")
                if start is not None and start >= end:
                    problems.append(
                        f"{where}: chapter {index} starts at {start}s, at or past the "
                        f"end of {source} ({end:.1f}s)"
                    )

    return problems, checked, skipped


def schema_errors(trip: dict, schema: dict) -> list[str]:
    """Schema violations as `path: message`, ordered by where they occur.

    Extracted so `content_preflight.py` can run the same validation instead of
    passing a package this script would reject.
    """
    validator_cls = jsonschema.validators.validator_for(schema)
    validator_cls.check_schema(schema)
    validator = validator_cls(schema, format_checker=jsonschema.FormatChecker())
    return [
        f"{'.'.join(str(p) for p in error.absolute_path) or '<root>'}: {error.message}"
        for error in sorted(validator.iter_errors(trip), key=lambda e: list(e.absolute_path))
    ]


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

    errors = schema_errors(trip, schema)
    if errors:
        print(f"FAIL: {len(errors)} schema error(s)")
        for error in errors[:50]:
            print(f"- {error}")
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

    # Always an error, for the reason written on `audio_duration_problems`.
    audio_problems, audio_checked, audio_skipped = audio_duration_problems(trip, assets_root)
    if audio_problems:
        print(f"FAIL: {len(audio_problems)} audio duration/chapter error(s) in {args.trip}")
        for problem in audio_problems:
            print(f"- {problem}")
        return 1

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
    if audio_checked or audio_skipped:
        print(
            f"Audio: {len(audio_checked)} guide(s) timed against a packaged file, "
            f"{len(audio_skipped)} not timed"
        )
        for guide_id, why in audio_skipped:
            print(f"- no duration check for '{guide_id}': {why}")
    if problems:
        print(f"WARNING: {len(problems)} content issue(s) (contentStatus is not 'production')")
        for problem in problems:
            print(f"- {problem}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
