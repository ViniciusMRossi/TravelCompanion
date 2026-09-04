package com.travelcompanion.app.service.sync

import com.travelcompanion.app.data.sync.GroupParticipant
import com.travelcompanion.app.data.sync.GroupSyncRepository
import com.travelcompanion.app.data.sync.GroupSyncState
import com.travelcompanion.app.data.sync.ParticipantSync
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.domain.sync.GroupPlayback
import com.travelcompanion.app.domain.sync.ServerClock
import com.travelcompanion.app.service.playback.FakeAudioEngine
import com.travelcompanion.app.service.playback.InMemoryPlaybackPositionStore
import com.travelcompanion.app.service.playback.PlaybackController
import com.travelcompanion.app.service.playback.audioGuideRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The group driving the local player, and — more importantly — the group
 * failing to.
 *
 * Brief §3.3 says synchronization must never block local behaviour. That is
 * the product's invariant, so it is asserted here against a real
 * [PlaybackController] with a fake engine, using a repository that can be told
 * to go away. No Firebase, no emulator, no network.
 */
class GroupSessionControllerTest {

    private val content = packagedContent(exists = { true })
    private val guide = "ag.bascarsija"

    private val engine = FakeAudioEngine()
    private val playback = PlaybackController(
        engine = engine,
        positions = InMemoryPlaybackPositionStore(),
        scope = CoroutineScope(Dispatchers.Unconfined),
    )
    private val sync = FakeGroupSync()

    private var nowMs = 100_000L

    private val controller = GroupSessionController(
        sync = sync,
        playback = playback,
        scope = CoroutineScope(Dispatchers.Unconfined),
        participantId = { "vinicius" },
        now = { nowMs },
    )

    private fun playLocally() {
        playback.playAudioGuide(audioGuideRequest(content, guide)!!)
        engine.becomeReady(durationMs = 720_000L)
    }

    private fun group(
        positionMs: Long,
        isPlaying: Boolean = true,
        anchorServerMs: Long = 0L,
        mediaId: String = guide,
    ) = GroupPlayback(mediaId, positionMs, isPlaying, anchorServerMs, "erika")

    // -- the invariant ----------------------------------------------------

    @Test
    fun `local audio keeps playing when the group cannot be reached`() {
        playLocally()
        controller.join()
        sync.emit(GroupSyncState(GroupSyncState.Status.Synchronized))

        val positionBefore = playback.state.value.positionMs
        val playingBefore = playback.state.value.isPlaying
        assertTrue("fixture should be playing", playingBefore)

        // The group drops in every way it can.
        sync.emit(GroupSyncState(GroupSyncState.Status.Reconnecting))
        sync.emit(GroupSyncState(GroupSyncState.Status.Offline))
        sync.emit(GroupSyncState(GroupSyncState.Status.Disabled))

        assertEquals(playingBefore, playback.state.value.isPlaying)
        assertEquals(positionBefore, playback.state.value.positionMs)
        assertEquals(guide, playback.state.value.mediaId)
    }

    @Test
    fun `a group that is not live cannot pause this phone`() {
        playLocally()
        controller.join()

        // Reconnecting, and the last thing the group said was "paused".
        sync.emit(
            GroupSyncState(
                status = GroupSyncState.Status.Reconnecting,
                playback = group(positionMs = 0L, isPlaying = false),
            ),
        )

        assertTrue(playback.state.value.isPlaying)
    }

    @Test
    fun `leaving the group does not stop the audio`() {
        playLocally()
        controller.join()
        sync.emit(GroupSyncState(GroupSyncState.Status.Synchronized))

        controller.leave()

        assertTrue(playback.state.value.isPlaying)
        assertEquals(guide, playback.state.value.mediaId)
    }

    // -- following the group ---------------------------------------------

    @Test
    fun `drift past tolerance is corrected towards the group`() {
        playLocally()
        engine.advanceTo(10_000L)
        playback.refreshProgress()
        controller.join()

        // Group anchored at server 0 playing from 0; server now is 30s in.
        sync.emit(
            GroupSyncState(
                status = GroupSyncState.Status.Synchronized,
                playback = group(positionMs = 0L, anchorServerMs = 0L),
                clock = ServerClock.fromObservation(serverMs = 30_000L, deviceMs = nowMs),
            ),
        )

        assertTrue("expected a seek towards the group", engine.commands.contains("seekTo(30000)"))
    }

    @Test
    fun `the group pausing pauses this phone`() {
        playLocally()
        controller.join()
        sync.emit(
            GroupSyncState(
                status = GroupSyncState.Status.Synchronized,
                playback = group(positionMs = 0L, isPlaying = false),
            ),
        )
        assertTrue(engine.commands.contains("pause"))
    }

    @Test
    fun `a guide the group loads is not forced onto this phone`() {
        playLocally()
        val prepareCountBefore = engine.prepareCount
        controller.join()
        sync.emit(
            GroupSyncState(
                status = GroupSyncState.Status.Synchronized,
                playback = group(positionMs = 0L, mediaId = "ag.latin-bridge"),
            ),
        )
        // Following a Load blindly would let the group replace what this phone
        // is listening to; the screen that asks owns that decision.
        assertEquals(prepareCountBefore, engine.prepareCount)
    }

    /**
     * The heartbeat (D039) makes the group's snapshot arrive every few seconds
     * instead of only when someone touches something, which puts every branch
     * of the correction on a five-second loop. A guide that has finished on
     * both phones leaves the group node saying "playing" — nobody publishes a
     * stop when a guide simply ends — and each snapshot then asks this phone
     * to resume a guide that is over.
     */
    @Test
    fun `a guide that has ended is not resurrected by a group nobody stopped`() {
        playLocally()
        engine.advanceTo(720_000L)
        engine.finish()
        playback.refreshProgress()
        controller.join()

        val commandsBefore = engine.commands.size

        // Five snapshots, as five heartbeats would deliver them, with the
        // group still anchored back where the shared listen began.
        repeat(5) {
            nowMs += 5_000L
            sync.emit(
                GroupSyncState(
                    status = GroupSyncState.Status.Synchronized,
                    playback = group(positionMs = 0L, anchorServerMs = 0L),
                    clock = ServerClock.fromObservation(serverMs = 900_000L, deviceMs = nowMs),
                ),
            )
        }

        assertEquals(
            "a finished guide must not be seeked and played once per heartbeat",
            commandsBefore,
            engine.commands.size,
        )
    }

    // -- the shared start -------------------------------------------------

    @Test
    fun `starting together anchors on server time, not the device clock`() {
        // Device is two minutes behind the server.
        controller.join()
        sync.emit(
            GroupSyncState(
                status = GroupSyncState.Status.Synchronized,
                clock = ServerClock.fromObservation(serverMs = nowMs + 120_000L, deviceMs = nowMs),
            ),
        )

        controller.startTogether(guide, positionMs = 0L)

        val published = sync.published.single()
        assertEquals(guide, published.mediaId)
        assertTrue(published.isPlaying)
        // now + offset + the 3s countdown, all in server time.
        assertEquals(nowMs + 120_000L + 3_000L, published.anchorServerMs)
    }

    @Test
    fun `the countdown runs three two one and then clears`() {
        controller.startTogether(guide, positionMs = 0L)
        assertEquals(3, controller.state.value.countdown)
        assertTrue(controller.state.value.isStarting)

        controller.tickCountdown()
        assertEquals(2, controller.state.value.countdown)
        controller.tickCountdown()
        assertEquals(1, controller.state.value.countdown)
        controller.tickCountdown()
        assertNull(controller.state.value.countdown)
        assertTrue(!controller.state.value.isStarting)
    }

    // -- silence is not failure, and silence is not health -----------------

    /**
     * The bug this whole heartbeat exists for.
     *
     * Two people listening to the same guide write nothing for minutes, and on
     * the device screen 09 accused a perfectly reachable group of having
     * failed after twenty quiet seconds — while edits made from outside the
     * app were still arriving within a second.
     */
    @Test
    fun `a quiet group that is still reachable stays synchronized`() = runBlocking {
        playLocally()
        controller.join()
        sync.emit(GroupSyncState(GroupSyncState.Status.Synchronized))
        controller.startTogether(guide, positionMs = 0L)

        // Two minutes in which nobody touches anything.
        repeat(24) {
            nowMs += 5_000L
            controller.beat()
        }

        assertEquals(GroupSyncState.Status.Synchronized, controller.state.value.status)
        assertTrue(
            "a reachable group must not be reported as failed",
            !controller.state.value.isDegraded,
        )
    }

    @Test
    fun `a group that stops answering is reported as reconnecting`() = runBlocking {
        playLocally()
        controller.join()
        sync.emit(GroupSyncState(GroupSyncState.Status.Synchronized))
        controller.startTogether(guide, positionMs = 0L)
        controller.beat()
        assertTrue(!controller.state.value.isDegraded)

        sync.reachable = false

        // Still inside the window.
        nowMs += 19_000L
        controller.beat()
        assertEquals(GroupSyncState.Status.Synchronized, controller.state.value.status)

        // Past the twenty seconds nothing has been acknowledged in.
        nowMs += 2_000L
        controller.beat()
        assertEquals(GroupSyncState.Status.Reconnecting, controller.state.value.status)
        assertTrue(controller.state.value.isDegraded)
    }

    @Test
    fun `data arriving while unreachable does not claim the group is back`() = runBlocking {
        playLocally()
        controller.join()
        sync.emit(GroupSyncState(GroupSyncState.Status.Synchronized))
        controller.beat()

        sync.reachable = false
        nowMs += 30_000L
        controller.beat()
        assertEquals(GroupSyncState.Status.Reconnecting, controller.state.value.status)

        // Firebase raises events for this phone's own writes before they reach
        // anyone, so a snapshot arriving on its own proves nothing.
        sync.emit(
            GroupSyncState(
                status = GroupSyncState.Status.Synchronized,
                participants = listOf(GroupParticipant("erika", ParticipantSync.Synchronized)),
            ),
        )
        assertEquals(GroupSyncState.Status.Reconnecting, controller.state.value.status)
    }

    @Test
    fun `going quiet never touches the audio`() = runBlocking {
        playLocally()
        controller.join()
        sync.emit(GroupSyncState(GroupSyncState.Status.Synchronized))
        controller.beat()
        val positionBefore = playback.state.value.positionMs

        sync.reachable = false
        nowMs += 60_000L
        controller.beat()

        assertTrue(playback.state.value.isPlaying)
        assertEquals(positionBefore, playback.state.value.positionMs)
    }

    @Test
    fun `a beat landing again clears the reconnecting state`() = runBlocking {
        playLocally()
        controller.join()
        sync.emit(GroupSyncState(GroupSyncState.Status.Synchronized))
        controller.beat()

        sync.reachable = false
        nowMs += 30_000L
        controller.beat()
        assertEquals(GroupSyncState.Status.Reconnecting, controller.state.value.status)

        sync.reachable = true
        controller.beat()
        assertEquals(GroupSyncState.Status.Synchronized, controller.state.value.status)
        assertEquals(
            ParticipantSync.Synchronized,
            controller.state.value.participants.first().sync,
        )
    }

    @Test
    fun `staleness says nothing before the group has ever been reached`() = runBlocking {
        sync.reachable = false
        controller.join()
        nowMs += 120_000L
        controller.beat()
        // Never reached a live group: there is nothing to have gone away.
        assertEquals(GroupSyncState.Status.Disabled, controller.state.value.status)
    }

    @Test
    fun `a group that stopped answering cannot move this phone`() = runBlocking {
        playLocally()
        engine.advanceTo(10_000L)
        playback.refreshProgress()
        controller.join()
        controller.beat()

        sync.reachable = false
        nowMs += 30_000L
        controller.beat()

        val commandsBefore = engine.commands.size
        sync.emit(
            GroupSyncState(
                status = GroupSyncState.Status.Synchronized,
                playback = group(positionMs = 600_000L, anchorServerMs = 0L),
            ),
        )
        assertEquals(commandsBefore, engine.commands.size)
    }

    // -- what the screens are told ---------------------------------------

    @Test
    fun `this phone reads as synchronized to itself while it is being heard`() {
        controller.join()
        sync.emit(
            GroupSyncState(
                status = GroupSyncState.Status.Reconnecting,
                participants = listOf(GroupParticipant("erika", ParticipantSync.Reconnecting)),
            ),
        )

        val participants = controller.state.value.participants
        assertEquals("vinicius", participants.first().id)
        assertEquals(ParticipantSync.Synchronized, participants.first().sync)
        assertEquals(ParticipantSync.Reconnecting, participants.last().sync)
    }

    /**
     * On the device, screen 09 showed "Vinícius · você — Sincronizado" sitting
     * directly above "Não foi possível sincronizar o grupo agora". Both cannot
     * be true: when this phone is the one that lost the server, its own row is
     * not in step with anybody.
     */
    @Test
    fun `this phone's own row goes amber when it is the one that lost the group`() = runBlocking {
        controller.join()
        sync.emit(
            GroupSyncState(
                status = GroupSyncState.Status.Synchronized,
                participants = listOf(GroupParticipant("erika", ParticipantSync.Synchronized)),
            ),
        )
        controller.beat()
        assertEquals(
            ParticipantSync.Synchronized,
            controller.state.value.participants.first().sync,
        )

        sync.reachable = false
        nowMs += 30_000L
        controller.beat()

        val mine = controller.state.value.participants.first()
        assertEquals("vinicius", mine.id)
        assertEquals(ParticipantSync.Reconnecting, mine.sync)
    }

    @Test
    fun `a group never joined is not a group that failed`() {
        controller.join()
        sync.emit(GroupSyncState(GroupSyncState.Status.Reconnecting))
        // Nothing shared yet, so screen 09 has no reason for the amber note.
        assertTrue(!controller.state.value.isDegraded)

        controller.startTogether(guide, positionMs = 0L)
        sync.emit(GroupSyncState(GroupSyncState.Status.Reconnecting))
        assertTrue(controller.state.value.isDegraded)
    }

    private class FakeGroupSync : GroupSyncRepository {
        private val flow = MutableStateFlow(GroupSyncState())
        val published = mutableListOf<GroupPlayback>()
        var connectedAs: String? = null

        override val state: Flow<GroupSyncState> = flow

        fun emit(next: GroupSyncState) {
            flow.value = next
        }

        override suspend fun connect(participantId: String) {
            connectedAs = participantId
        }

        override suspend fun disconnect() {
            connectedAs = null
        }

        override suspend fun publish(playback: GroupPlayback) {
            published += playback
        }

        /** Whether the server is acknowledging this phone's round trips. */
        var reachable: Boolean = true

        override suspend fun heartbeat(): Boolean = reachable
    }
}
