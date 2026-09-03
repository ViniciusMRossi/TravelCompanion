package com.travelcompanion.app.service.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Connecting to the media session is asynchronous, and the traveller's first
 * tap issues `prepare` immediately followed by `play`. If the queue kept only
 * the latest command, `play` would arrive with nothing loaded and the tap
 * would silently do nothing — the exact failure this guards.
 */
class ConnectionQueueTest {

    @Test
    fun commandsIssuedBeforeConnectingAreReplayedInOrder() {
        val queue = ConnectionQueue<MutableList<String>>()
        val recorded = mutableListOf<String>()

        queue.submit { it += "prepare" }
        queue.submit { it += "play" }

        assertFalse(queue.isConnected)
        assertEquals(2, queue.pendingCount)
        assertTrue("nothing runs before a connection exists", recorded.isEmpty())

        queue.attach(recorded)

        assertEquals(listOf("prepare", "play"), recorded)
        assertEquals("the media item is loaded before playback starts", 0, recorded.indexOf("prepare"))
        assertEquals(0, queue.pendingCount)
    }

    @Test
    fun commandsIssuedAfterConnectingRunImmediately() {
        val queue = ConnectionQueue<MutableList<String>>()
        val recorded = mutableListOf<String>()
        queue.attach(recorded)

        queue.submit { it += "pause" }

        assertEquals(listOf("pause"), recorded)
        assertEquals(0, queue.pendingCount)
    }

    @Test
    fun aSecondRequestBeforeConnectingKeepsBothPrepareAndPlay() {
        val queue = ConnectionQueue<MutableList<String>>()
        val recorded = mutableListOf<String>()

        queue.submit { it += "prepare:A" }
        queue.submit { it += "play:A" }
        queue.submit { it += "prepare:B" }
        queue.submit { it += "play:B" }

        queue.attach(recorded)

        assertEquals(listOf("prepare:A", "play:A", "prepare:B", "play:B"), recorded)
        assertEquals("the last request wins because it is applied last", "play:B", recorded.last())
    }

    @Test
    fun detachingDropsTheConnectionAndAnythingStillQueued() {
        val queue = ConnectionQueue<MutableList<String>>()
        queue.submit { it += "prepare" }

        queue.detach()

        assertFalse(queue.isConnected)
        assertEquals(0, queue.pendingCount)
    }

    @Test
    fun afterAFailedConnectionARetryReplaysOnlyTheNewCommands() {
        val queue = ConnectionQueue<MutableList<String>>()
        val recorded = mutableListOf<String>()

        // First attempt: the traveller taps, the connection never lands.
        queue.submit { it += "prepare:old" }
        queue.submit { it += "play:old" }
        queue.detach() // buildAsync failed — drop what nothing will ever run

        // Second attempt: a fresh tap, then the connection succeeds.
        queue.submit { it += "prepare:new" }
        queue.submit { it += "play:new" }
        queue.attach(recorded)

        assertEquals(listOf("prepare:new", "play:new"), recorded)
        assertTrue("no stale command may resurface", recorded.none { it.endsWith(":old") })
    }

    @Test
    fun aDisconnectRequiresTheNextCommandToWaitForANewConnection() {
        val queue = ConnectionQueue<MutableList<String>>()
        val first = mutableListOf<String>()
        queue.attach(first)
        queue.submit { it += "play" }
        assertEquals(listOf("play"), first)

        // The session goes away underneath us.
        queue.detach()
        assertFalse(queue.isConnected)

        queue.submit { it += "pause" }
        assertEquals("it must not reach the dead controller", listOf("play"), first)
        assertEquals(1, queue.pendingCount)

        val second = mutableListOf<String>()
        queue.attach(second)
        assertEquals(listOf("pause"), second)
    }
}
