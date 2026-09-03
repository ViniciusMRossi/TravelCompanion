package com.travelcompanion.app.service.playback

/**
 * Holds commands issued before a connection exists, and replays them in order.
 *
 * Connecting to the media session is asynchronous, but the traveller's first
 * tap issues two commands back to back: prepare, then play. Keeping only the
 * most recent one would drop the `prepare` and leave `play` with nothing
 * loaded, so order and completeness both matter here.
 */
class ConnectionQueue<T> {

    private var target: T? = null
    private val pending = ArrayDeque<(T) -> Unit>()

    val isConnected: Boolean get() = target != null

    /** Queued commands waiting for a connection. Exposed for assertions. */
    val pendingCount: Int get() = pending.size

    /** Runs [action] now if connected, otherwise queues it in order. */
    fun submit(action: (T) -> Unit) {
        val connected = target
        if (connected != null) action(connected) else pending.addLast(action)
    }

    /** Connects and drains everything queued so far, oldest first. */
    fun attach(target: T) {
        this.target = target
        while (pending.isNotEmpty()) {
            pending.removeFirst().invoke(target)
        }
    }

    fun detach() {
        target = null
        pending.clear()
    }

    /** The live target, or null while still connecting. */
    fun current(): T? = target
}
