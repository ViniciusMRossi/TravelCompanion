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
from collections import Counter, defaultdict
from datetime import date
from math import asin, cos, radians, sin, sqrt
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


# How far a coordinate may sit from the nearest other coordinate in the same
# city before it is called wrong.
#
# The schema's `$defs/geoPoint` already rejects a latitude outside -90..90 and a
# longitude outside -180..180, and schema validation runs first, so a range
# check here would add nothing. What passes today are the errors that stay
# inside the range: latitude and longitude transposed, a flipped sign, a
# transposed digit. Nothing catches any of them, and each costs a story that
# never fires or one that fires where nobody is standing.
#
# A hundred kilometres, measured against the *nearest* sibling rather than a
# centroid: with two points and one of them wrong the centroid sits between them
# and blames both equally, while the distance between the pair is the signal.
# The number is two orders of magnitude above what correct content produces -
# the four packaged Sarajevo points span 428 m - and an order of magnitude below
# the smallest of the three defects, which are 1000 km (a transposed integer
# digit), 3691 km (a transposition) and 9754 km (a flipped sign). Twenty-five
# kilometres would also catch a transposed decimal, and would sit inside the
# legitimate spread of a large sparse city: days 1 and 20 of this trip are Sao
# Paulo, whose car park is in Guarulhos, ~25 km from the centre. A guard that
# shouts at correct content is a guard people switch off.
#
# Out of reach, on purpose: a transposed decimal - 43.8576 written as 43.5878 -
# moves the point 30 km and passes. That error needs a map, not a validator, and
# it is recorded here the way `.mp3` is recorded as a format this script cannot
# time.
COORDINATE_CLUSTER_LIMIT_METERS = 100_000.0

# How close swapping latitude and longitude must bring a flagged point to that
# same neighbour before the message says so. Five kilometres: a transposition
# that resolves to within a city is a diagnosis, not a coincidence, and naming
# it turns an accusation into an instruction.
COORDINATE_SWAP_MATCH_METERS = 5_000.0

EARTH_RADIUS_METERS = 6_371_000.0


def distance_meters(a: tuple[float, float], b: tuple[float, float]) -> float:
    """Great-circle distance in metres between two (latitude, longitude) pairs.

    The same haversine `domain/walk/WalkGeo.kt` uses to decide a story trigger,
    written again here rather than taking a geo dependency for six lines.
    """
    lat1, lat2 = radians(a[0]), radians(b[0])
    d_lat = lat2 - lat1
    d_lon = radians(b[1] - a[1])
    h = sin(d_lat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(d_lon / 2) ** 2
    return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(h)))


def _point(geo) -> tuple[float, float] | None:
    if not isinstance(geo, dict):
        return None
    latitude, longitude = geo.get("latitude"), geo.get("longitude")
    if latitude is None or longitude is None:
        return None
    return (float(latitude), float(longitude))


def geo_points(trip: dict):
    """Every packaged coordinate, split by whether it declares a city.

    Returns `(anchored, unanchored)`. An anchored entry is
    `(cityId, label, field, point)`; an unanchored one is `(label, field)`.

    `city` carries no `geo`, so the anchor has to come from the points
    themselves, grouped by the city they belong to. Four of the five places a
    coordinate can appear declare a required `cityId` - attraction, walk, story
    and accommodation. `transportEndpoint` declares none, and there is no honest
    way to infer one: a leg's two endpoints are legitimately in different
    countries, `days[].cityIds` puts Amsterdam, Corfu, Sarande and Ksamil on one
    day, and a time zone is a political boundary that spans 60 degrees of
    longitude in China. So an endpoint's coordinate is reported as a check that
    did not run, never as a pass it did not earn.
    """
    anchored: list[tuple[str, str, str, tuple[float, float]]] = []
    unanchored: list[tuple[str, str]] = []

    for attraction in trip.get("attractions", []):
        point = _point((attraction.get("location") or {}).get("geo"))
        if point:
            anchored.append(
                (
                    attraction.get("cityId"),
                    f"attraction '{attraction['id']}'",
                    "location.geo",
                    point,
                )
            )
    for walk in trip.get("walks", []):
        point = _point((walk.get("startLocation") or {}).get("geo"))
        if point:
            anchored.append(
                (walk.get("cityId"), f"walk '{walk['id']}'", "startLocation.geo", point)
            )
    for story in trip.get("stories", []):
        point = _point((story.get("trigger") or {}).get("geo"))
        if point:
            anchored.append(
                (story.get("cityId"), f"story '{story['id']}'", "trigger.geo", point)
            )
    for stay in trip.get("accommodations", []):
        point = _point((stay.get("location") or {}).get("geo"))
        if point:
            anchored.append(
                (stay.get("cityId"), f"accommodation '{stay['id']}'", "location.geo", point)
            )
    for transport in trip.get("transports", []):
        for end in ("origin", "destination"):
            point = _point(((transport.get(end) or {}).get("location") or {}).get("geo"))
            if point:
                unanchored.append((f"transport '{transport['id']}' {end}", "location.geo"))

    return anchored, unanchored


def _swap_gap(point: tuple[float, float], anchor: tuple[float, float]) -> float | None:
    """Distance from `point` with its two values transposed to `anchor`.

    None when the transposition is not itself a valid latitude - the schema
    rejects those before this script sees them, so there is nothing to diagnose.
    """
    if abs(point[1]) > 90:
        return None
    return distance_meters((point[1], point[0]), anchor)


def coordinate_problems(trip: dict):
    """Coordinates that contradict the rest of their own city.

    Returns `(problems, checked, skipped)`, where `skipped` names each city and
    each transport endpoint whose coordinate could not be anchored, and why - a
    city with one packaged point has nothing to compare it with, and saying so
    out loud is the difference between an absent check and a hole.

    Always an error, prototype/draft included, for the reason argued on
    `audio_duration_problems`: the point and its siblings are all already in the
    package and contradict each other, and no later step of the pipeline
    resolves it.

    Coincident points are correct content, not a finding. A walk that starts at
    its first stop repeats that stop's trigger, and a story that narrates one
    attraction sits on top of it; both happen in the packaged trips. A nearest
    neighbour at zero metres is the strongest agreement there is, so nothing is
    reported for it.

    One finding per pair, and the subject of it depends on whether the city has
    a cluster to compare against. Where it does, the wrong point is the one
    standing alone and the finding names it. Where it does not - two points, or
    three all far from each other - the finding names both ends and says that
    neither has an anchor, because the swap test cannot break that tie:
    transposing either half of a transposed pair lands on the other half by
    construction.

    Then the city is measured as a whole, by the distance between its two
    furthest points, and only where the nearest neighbour found nothing. That
    is the check for the group rewritten in one go, which the nearest neighbour
    cannot see at all - two coordinates wrong the same way are each other's
    close neighbour.
    """
    anchored, unanchored = geo_points(trip)
    problems: list[str] = []
    checked: list[str] = []
    skipped: list[tuple[str, str]] = []

    by_city: dict[str, list[tuple[str, str, tuple[float, float]]]] = defaultdict(list)
    for city_id, label, field, point in anchored:
        by_city[city_id].append((label, field, point))

    for city_id in sorted(by_city, key=lambda value: (value is None, value)):
        points = by_city[city_id]
        if len(points) < 2:
            label, field, _ = points[0]
            skipped.append(
                (
                    f"city '{city_id}'",
                    f"only one packaged coordinate ({label} {field}), so there is nothing "
                    "in its own city to compare it with",
                )
            )
            continue
        checked.append(f"city '{city_id}' ({len(points)} coordinates)")

        # One finding per *pair*, not per point. A city holding exactly two
        # coordinates, one of them wrong, makes each of them the other's
        # nearest neighbour, and reporting that twice accuses the correct one
        # as loudly as the wrong one.
        far: dict[int, tuple[int, float]] = {}
        for index, (_, _, point) in enumerate(points):
            near_index, near = min(
                ((other, points[other]) for other in range(len(points)) if other != index),
                key=lambda candidate: distance_meters(point, candidate[1][2]),
            )
            gap = distance_meters(point, near[2])
            if gap > COORDINATE_CLUSTER_LIMIT_METERS:
                far[index] = (near_index, gap)

        pairs: dict[frozenset[int], float] = {}
        for index, (near_index, gap) in far.items():
            pairs.setdefault(frozenset((index, near_index)), gap)

        for key in sorted(pairs, key=sorted):
            gap = pairs[key]
            first, second = sorted(key)
            subjects = [index for index in (first, second) if index in far]

            if len(subjects) == 1:
                # The city has a cluster and this point is not in it, so the
                # cluster is the anchor and the point is the accusation.
                index = subjects[0]
                near_index = first if index == second else second
                label, field, point = points[index]
                near_label, _, near_point = points[near_index]
                message = (
                    f"{label}: {field} ({point[0]}, {point[1]}) is {gap / 1000:.1f} km from "
                    f"the nearest other point in city '{city_id}' ({near_label} at "
                    f"{near_point[0]}, {near_point[1]})."
                )
                swapped_gap = _swap_gap(point, near_point)
                if swapped_gap is not None and swapped_gap <= COORDINATE_SWAP_MATCH_METERS:
                    # A transposition that resolves is not a suspicious
                    # distance, it is an answer, and saying so saves opening a
                    # map.
                    message += (
                        f" Swapping latitude and longitude puts it "
                        f"{swapped_gap / 1000:.1f} km from that point - the two values look "
                        "transposed."
                    )
                problems.append(message)
                continue

            # Neither member has anything close by, so there is no cluster to
            # call the city and no way to choose between them. The wording is
            # deliberately about *that* and not about the city holding exactly
            # two points: the same branch is reached by three or more points
            # that are all far from each other, and "no third coordinate"
            # would then be a sentence the check never tested and that the
            # package contradicts.
            label, field, point = points[first]
            other_label, other_field, other_point = points[second]
            message = (
                f"city '{city_id}': {label} {field} ({point[0]}, {point[1]}) and "
                f"{other_label} {other_field} ({other_point[0]}, {other_point[1]}) are "
                f"{gap / 1000:.1f} km apart, and neither has a near neighbour in the "
                "city to anchor it, so nothing here says which of them belongs there."
            )
            swapped_gap = _swap_gap(point, other_point)
            if swapped_gap is not None and swapped_gap <= COORDINATE_SWAP_MATCH_METERS:
                message += (
                    f" Swapping either one's latitude and longitude puts them "
                    f"{swapped_gap / 1000:.1f} km apart - one of the two has its values "
                    "transposed."
                )
            problems.append(message)

        # The nearest neighbour asks "does this point have a friend nearby?",
        # which is not the same question as "is this city one place?". Two
        # coordinates wrong the same way become each other's friend and neither
        # is far from anything: transposing two of the four packaged Sarajevo
        # points leaves two tight groups 3691 km apart and every single point
        # with a neighbour at 300 m. That is the shape a script or an agent
        # produces when it rewrites a group, which is the common one - the
        # nearest neighbour catches the transposition typed by hand, which is
        # the rare one.
        #
        # So the city is also measured against itself as a whole: the distance
        # between its two furthest points, against the same 100 km. Correct
        # content spans 429 m in the sample and 296 m in the real package, and
        # a large sparse city would span ~25 km, so the same two orders of
        # magnitude of headroom hold on both sides.
        #
        # Only when the nearest neighbour found nothing in this city. One wrong
        # point stretches the diameter too, and reporting it twice would be two
        # findings for one defect; where the nearest neighbour can name the
        # culprit, its message is the better one. The cost is that a city
        # carrying *both* a lone wrong point and a transposed group reports only
        # the lone point, and the group surfaces on the next run once that is
        # fixed - stated here rather than left to be discovered.
        if not any(index in far for index in range(len(points))):
            (far_label, far_field, far_point), (near_label_2, near_field_2, near_point_2) = max(
                ((a, b) for a in points for b in points),
                key=lambda pair: distance_meters(pair[0][2], pair[1][2]),
            )
            span = distance_meters(far_point, near_point_2)
            if span > COORDINATE_CLUSTER_LIMIT_METERS:
                message = (
                    f"city '{city_id}': its coordinates span {span / 1000:.1f} km, from "
                    f"{far_label} {far_field} ({far_point[0]}, {far_point[1]}) to "
                    f"{near_label_2} {near_field_2} ({near_point_2[0]}, {near_point_2[1]}). "
                    "Every point here has a close neighbour, so the city holds more than "
                    "one group of coordinates and nothing says which group is the city."
                )
                swapped_gap = _swap_gap(far_point, near_point_2)
                if swapped_gap is not None and swapped_gap <= COORDINATE_SWAP_MATCH_METERS:
                    message += (
                        f" Swapping either end's latitude and longitude puts the two "
                        f"{swapped_gap / 1000:.1f} km apart - one group has its values "
                        "transposed."
                    )
                problems.append(message)

    for label, field in unanchored:
        skipped.append((label, f"{field} declares no city, so there is no cluster to anchor it to"))

    return problems, checked, skipped


def story_guide_title_problems(trip: dict) -> list[str]:
    """A story and its audio guide carrying the same title.

    The player does not choose between the two, it shows both at once:
    `WalkModeController.kt:231` passes `subtitle = story.title` into
    `audioGuideRequest`, `AudioGuideRequest.kt:49` sets `title = guide.title`,
    `AppNavigation.kt:462-463` hands the pair to the compact player, and
    `Media3AudioEngine.kt:70-73` puts them in the media session as title and
    artist. Equal titles therefore print the same line twice, in the compact
    player and on the lock screen - which is Walk Mode with headphones and the
    screen off, the case the whole feature was drawn for.

    The rule is the pair, so it is read from the story side: a guide nobody
    points at has nothing to collide with and never enters. Titles are compared
    stripped and case-insensitively, because a title retyped rather than copied
    is the same duplicated line on the screen.

    Reported through `content_checks`, so it is a warning until the package
    declares `production` - see the argument in D102.
    """
    guides = {g["id"]: g for g in trip.get("audioGuides", [])}
    problems: list[str] = []

    for story in trip.get("stories", []):
        guide = guides.get(story.get("audioGuideId"))
        if guide is None:
            continue
        story_title = (story.get("title") or "").strip()
        guide_title = (guide.get("title") or "").strip()
        if not story_title or story_title.casefold() != guide_title.casefold():
            continue
        problems.append(
            f"story '{story['id']}' and audioGuide '{guide['id']}' carry the same title "
            f"({story_title!r}); the player shows the guide's as the title and the "
            "story's as the subtitle, so the lock screen prints the line twice"
        )

    return problems


def walk_order_problems(trip: dict):
    """Walk stop numbering that contradicts itself.

    Returns `(errors, warnings)`, and the split is D095's argument applied
    twice. `WalkModeState.kt` sorts the stops by `order` before showing them, so
    nothing here is about a list being read in the sequence it was typed in - it
    is about a numbering that cannot produce one.

    A duplicate is an error at every stage. Two stops both numbered 2 leave
    their on-screen order decided by `sortedBy` being stable, which is correct
    by accident; both facts are already present, they contradict each other, and
    no later step resolves it.

    A gap, or a first stop that is not 1, is a warning until production. Stops
    numbered 1, 2, 4 sort into the right sequence and the walk runs; the missing
    3 is content that has not arrived yet, and the promotion to production is
    exactly when it must have.

    `stops[].storyId` is not checked here: `content_checks` already resolves it
    against the story registry.
    """
    errors: list[str] = []
    warnings: list[str] = []

    for walk in trip.get("walks", []):
        where = f"walk '{walk['id']}'"
        orders = [stop["order"] for stop in walk.get("stops", []) if stop.get("order") is not None]
        if not orders:
            continue

        counts = Counter(orders)
        for value in sorted(number for number, times in counts.items() if times > 1):
            errors.append(
                f"{where}: {counts[value]} stops both declare order {value}, so which one the "
                "traveller walks first depends on the sort being stable"
            )

        declared = ", ".join(str(number) for number in orders)
        lowest, highest = min(counts), max(counts)
        if lowest != 1:
            warnings.append(
                f"{where}: stop order starts at {lowest}, not 1 (stops are numbered {declared})"
            )
        missing = [number for number in range(lowest, highest) if number not in counts]
        if missing:
            warnings.append(
                f"{where}: stop order skips {', '.join(str(number) for number in missing)} "
                f"(stops are numbered {declared})"
            )

    return errors, warnings


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
    see `timezone_problems`).

    Warnings while `metadata.contentStatus` is prototype/draft, errors once the
    package declares `production` - which is the severity every finding here
    shares, and the reason `story_guide_title_problems` lives here rather than
    beside the coordinate check.
    """
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

    # Screen 20's menus. The fallback is a declared city, and a fallback that
    # points at a city carrying no menu is not a fallback: it is a day that
    # silently shows nothing, which is the failure the field exists to prevent
    # (D126). Checked here rather than in the schema, which cannot see across
    # the package.
    fallback_id = trip.get("fallbackMenuCityId")
    if fallback_id is not None:
        fallback_city = next(
            (c for c in trip.get("cities", []) if c.get("id") == fallback_id), None
        )
        if fallback_city is None:
            problems.append(f"fallbackMenuCityId: unknown city '{fallback_id}'")
        elif not (fallback_city.get("menu") or {}).get("meals"):
            problems.append(
                f"fallbackMenuCityId points at city '{fallback_id}', which carries no menu"
            )

    # A dish id has to be unique across the whole package, not merely inside
    # its own meal: it is the id a screen and a later photograph both name.
    seen_dishes, duplicate_dishes = set(), set()
    for city in trip.get("cities", []):
        menu = city.get("menu")
        if not menu:
            continue
        for meal in menu.get("meals", []):
            for dish in meal.get("dishes", []):
                dish_id = dish.get("id")
                where = f"city '{city['id']}' dish '{dish_id}'"
                ref("asset", assets, dish.get("photoAssetId"), where)
                if dish_id in seen_dishes:
                    duplicate_dishes.add(dish_id)
                seen_dishes.add(dish_id)
    for dupe in sorted(duplicate_dishes):
        problems.append(f"cities: duplicate dish id '{dupe}'")

    problems.extend(story_guide_title_problems(trip))

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

    # Same reasoning again: a point that contradicts its own city, and two stops
    # that claim the same position, are contradictions inside a package that is
    # already complete enough to hold both halves.
    coordinate_findings, coordinate_checked, coordinate_skipped = coordinate_problems(trip)
    if coordinate_findings:
        print(f"FAIL: {len(coordinate_findings)} coordinate error(s) in {args.trip}")
        for problem in coordinate_findings:
            print(f"- {problem}")
        return 1

    order_errors, order_warnings = walk_order_problems(trip)
    if order_errors:
        print(f"FAIL: {len(order_errors)} walk stop order error(s) in {args.trip}")
        for problem in order_errors:
            print(f"- {problem}")
        return 1

    problems = content_checks(trip, assets_root) + order_warnings

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
    if coordinate_checked or coordinate_skipped:
        print(
            f"Coordinates: {len(coordinate_checked)} city cluster(s) checked, "
            f"{len(coordinate_skipped)} point(s) with nothing to anchor them to"
        )
        for cluster in coordinate_checked:
            print(f"- checked {cluster}")
        for where, why in coordinate_skipped:
            print(f"- no coordinate check for {where}: {why}")
    if problems:
        print(f"WARNING: {len(problems)} content issue(s) (contentStatus is not 'production')")
        for problem in problems:
            print(f"- {problem}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
