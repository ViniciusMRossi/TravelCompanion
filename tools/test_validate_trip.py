#!/usr/bin/env python3
"""Practical fixtures for the two checks that always fail, at any stage.

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
    audio_duration_problems,
    audio_duration_seconds,
    known_timezones,
    timezone_problems,
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
