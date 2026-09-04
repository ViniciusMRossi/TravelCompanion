package com.travelcompanion.app.data.sync

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.travelcompanion.app.domain.sync.GroupPlayback
import com.travelcompanion.app.domain.sync.ServerClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Group sync over Firebase Realtime Database, on the paths the brief names:
 *
 * ```text
 * trip/<groupId>/participants/<participantId>
 * trip/<groupId>/playback
 * ```
 *
 * Only live runtime state lives here. No trip content, no audio, no chat —
 * every phone already has the same packaged files, so what crosses is playback
 * state (brief §5). `groupId` comes from `trip.sync.groupId` in the package,
 * so there is nothing for the traveller to join, pair or enter.
 *
 * Sign-in is anonymous and invisible: it exists so database rules can require
 * `auth != null` rather than being open to anyone with the URL. It is not an
 * account and there is no login surface (D035).
 *
 * Every failure path ends the same way — the group becomes unavailable and
 * local playback is never told (brief §3.3). That is why nothing here throws
 * at the caller and no method returns success.
 */
class FirebaseGroupSyncRepository(
    context: Context,
    private val groupId: String,
) : GroupSyncRepository {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(GroupSyncState())
    override val state: Flow<GroupSyncState> = _state.asStateFlow()

    /**
     * Null whenever the app has no Firebase configuration.
     *
     * `google-services.json` is not in the repository, and the plugin that
     * turns it into resources is applied only when the file is there — so a
     * fresh clone has no configuration, and that is a normal state rather than
     * an error (D034).
     */
    private val app: FirebaseApp? by lazy {
        runCatching { FirebaseApp.initializeApp(appContext) }.getOrNull()
    }

    private val database: FirebaseDatabase? by lazy {
        app?.let { runCatching { FirebaseDatabase.getInstance(it) }.getOrNull() }
    }

    private var participantId: String? = null
    private var groupListener: ValueEventListener? = null
    private var connectedListener: ValueEventListener? = null
    private var offsetListener: ValueEventListener? = null

    private fun group(): DatabaseReference? = database?.getReference("trip")?.child(groupId)

    override suspend fun connect(participantId: String) {
        val root = group()
        if (root == null) {
            // No configuration at all. Not "offline": group sync was never
            // switched on for this build.
            _state.value = GroupSyncState(GroupSyncState.Status.Disabled)
            return
        }

        this.participantId = participantId
        _state.update { it.copy(status = GroupSyncState.Status.Connecting) }

        if (!signInAnonymously()) {
            _state.update { it.copy(status = GroupSyncState.Status.Offline) }
            return
        }

        observeGroup(root)
        observeConnectivity()
        observeServerOffset()

        val mine = root.child("participants").child(participantId)
        runCatching {
            mine.setValue(mapOf("seenAt" to ServerValue.TIMESTAMP))
            // The group stops showing someone whose phone went away, without
            // the traveller having to leave anything.
            mine.onDisconnect().removeValue()
        }
    }

    override suspend fun disconnect() {
        val id = participantId
        participantId = null

        groupListener?.let { group()?.removeEventListener(it) }
        connectedListener?.let { database?.getReference(".info/connected")?.removeEventListener(it) }
        offsetListener?.let { database?.getReference(".info/serverTimeOffset")?.removeEventListener(it) }
        groupListener = null
        connectedListener = null
        offsetListener = null

        if (id != null) {
            runCatching { group()?.child("participants")?.child(id)?.removeValue() }
        }
        _state.value = GroupSyncState()
    }

    override suspend fun publish(playback: GroupPlayback) {
        val ref = group()?.child("playback") ?: return
        // Best-effort by contract: a publish that never lands is not an error
        // anything local has to handle.
        runCatching {
            ref.setValue(
                mapOf(
                    "mediaId" to playback.mediaId,
                    "positionMs" to playback.positionMs,
                    "isPlaying" to playback.isPlaying,
                    "anchorServerMs" to playback.anchorServerMs,
                    "updatedBy" to playback.updatedBy,
                ),
            )
        }
    }

    /**
     * Refreshes this phone's `seenAt` and reports whether the server took it.
     *
     * A Realtime Database write completes only once the server acknowledges
     * it, so the completion — not the write — is the evidence. Firebase also
     * raises local events for our own writes straight away, which is why the
     * data coming back through [state] cannot be used for this.
     *
     * The same write is what lets the other phones see this one as present, so
     * liveness costs one small write and no extra traffic.
     */
    override suspend fun heartbeat(): Boolean {
        val id = participantId ?: return false
        val mine = group()?.child("participants")?.child(id) ?: return false
        return runCatching {
            mine.setValue(mapOf("seenAt" to ServerValue.TIMESTAMP)).awaitSuccess()
        }.getOrDefault(false)
    }

    // ---------------------------------------------------------------------

    private fun observeGroup(root: DatabaseReference) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _state.update { current ->
                    current.copy(
                        status = GroupSyncState.Status.Synchronized,
                        participants = participantsFrom(snapshot),
                        playback = playbackFrom(snapshot),
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Reported as a group state, never thrown: rules refusing a
                // read is the group becoming unavailable, nothing more.
                _state.update { it.copy(status = GroupSyncState.Status.Offline) }
            }
        }
        groupListener = listener
        root.addValueEventListener(listener)
    }

    /**
     * Firebase's own view of the connection, turned into the two words the
     * approved design uses. The vocabulary stops here: screens say
     * "Sincronizado" and "Sincronizando novamente".
     */
    private fun observeConnectivity() {
        val ref = database?.getReference(".info/connected") ?: return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                _state.update {
                    if (connected) it else it.copy(status = GroupSyncState.Status.Reconnecting)
                }
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        connectedListener = listener
        ref.addValueEventListener(listener)
    }

    /**
     * Keeps a rough device/server offset, which is what a synchronized start
     * is agreed in (brief §6). Rough is enough — this is narration, not
     * multi-speaker music.
     */
    private fun observeServerOffset() {
        val ref = database?.getReference(".info/serverTimeOffset") ?: return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offset = snapshot.getValue(Long::class.java) ?: 0L
                _state.update { it.copy(clock = ServerClock(offsetMs = offset)) }
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        offsetListener = listener
        ref.addValueEventListener(listener)
    }

    /**
     * Invisible sign-in. No screen, no account, nothing the traveller does.
     *
     * Returns false rather than throwing: failing to sign in means the group
     * is unavailable, which is a state the screens render, not an error the
     * player hears about.
     */
    private suspend fun signInAnonymously(): Boolean {
        val auth = app?.let { runCatching { FirebaseAuth.getInstance(it) }.getOrNull() } ?: return false
        if (auth.currentUser != null) return true
        return runCatching { auth.signInAnonymously().awaitOrNull() != null }.getOrDefault(false)
    }

    /**
     * Who the group can see, and which of them are still checking in.
     *
     * Someone whose phone is gone is removed by `onDisconnect`, but Firebase
     * takes its own time to notice. `seenAt` is faster and is what screen 09's
     * amber dot is for: still in the group, not currently keeping up.
     */
    private fun participantsFrom(snapshot: DataSnapshot): List<GroupParticipant> {
        val serverNow = _state.value.clock.serverNow(System.currentTimeMillis())
        return snapshot.child("participants").children.mapNotNull { child ->
            val id = child.key ?: return@mapNotNull null
            val seenAt = child.child("seenAt").getValue(Long::class.java)
            val keepingUp = seenAt != null && serverNow - seenAt <= PARTICIPANT_STALE_AFTER_MS
            GroupParticipant(
                id = id,
                sync = if (keepingUp) ParticipantSync.Synchronized else ParticipantSync.Reconnecting,
            )
        }
    }

    private fun playbackFrom(snapshot: DataSnapshot): GroupPlayback? {
        val playback = snapshot.child("playback")
        val mediaId = playback.child("mediaId").getValue(String::class.java) ?: return null
        return GroupPlayback(
            mediaId = mediaId,
            positionMs = playback.child("positionMs").getValue(Long::class.java) ?: 0L,
            isPlaying = playback.child("isPlaying").getValue(Boolean::class.java) ?: false,
            anchorServerMs = playback.child("anchorServerMs").getValue(Long::class.java) ?: 0L,
            updatedBy = playback.child("updatedBy").getValue(String::class.java).orEmpty(),
        )
    }

    private companion object {
        /**
         * Long enough to survive a missed beat, short enough that someone who
         * walked out of signal is shown as such while the other person is
         * still looking at the screen. Matches the group-level threshold, so
         * one person dropping and the group going quiet read the same way.
         */
        const val PARTICIPANT_STALE_AFTER_MS = 20_000L
    }
}

/**
 * A Play Services [Task] as a suspending call, without pulling in
 * `kotlinx-coroutines-play-services` for one await (the dependency baseline
 * asks for deliberate dependency changes).
 *
 * Failure resolves to null rather than throwing, which is the contract the
 * whole repository keeps.
 */
private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        continuation.resume(if (task.isSuccessful) task.result else null)
    }
}

/**
 * Whether a write landed on the server.
 *
 * Separate from [awaitOrNull] because a `Task<Void>` carries a null result on
 * success, so "did it produce something" cannot answer "did it arrive".
 */
private suspend fun Task<Void>.awaitSuccess(): Boolean = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task -> continuation.resume(task.isSuccessful) }
}
