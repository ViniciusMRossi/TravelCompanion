package com.travelcompanion.app.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Group playback divergence, decided with no network and no Firebase.
 *
 * This is the part of Phase 4 that carries real runtime risk: what one phone
 * does when it disagrees with the other, and what it does when it cannot hear
 * the other at all. The second one is the product invariant (brief §3.3), so
 * it is asserted rather than assumed.
 */
class GroupPlaybackTest {

    private val guide = "ag.bascarsija"

    private fun group(
        positionMs: Long = 0L,
        isPlaying: Boolean = true,
        anchorServerMs: Long = 1_000L,
        mediaId: String = guide,
    ) = GroupPlayback(
        mediaId = mediaId,
        positionMs = positionMs,
        isPlaying = isPlaying,
        anchorServerMs = anchorServerMs,
        updatedBy = "vinicius",
    )

    private fun local(
        positionMs: Long = 0L,
        isPlaying: Boolean = true,
        mediaId: String? = guide,
    ) = LocalPlayback(mediaId = mediaId, positionMs = positionMs, isPlaying = isPlaying)

    @Test
    fun `a playing group moves on with elapsed server time`() {
        val g = group(positionMs = 10_000L, anchorServerMs = 1_000L)
        assertEquals(10_000L, expectedPosition(g, serverNowMs = 1_000L))
        assertEquals(15_000L, expectedPosition(g, serverNowMs = 6_000L))
    }

    @Test
    fun `a paused group stays where it is however long passes`() {
        val g = group(positionMs = 42_000L, isPlaying = false, anchorServerMs = 1_000L)
        assertEquals(42_000L, expectedPosition(g, serverNowMs = 999_000L))
    }

    @Test
    fun `a start agreed for the future has not begun yet`() {
        // The 3-2-1 window on screen 08: the anchor is server time plus a short
        // delay, and until it arrives the guide is still at its start.
        val g = group(positionMs = 0L, anchorServerMs = 10_000L)
        assertEquals(0L, expectedPosition(g, serverNowMs = 7_000L))
        assertEquals(0L, expectedPosition(g, serverNowMs = 10_000L))
        assertEquals(2_000L, expectedPosition(g, serverNowMs = 12_000L))
    }

    @Test
    fun `no group state leaves local playback alone`() {
        // The invariant: unreachable group must never stop or move local audio.
        assertEquals(
            SyncCorrection.None,
            syncCorrection(local(positionMs = 30_000L), group = null, serverNowMs = 99_000L),
        )
    }

    @Test
    fun `small drift is not worth correcting`() {
        val g = group(positionMs = 0L, anchorServerMs = 0L)
        // Group expects 30_000; this phone is 1.2s behind.
        val decision = syncCorrection(local(positionMs = 28_800L), g, serverNowMs = 30_000L)
        assertEquals(SyncCorrection.None, decision)
    }

    @Test
    fun `drift past tolerance seeks to where the group is`() {
        val g = group(positionMs = 0L, anchorServerMs = 0L)
        val decision = syncCorrection(local(positionMs = 20_000L), g, serverNowMs = 30_000L)
        assertEquals(SyncCorrection.Seek(30_000L), decision)
    }

    @Test
    fun `a different guide is loaded at the group's position`() {
        val g = group(positionMs = 5_000L, anchorServerMs = 0L, mediaId = "ag.latin-bridge")
        val decision = syncCorrection(local(mediaId = guide), g, serverNowMs = 1_000L)
        assertEquals(SyncCorrection.Load("ag.latin-bridge", 6_000L), decision)
    }

    @Test
    fun `nothing loaded locally loads what the group is playing`() {
        val g = group(positionMs = 0L, anchorServerMs = 0L)
        val decision = syncCorrection(local(mediaId = null), g, serverNowMs = 4_000L)
        assertEquals(SyncCorrection.Load(guide, 4_000L), decision)
    }

    @Test
    fun `the group pausing pauses this phone`() {
        val g = group(positionMs = 30_000L, isPlaying = false)
        assertEquals(SyncCorrection.Pause, syncCorrection(local(isPlaying = true), g, 50_000L))
    }

    @Test
    fun `a paused group and a paused phone need nothing`() {
        val g = group(positionMs = 30_000L, isPlaying = false)
        assertEquals(
            SyncCorrection.None,
            syncCorrection(local(positionMs = 30_000L, isPlaying = false), g, 50_000L),
        )
    }

    @Test
    fun `the group playing resumes this phone at the group's position`() {
        val g = group(positionMs = 0L, anchorServerMs = 0L)
        val decision = syncCorrection(local(positionMs = 0L, isPlaying = false), g, 8_000L)
        assertEquals(SyncCorrection.Resume(8_000L), decision)
    }

    @Test
    fun `server clock offset is what the start is agreed in, not the device clock`() {
        // Device is 90 seconds behind the server.
        val clock = ServerClock.fromObservation(serverMs = 1_000_090_000L, deviceMs = 1_000_000_000L)
        assertEquals(90_000L, clock.offsetMs)
        assertEquals(1_000_090_500L, clock.serverNow(1_000_000_500L))
        assertEquals(1_000_000_500L, clock.deviceTimeFor(1_000_090_500L))
    }

    @Test
    fun `two phones with different wall clocks agree on the same position`() {
        // Same trip, same guide, same server anchor — but the phones' own
        // clocks disagree by two minutes. Both must land on the same place.
        val anchorServer = 5_000_000L
        val g = group(positionMs = 0L, anchorServerMs = anchorServer)

        val phoneA = ServerClock.fromObservation(serverMs = anchorServer, deviceMs = 0L)
        val phoneB = ServerClock.fromObservation(serverMs = anchorServer, deviceMs = 120_000L)

        val tenSecondsLaterOnA = phoneA.serverNow(10_000L)
        val tenSecondsLaterOnB = phoneB.serverNow(130_000L)

        assertEquals(tenSecondsLaterOnA, tenSecondsLaterOnB)
        assertEquals(
            expectedPosition(g, tenSecondsLaterOnA),
            expectedPosition(g, tenSecondsLaterOnB),
        )
    }
}
