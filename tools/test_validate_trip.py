#!/usr/bin/env python3
"""Practical fixture for the IANA time zone check: a real zone name is
accepted, a typo'd one is rejected. This is the class of defect D026 exists
to catch, so it needs a test that actually exercises the tz database rather
than trusting the schema's pattern alone.

Run with: python -m unittest tools/test_validate_trip.py
"""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from validate_trip import known_timezones, timezone_problems


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


if __name__ == "__main__":
    unittest.main()
