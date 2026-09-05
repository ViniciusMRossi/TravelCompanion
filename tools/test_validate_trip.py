#!/usr/bin/env python3
"""Practical fixtures for the checks that no schema can express.

The IANA time zone check: a real zone name is accepted, a typo'd one is
rejected. This is the class of defect D026 exists to catch, so it needs a test
that actually exercises the tz database rather than trusting the schema's
pattern alone.

The audio duration check: the packaged prototype WAV is a positive case that
lives in this repository, so the test that guards the real `.m4a` guides is
proved against a file anyone can open. The MP4 side is exercised against
`mvhd` atoms built byte by byte here, both versions, because no `.m4a` is
committed and one measured by hand would only prove that this parser agrees
with itself.

The coordinate check: the four packaged Sarajevo points are the positive case,
and the fixtures use their real values rather than synthetic ones near (0, 0),
because what is being proved is a distance threshold and not arithmetic. The
coincident pair has a test of its own - it is correct content in both packaged
trips, and it is what somebody will "fix" on the day zero metres looks
suspicious.

The walk ordering check: `stops[].order` is sorted by `WalkModeState.kt`, so
1, 3, 2 is not a defect and has a test saying so; what is checked is a
numbering that cannot produce an order at all.

Run with: python -m unittest tools/test_validate_trip.py
"""
from __future__ import annotations

import json
import struct
import sys
import tempfile
import unittest
import wave
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from validate_trip import (
    AUDIO_DURATION_TOLERANCE_SECONDS,
    COORDINATE_CLUSTER_LIMIT_METERS,
    audio_duration_problems,
    audio_duration_seconds,
    content_checks,
    coordinate_problems,
    distance_meters,
    known_timezones,
    story_guide_title_problems,
    timezone_problems,
    walk_order_problems,
)

REPO = Path(__file__).resolve().parent.parent
PACKAGED_TRIP = REPO / "app/src/main/assets/trip/trip.json"


def _trip(city_zone: str, day_zone: str, override_zone: str | None = None) -> dict:
    timeline_item = {"id": "d1.morning", "startTime": "09:00", "kind": "custom", "title": "x"}
    if override_zone is not None:
        timeline_item["timeZone"] = override_zone
    return {
        "cities": [{"id": "c", "timeZone": city_zone}],
        "days": [{"id": "d1", "timeZone": day_zone, "timeline": [timeline_item]}],
        "transports": [
            {
                "id": "t1",
                "origin": {"timeZone": city_zone},
                "destination": {"timeZone": day_zone},
            }
        ],
    }


class TimezoneProblemsTest(unittest.TestCase):
    def setUp(self):
        self.timezones = known_timezones()
        if not self.timezones:
            self.skipTest("tz database unavailable in this environment (install tzdata)")

    def test_valid_iana_zones_are_accepted(self):
        trip = _trip("Europe/Sarajevo", "Europe/Sarajevo", override_zone="Europe/Belgrade")
        self.assertEqual([], timezone_problems(trip, self.timezones))

    def test_invalid_iana_zone_is_rejected(self):
        trip = _trip("Europe/Sarayevo", "Europe/Sarajevo")
        problems = timezone_problems(trip, self.timezones)
        self.assertTrue(
            any("Europe/Sarayevo" in p for p in problems),
            f"expected the misspelled zone to be flagged, got: {problems}",
        )

    def test_fixed_offset_zones_are_rejected(self):
        # These resolve in the tz database, so the "unknown name" check lets
        # them through, but they carry no DST rule — the very thing schema 1.1
        # exists to prevent (CONTENT-GENERATOR.md §8).
        for zone in ("Etc/GMT+2", "UTC", "Etc/UTC", "GMT", "Zulu"):
            with self.subTest(zone=zone):
                self.assertIn(zone, self.timezones, "fixture assumes a real tz name")
                trip = _trip(zone, "Europe/Sarajevo")
                problems = timezone_problems(trip, self.timezones)
                self.assertTrue(
                    any("fixed UTC offset" in p for p in problems),
                    f"expected {zone} to be rejected as a fixed offset, got: {problems}",
                )

    def test_override_equal_to_day_zone_is_not_flagged(self):
        # Redundant, but valid per schema/package: the override just repeats
        # the zone it already inherits from. Not a validator error (D026).
        trip = _trip("Europe/Sarajevo", "Europe/Sarajevo", override_zone="Europe/Sarajevo")
        self.assertEqual([], timezone_problems(trip, self.timezones))


def _mvhd(timescale: int, duration: int, version: int = 0) -> bytes:
    """A movie header atom, as an encoder writes it.

    Version 0 keeps the times in 32 bits and version 1 in 64; a file longer
    than about 13 hours at a 90 kHz timescale needs the second, and TTS output
    can be written either way, so both are read and both are tested.
    """
    if version == 0:
        body = struct.pack(">B3x", 0) + struct.pack(">IIII", 0, 0, timescale, duration)
    else:
        body = struct.pack(">B3x", 1) + struct.pack(">QQIQ", 0, 0, timescale, duration)
    body += b"\x00" * 80  # rate, volume, matrix and the rest, unread here
    return struct.pack(">I", len(body) + 8) + b"mvhd" + body


def _atom(name: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload) + 8) + name + payload


def _write_m4a(path: Path, seconds: float, version: int = 0, timescale: int = 44100) -> None:
    path.write_bytes(
        _atom(b"ftyp", b"M4A isomM4A ")
        + _atom(b"moov", _mvhd(timescale, round(seconds * timescale), version))
    )


def _write_wav(path: Path, seconds: float, rate: int = 8000) -> None:
    with wave.open(str(path), "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(1)
        handle.setframerate(rate)
        handle.writeframes(b"\x80" * int(seconds * rate))


def _guide_trip(duration_seconds, chapters, path="audio/guide.m4a"):
    return {
        "assets": [{"id": "audio.g", "type": "audio", "path": path}],
        "audioGuides": [
            {
                "id": "ag.g",
                "title": "g",
                "audioAssetId": "audio.g",
                "durationSeconds": duration_seconds,
                "chapters": [{"title": f"c{i}", "startSeconds": s} for i, s in enumerate(chapters)],
            }
        ],
    }


class AudioDurationReaderTest(unittest.TestCase):
    """Can this script tell how long a file is, without ffmpeg?"""

    def test_the_packaged_prototype_wav_reads_its_real_length(self):
        # The one audio file this repository carries, and the case the check
        # has to pass: `ag.bascarsija` declares 720 and the file is 720.0s.
        wav = REPO / "app/src/main/assets/trip/audio/attractions/bascarsija.prototype.wav"
        self.assertTrue(wav.is_file(), f"fixture missing: {wav}")
        self.assertAlmostEqual(720.0, audio_duration_seconds(wav), places=3)

    def test_mp4_duration_is_read_from_mvhd_in_both_versions(self):
        with tempfile.TemporaryDirectory() as tmp:
            for version in (0, 1):
                with self.subTest(mvhd_version=version):
                    path = Path(tmp) / f"guide{version}.m4a"
                    _write_m4a(path, 754.5, version=version)
                    self.assertAlmostEqual(754.5, audio_duration_seconds(path), places=2)

    def test_a_format_this_script_cannot_read_returns_no_duration(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "guide.mp3"
            path.write_bytes(b"ID3\x03\x00\x00\x00\x00\x00\x00")
            self.assertIsNone(audio_duration_seconds(path))


class AudioDurationProblemsTest(unittest.TestCase):
    """Does the declared number have to agree with the file?"""

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.root = Path(self._tmp.name)
        (self.root / "audio").mkdir()
        self.addCleanup(self._tmp.cleanup)

    def _problems(self, trip):
        problems, _, _ = audio_duration_problems(trip, self.root)
        return problems

    def test_the_packaged_trip_has_no_duration_problem(self):
        # The positive case, on the real file rather than a fixture: whatever
        # else this check does, it must not start rejecting what ships.
        trip = json.loads(PACKAGED_TRIP.read_text(encoding="utf-8"))
        problems, checked, _ = audio_duration_problems(trip, PACKAGED_TRIP.parent)
        self.assertEqual([], problems)
        self.assertIn("ag.bascarsija", checked, "the packaged WAV must actually be timed")

    def test_a_declared_duration_that_the_file_contradicts_is_reported(self):
        _write_m4a(self.root / "audio/guide.m4a", 720.0)
        problems = self._problems(_guide_trip(690, [0, 240]))
        self.assertTrue(
            any("declares durationSeconds 690" in p and "720.0s" in p for p in problems),
            f"expected the 30s gap to be flagged, got: {problems}",
        )

    def test_a_rounding_difference_is_not_reported(self):
        # 719.6s is honestly written as 720; an encoder that pads the last
        # frame moves the end by less again. Neither is a defect.
        _write_m4a(self.root / "audio/guide.m4a", 719.6)
        self.assertEqual([], self._problems(_guide_trip(720, [0, 240])))
        self.assertGreaterEqual(AUDIO_DURATION_TOLERANCE_SECONDS, 1.0)

    def test_a_chapter_at_or_past_the_end_of_the_file_is_reported(self):
        _write_m4a(self.root / "audio/guide.m4a", 600.0)
        for start in (600, 900):
            with self.subTest(startSeconds=start):
                problems = self._problems(_guide_trip(600, [0, start]))
                self.assertTrue(
                    any("at or past the end of the file" in p for p in problems),
                    f"expected a chapter at {start}s to be flagged, got: {problems}",
                )

    def test_chapters_out_of_order_are_reported_with_no_file_at_all(self):
        # Nothing is packaged here: the contradiction is inside the JSON, and
        # the sheet is drawn in list order regardless of what the audio does.
        problems = self._problems(_guide_trip(600, [0, 300, 120]))
        self.assertTrue(
            any("at or before the chapter before it" in p for p in problems),
            f"expected the backwards chapter to be flagged, got: {problems}",
        )

    def test_chapters_are_still_checked_against_the_declared_length(self):
        problems = self._problems(_guide_trip(600, [0, 700]))
        self.assertTrue(
            any("at or past the end of the declared duration" in p for p in problems),
            f"expected the chapter past the declared end to be flagged, got: {problems}",
        )

    def test_an_unreadable_format_is_an_absent_check_and_not_a_failure(self):
        (self.root / "audio/guide.mp3").write_bytes(b"ID3\x03\x00\x00\x00\x00\x00\x00")
        trip = _guide_trip(9999, [0], path="audio/guide.mp3")
        problems, checked, skipped = audio_duration_problems(trip, self.root)
        self.assertEqual([], problems)
        self.assertEqual([], checked)
        self.assertEqual(1, len(skipped))
        self.assertIn("no duration reader for '.mp3'", skipped[0][1])

    def test_a_file_that_is_not_packaged_yet_is_named_rather_than_rejected(self):
        trip = _guide_trip(720, [0, 240])
        problems, checked, skipped = audio_duration_problems(trip, self.root)
        self.assertEqual([], problems)
        self.assertEqual([], checked)
        self.assertIn("file not packaged", skipped[0][1])


if __name__ == "__main__":
    unittest.main()


# Baščaršija, the Latin Bridge and the point between them, as the packaged trips
# declare them. Real coordinates on purpose: a fixture invented near (0, 0) would
# prove the arithmetic and nothing about the distances this check was sized for.
BASCARSIJA = (43.8595, 18.4310)
LATIN_BRIDGE = (43.8578, 18.4289)
MEETING_OF_CULTURES = (43.8590, 18.4257)


def _geo_trip(*stories, attractions=(), walks=(), transports=()):
    """A trip carrying only what the coordinate check reads."""
    return {
        "cities": [{"id": "sarajevo"}],
        "attractions": [
            {"id": name, "cityId": city, "location": {"geo": {"latitude": lat, "longitude": lon}}}
            for name, city, (lat, lon) in attractions
        ],
        "walks": [
            {
                "id": name,
                "cityId": city,
                "startLocation": {"geo": {"latitude": lat, "longitude": lon}},
            }
            for name, city, (lat, lon) in walks
        ],
        "stories": [
            {
                "id": name,
                "cityId": city,
                "trigger": {"geo": {"latitude": lat, "longitude": lon}, "radiusMeters": 60},
            }
            for name, city, (lat, lon) in stories
        ],
        "transports": [
            {
                "id": name,
                "origin": {"location": {"geo": {"latitude": lat, "longitude": lon}}},
                "destination": {"name": "somewhere"},
            }
            for name, (lat, lon) in transports
        ],
    }


class CoordinateProblemsTest(unittest.TestCase):
    """The three errors that stay inside the schema's own latitude/longitude range."""

    def test_the_packaged_trip_has_no_coordinate_problem(self):
        trip = json.loads(PACKAGED_TRIP.read_text(encoding="utf-8"))
        problems, checked, skipped = coordinate_problems(trip)
        self.assertEqual(problems, [])
        # Four coordinates, one city, and nothing left unanchored.
        self.assertEqual(checked, ["city 'sarajevo' (4 coordinates)"])
        self.assertEqual(skipped, [])

    def test_swapped_latitude_and_longitude_are_reported_and_named(self):
        """The sample's own shape: four coordinates in the city, one transposed."""
        swapped = (LATIN_BRIDGE[1], LATIN_BRIDGE[0])
        trip = _geo_trip(
            ("story.latin-bridge", "sarajevo", swapped),
            ("story.meeting-of-cultures", "sarajevo", MEETING_OF_CULTURES),
            attractions=[
                ("bascarsija", "sarajevo", BASCARSIJA),
                ("latin-bridge", "sarajevo", LATIN_BRIDGE),
            ],
        )
        problems, _, _ = coordinate_problems(trip)
        self.assertEqual(len(problems), 1)
        self.assertIn("story 'story.latin-bridge': trigger.geo (18.4289, 43.8578)", problems[0])
        self.assertIn("km from the nearest other point in city 'sarajevo'", problems[0])
        self.assertIn("attraction 'latin-bridge' at 43.8578, 18.4289", problems[0])
        # The diagnosis, not just the accusation.
        self.assertIn("the two values look transposed", problems[0])

    def test_a_flipped_sign_is_reported_without_claiming_a_transposition(self):
        trip = _geo_trip(
            ("story.latin-bridge", "sarajevo", (-LATIN_BRIDGE[0], LATIN_BRIDGE[1])),
            ("story.meeting-of-cultures", "sarajevo", MEETING_OF_CULTURES),
            attractions=[("bascarsija", "sarajevo", BASCARSIJA)],
        )
        problems, _, _ = coordinate_problems(trip)
        self.assertEqual(len(problems), 1)
        self.assertIn("story 'story.latin-bridge': trigger.geo (-43.8578, 18.4289)", problems[0])
        self.assertNotIn("transposed", problems[0])

    def test_two_coordinates_and_no_cluster_accuses_neither_of_them(self):
        """Butmir's shape, and the one the swap test cannot resolve.

        Transposing either half of a transposed pair lands on the other half, so
        with only two coordinates in the city there is nothing that says which
        one was typed wrong. The finding names both rather than being right half
        the time.
        """
        trip = _geo_trip(
            ("story.latin-bridge", "sarajevo", (LATIN_BRIDGE[1], LATIN_BRIDGE[0])),
            attractions=[("bascarsija", "sarajevo", BASCARSIJA)],
        )
        problems, _, _ = coordinate_problems(trip)
        self.assertEqual(len(problems), 1)
        self.assertIn("attraction 'bascarsija' location.geo (43.8595, 18.431)", problems[0])
        self.assertIn("story 'story.latin-bridge' trigger.geo (18.4289, 43.8578)", problems[0])
        self.assertIn("neither has a near neighbour in the city to anchor it", problems[0])
        self.assertIn("one of the two has its values transposed", problems[0])

    def test_coincident_points_are_correct_content(self):
        """The regression somebody will cause the day zero metres looks suspicious.

        Both packaged trips contain a pair: the real walk starts at its first
        stop, so `startLocation` repeats that stop's trigger, and in the sample
        the Latin Bridge story sits on the Latin Bridge attraction.
        """
        trip = _geo_trip(
            ("story.sebilj", "sarajevo", BASCARSIJA),
            walks=[("walk.bazar-ao-rio", "sarajevo", BASCARSIJA)],
        )
        problems, checked, skipped = coordinate_problems(trip)
        self.assertEqual(problems, [])
        self.assertEqual(checked, ["city 'sarajevo' (2 coordinates)"])
        self.assertEqual(skipped, [])

    def test_a_city_with_one_coordinate_is_named_rather_than_checked(self):
        trip = _geo_trip(("story.tunnel", "butmir", BASCARSIJA))
        problems, checked, skipped = coordinate_problems(trip)
        self.assertEqual(problems, [])
        self.assertEqual(checked, [])
        self.assertEqual(len(skipped), 1)
        self.assertEqual(skipped[0][0], "city 'butmir'")
        self.assertIn("only one packaged coordinate", skipped[0][1])

    def test_a_transport_endpoint_is_named_rather_than_anchored(self):
        trip = _geo_trip(
            ("story.latin-bridge", "sarajevo", LATIN_BRIDGE),
            attractions=[("bascarsija", "sarajevo", BASCARSIJA)],
            transports=[("transport.sarajevo-mostar.bus", BASCARSIJA)],
        )
        problems, _, skipped = coordinate_problems(trip)
        self.assertEqual(problems, [])
        self.assertEqual(len(skipped), 1)
        self.assertEqual(skipped[0][0], "transport 'transport.sarajevo-mostar.bus' origin")
        self.assertIn("declares no city", skipped[0][1])

    def test_a_transposed_decimal_is_out_of_reach_and_that_is_recorded(self):
        """43.8578 typed as 43.5878 moves the point ~30 km and passes.

        Asserted rather than left implicit, so the limit of the check is a fact
        in the suite and not a surprise on the road: the threshold buys immunity
        to a large sparse city, and this is what it costs.
        """
        trip = _geo_trip(
            ("story.latin-bridge", "sarajevo", (43.5878, LATIN_BRIDGE[1])),
            attractions=[("bascarsija", "sarajevo", BASCARSIJA)],
        )
        problems, _, _ = coordinate_problems(trip)
        self.assertEqual(problems, [])
        gap = distance_meters((43.5878, LATIN_BRIDGE[1]), BASCARSIJA)
        self.assertLess(gap, COORDINATE_CLUSTER_LIMIT_METERS)
        self.assertGreater(gap, 25_000.0)


def _walk_trip(*orders):
    return {
        "walks": [
            {
                "id": "walk.sarajevo.historical",
                "cityId": "sarajevo",
                "stops": [{"storyId": f"story.{n}", "order": n} for n in orders],
            }
        ]
    }


class WalkOrderProblemsTest(unittest.TestCase):
    """`stops[].order` is sorted by `WalkModeState.kt` and checked by nobody else."""

    def test_the_packaged_walk_is_numbered_1_2(self):
        trip = json.loads(PACKAGED_TRIP.read_text(encoding="utf-8"))
        self.assertEqual(walk_order_problems(trip), ([], []))

    def test_a_duplicate_order_is_an_error_at_every_stage(self):
        errors, warnings = walk_order_problems(_walk_trip(1, 2, 2))
        self.assertEqual(warnings, [])
        self.assertEqual(len(errors), 1)
        self.assertIn("2 stops both declare order 2", errors[0])
        self.assertIn("depends on the sort being stable", errors[0])

    def test_a_gap_is_a_warning_because_the_missing_stop_may_still_arrive(self):
        errors, warnings = walk_order_problems(_walk_trip(1, 2, 4))
        self.assertEqual(errors, [])
        self.assertEqual(len(warnings), 1)
        self.assertIn("stop order skips 3", warnings[0])

    def test_a_first_stop_that_is_not_1_is_a_warning(self):
        errors, warnings = walk_order_problems(_walk_trip(2, 3))
        self.assertEqual(errors, [])
        self.assertEqual(len(warnings), 1)
        self.assertIn("starts at 2, not 1", warnings[0])

    def test_stops_numbered_out_of_sequence_but_complete_are_not_a_finding(self):
        """1, 3, 2 is the case the brief named, and `sortedBy` handles it.

        The numbering is complete and unambiguous; the walk runs in the order
        1, 2, 3. Nothing is wrong, so nothing is reported.
        """
        self.assertEqual(walk_order_problems(_walk_trip(1, 3, 2)), ([], []))


class CoordinateWordingTest(unittest.TestCase):
    """The sentence has to describe the condition the code actually tested."""

    def test_three_points_all_far_apart_do_not_claim_there_is_no_third(self):
        """The wording that D098 wrote in prose and the code over-generalised.

        The branch is reached whenever neither member of a pair has a near
        neighbour, which three mutually distant points satisfy just as two do.
        The old sentence said "no third coordinate in the city" and this
        package has three, so it was printed twice and was false twice.
        """
        trip = _geo_trip(
            ("story.sarajevo", "x", (43.85, 18.43)),
            ("story.paris", "x", (48.85, 2.35)),
            ("story.saopaulo", "x", (-23.55, -46.63)),
        )
        problems, checked, _ = coordinate_problems(trip)
        self.assertEqual(checked, ["city 'x' (3 coordinates)"])
        self.assertEqual(len(problems), 2)
        for problem in problems:
            self.assertNotIn("no third coordinate", problem)
            self.assertIn("neither has a near neighbour in the city to anchor it", problem)

    def test_the_diameter_does_not_add_a_third_finding_when_the_neighbour_spoke(self):
        """One defect, one finding: the diameter only runs where the other is blind."""
        trip = _geo_trip(
            ("story.latin-bridge", "sarajevo", (LATIN_BRIDGE[1], LATIN_BRIDGE[0])),
            ("story.meeting-of-cultures", "sarajevo", MEETING_OF_CULTURES),
            attractions=[
                ("bascarsija", "sarajevo", BASCARSIJA),
                ("latin-bridge", "sarajevo", LATIN_BRIDGE),
            ],
        )
        problems, _, _ = coordinate_problems(trip)
        self.assertEqual(len(problems), 1)
        self.assertNotIn("its coordinates span", problems[0])


class CoordinateDiameterTest(unittest.TestCase):
    """A group transposed in one go, which the nearest neighbour cannot see."""

    def test_two_of_four_transposed_together_are_caught_by_the_span(self):
        """The case that passed in silence: every point has a close neighbour.

        Transposing two of the four packaged Sarajevo points leaves two tight
        groups 3691 km apart. Each wrong point is 0.3 km from the other wrong
        point, each right point is 0.3 km from the other right one, so nothing
        is far from anything and the nearest neighbour reports nothing at all.
        """
        trip = _geo_trip(
            ("story.meeting-of-cultures", "sarajevo",
             (MEETING_OF_CULTURES[1], MEETING_OF_CULTURES[0])),
            ("story.latin-bridge", "sarajevo", (LATIN_BRIDGE[1], LATIN_BRIDGE[0])),
            attractions=[("bascarsija", "sarajevo", BASCARSIJA)],
            walks=[("walk.bazar-ao-rio", "sarajevo", BASCARSIJA)],
        )
        problems, checked, _ = coordinate_problems(trip)
        self.assertEqual(checked, ["city 'sarajevo' (4 coordinates)"])
        self.assertEqual(len(problems), 1)
        self.assertIn("city 'sarajevo': its coordinates span 3691.", problems[0])
        self.assertIn("Every point here has a close neighbour", problems[0])
        self.assertIn("more than one group of coordinates", problems[0])
        self.assertIn("one group has its values transposed", problems[0])

    def test_all_four_transposed_together_are_caught_too(self):
        """Nothing is left to compare against, and the span is still 0."""
        trip = _geo_trip(
            ("story.meeting-of-cultures", "sarajevo",
             (MEETING_OF_CULTURES[1], MEETING_OF_CULTURES[0])),
            ("story.latin-bridge", "sarajevo", (LATIN_BRIDGE[1], LATIN_BRIDGE[0])),
            attractions=[("bascarsija", "sarajevo", (BASCARSIJA[1], BASCARSIJA[0]))],
        )
        problems, _, _ = coordinate_problems(trip)
        # A city moved wholesale is internally consistent: it is one place, and
        # the wrong one. Nothing in the package can say so, and this asserts
        # that limit rather than leaving it to be found on the road.
        self.assertEqual(problems, [])

    def test_the_span_leaves_a_coincident_pair_alone(self):
        """Zero metres is the strongest agreement there is, and spans nothing."""
        trip = _geo_trip(
            ("story.sebilj", "sarajevo", BASCARSIJA),
            walks=[("walk.bazar-ao-rio", "sarajevo", BASCARSIJA)],
        )
        problems, checked, _ = coordinate_problems(trip)
        self.assertEqual(problems, [])
        self.assertEqual(checked, ["city 'sarajevo' (2 coordinates)"])

    def test_the_packaged_trip_span_is_far_under_the_limit(self):
        trip = json.loads(PACKAGED_TRIP.read_text(encoding="utf-8"))
        problems, _, _ = coordinate_problems(trip)
        self.assertEqual(problems, [])


def _title_trip(story_title, guide_title, link=True):
    return {
        "assets": [{"id": "a", "type": "audio", "path": "audio/x.m4a"}],
        "audioGuides": [
            {"id": "ag.x", "title": guide_title, "audioAssetId": "a", "durationSeconds": 60}
        ],
        "stories": [
            {
                "id": "story.x",
                "cityId": "sarajevo",
                "title": story_title,
                "hook": "h",
                "body": "b",
                **({"audioGuideId": "ag.x"} if link else {}),
            }
        ],
    }


class StoryGuideTitleTest(unittest.TestCase):
    """The player pairs the two titles; equal ones print the same line twice."""

    def test_identical_titles_are_reported(self):
        problems = story_guide_title_problems(_title_trip("Ponte Latina", "Ponte Latina"))
        self.assertEqual(len(problems), 1)
        self.assertIn("story 'story.x' and audioGuide 'ag.x' carry the same title", problems[0])
        self.assertIn("'Ponte Latina'", problems[0])
        self.assertIn("the lock screen prints the line twice", problems[0])

    def test_the_guide_naming_the_place_and_the_story_the_sentence_is_silent(self):
        problems = story_guide_title_problems(
            _title_trip("A Ponte Latina, e a esquina que nao e a ponte", "Ponte Latina")
        )
        self.assertEqual(problems, [])

    def test_a_guide_no_story_points_at_has_nothing_to_collide_with(self):
        problems = story_guide_title_problems(
            _title_trip("Ponte Latina", "Ponte Latina", link=False)
        )
        self.assertEqual(problems, [])

    def test_a_title_retyped_rather_than_copied_is_the_same_duplicated_line(self):
        problems = story_guide_title_problems(_title_trip("Ponte Latina", "ponte latina "))
        self.assertEqual(len(problems), 1)

    def test_it_reaches_the_package_through_content_checks(self):
        """Which is what makes it a warning until production, and an error in it."""
        trip = _title_trip("Ponte Latina", "Ponte Latina")
        with tempfile.TemporaryDirectory() as folder:
            problems = content_checks(trip, Path(folder))
        self.assertEqual(len(problems), 1)
        self.assertIn("carry the same title", problems[0])
