package com.travelcompanion.app.service.sync

import com.travelcompanion.app.data.sync.GroupParticipant
import com.travelcompanion.app.data.sync.GroupSyncRepository
import com.travelcompanion.app.data.sync.GroupSyncState
import com.travelcompanion.app.data.sync.ParticipantSync
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.domain.sync.GroupPlayback
import com.travelcompanion.app.domain.sync.ServerClock
import com.travelcompanion.app.service.playback.AudioGuideRequest
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

    /**
     * Derived by reasoning about the arithmetic rather than seen on the
     * device: `startTogether` publishes an anchor three seconds out with the
     * position playback is at *now*, and the audio does not stop while the
     * countdown runs. `expectedPosition` holds the group at its published
     * position until the anchor arrives, so the gap between it and the local
     * player widens by a second per second — and two of the three seconds are
     * past the drift tolerance.
     */
    @Test
    fun `the countdown of a shared start does not drag the player backwards`() {
        playLocally()
        engine.advanceTo(300_000L)
        playback.refreshProgress()
        controller.join()
        // Device and server agree at first, so the arithmetic reads plainly.
        sync.emit(GroupSyncState(GroupSyncState.Status.Synchronized))

        controller.startTogether(guide, positionMs = playback.state.value.positionMs)
        val published = sync.published.single()

        val commandsBefore = engine.commands.size

        // The three seconds of 3–2–1. Server time moves; the device clock is
        // held still so only the offset changes, which is also what makes each
        // snapshot a distinct value the flow will deliver.
        listOf(500L, 1_500L, 2_500L).forEach { elapsed ->
            engine.advanceTo(300_000L + elapsed)
            playback.refreshProgress()
            sync.emit(
                GroupSyncState(
                    status = GroupSyncState.Status.Synchronized,
                    playback = published,
                    clock = ServerClock(offsetMs = elapsed),
                ),
            )
        }

        assertEquals(
            "nothing may move the player before the agreed start moment arrives",
            commandsBefore,
            engine.commands.size,
        )
    }

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

    // -- arriving on screens 08 and 09 ------------------------------------

    /**
     * The other phone, as a value.
     *
     * Every test below is two participants: this one, driven through a real
     * [PlaybackController], and Érika's, present only as what she wrote to the
     * group. No second device is needed to make the disagreements happen — the
     * whole point of deciding them in `domain/sync` is that they are values.
     */
    private fun erikaIsListeningTo(
        mediaId: String,
        positionMs: Long,
        serverNowMs: Long,
        isPlaying: Boolean = true,
    ) = GroupSyncState(
        status = GroupSyncState.Status.Synchronized,
        participants = listOf(GroupParticipant("erika", ParticipantSync.Synchronized)),
        playback = GroupPlayback(mediaId, positionMs, isPlaying, anchorServerMs = 0L, updatedBy = "erika"),
        clock = ServerClock.fromObservation(serverMs = serverNowMs, deviceMs = nowMs),
    )

    /** What screens 08 and 09 hand the controller: the packaged trip, resolved. */
    private fun guides(): (String) -> AudioGuideRequest? = { audioGuideRequest(content, it) }

    /**
     * The failure this whole section exists for.
     *
     * Érika starts a shared listen; the other traveller opens screen 09 with
     * nothing playing. Before, the correction came back as `Load`, the
     * controller did nothing with it, and screen 09 answered "Comece um
     * audioguia para ouvir junto" while Érika's phone counted down to a guide
     * this one never heard.
     */
    @Test
    fun `a phone with nothing playing joins a listen already in progress`() {
        controller.join()
        controller.listenTogether(guides())

        sync.emit(erikaIsListeningTo("ag.latin-bridge", positionMs = 60_000L, serverNowMs = 30_000L))

        assertEquals("ag.latin-bridge", playback.state.value.mediaId)
        // Where the group is now, not where this phone last left the guide.
        assertEquals(90_000L, engine.preparedStartPositionMs)
        assertTrue("the joining phone must actually be playing", playback.state.value.isPlaying)
    }

    /**
     * D037, with the half that was missing added: the group may not replace
     * what this phone is listening to, and this phone may not replace the
     * group's either. Both were true before; what was missing is that neither
     * screen said so, and both read "Sincronizado".
     */
    @Test
    fun `a phone playing another guide keeps it and is not reported as synchronized`() {
        playLocally()
        val prepareCountBefore = engine.prepareCount
        controller.join()
        controller.listenTogether(guides())

        sync.emit(erikaIsListeningTo("ag.latin-bridge", positionMs = 60_000L, serverNowMs = 30_000L))

        assertEquals("the guide must not be swapped", guide, playback.state.value.mediaId)
        assertEquals(prepareCountBefore, engine.prepareCount)
        // And the group is not dragged onto this phone's guide either.
        assertTrue("arriving must not publish over a listen in progress", sync.published.isEmpty())
        assertTrue("the screen has to say the two are apart", controller.state.value.isDiverged)
    }

    /** Once this phone is on the group's guide, there is nothing to report. */
    @Test
    fun `agreeing with the group is not a divergence`() {
        playLocally()
        controller.join()
        controller.listenTogether(guides())

        sync.emit(erikaIsListeningTo(guide, positionMs = 0L, serverNowMs = 0L))

        assertTrue(!controller.state.value.isDiverged)
    }

    /**
     * Screen 09 → 07 → 09, which is one back-press and one tap.
     *
     * Every arrival used to publish a fresh anchor carrying this phone's own
     * position, so the traveller who last opened the screen pulled everyone
     * else to where they were — as often as they liked.
     */
    @Test
    fun `re-entering the screen on the guide the group is playing re-anchors nobody`() {
        playLocally()
        engine.advanceTo(240_000L)
        playback.refreshProgress()

        controller.join()
        controller.listenTogether(guides())
        sync.emit(erikaIsListeningTo(guide, positionMs = 0L, serverNowMs = 10_000L))
        controller.leave()

        // Back to screen 07, then in again.
        controller.join()
        controller.listenTogether(guides())
        sync.emit(erikaIsListeningTo(guide, positionMs = 0L, serverNowMs = 20_000L))

        assertTrue(
            "joining a listen that is already running is following, not starting",
            sync.published.isEmpty(),
        )
    }

    /** With nothing to join, arriving is still what proposes the shared start. */
    @Test
    fun `arriving first proposes the shared start`() {
        playLocally()
        engine.advanceTo(120_000L)
        playback.refreshProgress()
        controller.join()
        controller.listenTogether(guides())

        sync.emit(
            GroupSyncState(
                status = GroupSyncState.Status.Synchronized,
                participants = listOf(GroupParticipant("erika", ParticipantSync.Synchronized)),
            ),
        )

        val published = sync.published.single()
        assertEquals(guide, published.mediaId)
        assertEquals(120_000L, published.positionMs)
        assertEquals(3, controller.state.value.countdown)
    }

    /**
     * An unreachable group must not leave the traveller on a screen that never
     * does anything: proposing offline is what raises the amber note, and the
     * audio is unaffected either way (brief §3.3).
     */
    @Test
    fun `arriving with the group unreachable still proposes, and says so`() {
        playLocally()
        controller.join()
        controller.listenTogether(guides())

        sync.emit(GroupSyncState(GroupSyncState.Status.Offline))

        assertEquals(guide, sync.published.single().mediaId)
        assertTrue(controller.state.value.isDegraded)
        assertTrue(playback.state.value.isPlaying)
    }

    /** The group still cannot load a guide nobody asked it to. */
    @Test
    fun `a guide the group loads is not taken up without the traveller asking`() {
        controller.join()
        // No listenTogether: this is the group talking, not the traveller.
        sync.emit(erikaIsListeningTo("ag.latin-bridge", positionMs = 0L, serverNowMs = 0L))

        assertEquals(0, engine.prepareCount)
        assertNull(playback.state.value.mediaId)
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
