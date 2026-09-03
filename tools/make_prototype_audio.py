#!/usr/bin/env python3
"""Generate the prototype audioguide placeholder.

Why this exists
---------------
The real Baščaršija audioguide has not been produced yet, but Phase 2 needs a
file that actually decodes so local playback, seeking and chapter changes can
be exercised on a device. This writes that placeholder.

Origin and licence
------------------
The output is synthesised from scratch by this script — a short sine marker at
the start of each chapter, silence in between. It contains no third-party
recording, no music and no speech, so it carries no upstream licence. Treat it
as CC0 / public domain, authored here.

It is NOT trip content. When the real audioguide exists, replace the file and
update `audio.bascarsija`'s path and mimeType plus `ag.bascarsija`'s chapter
titles and offsets to match the recording. `durationSeconds` stays at 720 — the
placeholder already runs the approved 12 minutes, so that value does not change.

Usage
-----
    python tools/make_prototype_audio.py
"""
from __future__ import annotations
import math
import struct
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "app/src/main/assets/trip/audio/attractions/bascarsija.prototype.wav"

# The approved prototype states a 12-minute audioguide, and screen 05 renders
# that duration from content. The placeholder therefore runs the full 720s so
# the approved copy stays true; 8-bit PCM at 8 kHz keeps the file as small as
# an uncompressed 12-minute WAV can reasonably be.
SAMPLE_RATE = 8_000
SAMPLE_WIDTH = 1             # 8-bit unsigned PCM
SEGMENT_SECONDS = 240        # 3 chapters x 4 min = 12 min
# One pitch per chapter, so a chapter change and a seek are audible.
SEGMENT_TONES_HZ = (392.0, 440.0, 494.0)
MARKER_SECONDS = 2.0         # tone at each chapter start, then silence
AMPLITUDE = 0.18             # deliberately quiet
FADE_SECONDS = 0.2           # avoids clicks at the marker edges
SILENCE = 128                # mid-scale for unsigned 8-bit


def render() -> bytes:
    """Each chapter opens with a short tone and is otherwise silent.

    A 12-minute continuous tone would be unusable to test with; a marker at
    every chapter boundary makes chapter changes and seeks audible without
    making the placeholder unbearable.
    """
    frames = bytearray()
    marker = int(MARKER_SECONDS * SAMPLE_RATE)
    fade = int(FADE_SECONDS * SAMPLE_RATE)

    for tone in SEGMENT_TONES_HZ:
        for index in range(marker):
            envelope = 1.0
            if index < fade:
                envelope = index / fade
            elif index > marker - fade:
                envelope = max(0.0, (marker - index) / fade)
            value = AMPLITUDE * envelope * math.sin(2 * math.pi * tone * index / SAMPLE_RATE)
            frames.append(SILENCE + int(value * 127))
        frames.extend([SILENCE] * (SEGMENT_SECONDS * SAMPLE_RATE - marker))

    return bytes(frames)


def main() -> int:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUTPUT), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(SAMPLE_WIDTH)
        out.setframerate(SAMPLE_RATE)
        out.writeframes(render())

    seconds = SEGMENT_SECONDS * len(SEGMENT_TONES_HZ)
    print(f"wrote {OUTPUT.relative_to(ROOT)} ({OUTPUT.stat().st_size / 1024:.0f} KB, {seconds}s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
