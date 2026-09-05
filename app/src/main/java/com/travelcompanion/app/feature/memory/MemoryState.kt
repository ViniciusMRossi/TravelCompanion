package com.travelcompanion.app.feature.memory

import com.travelcompanion.app.data.memory.Memory
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.memory.MemoryAttribution
import com.travelcompanion.app.domain.memory.MemoryFailure
import com.travelcompanion.app.domain.memory.MemoryPhase
import com.travelcompanion.app.domain.memory.MemoryPlaybackState
import com.travelcompanion.app.domain.memory.MemoryRecordingState
import com.travelcompanion.app.domain.walk.WalkModeState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Screen 12 state. Derived; nothing here is stored. */
data class MemoryUiState(
    val contextLine: String,
    val phase: MemoryPhase,
    val elapsedLabel: String,
    val attributionLine: String,
    val note: String?,
    val savedConfirmation: SavedConfirmationUi?,
    val previous: List<SavedMemoryUi>,
)

/** The "Salva" state: where, when and how long, and nothing else. */
data class SavedConfirmationUi(
    val place: String,
    val time: String,
    val duration: String,
)

/** One row of "Memórias desta viagem". */
data class SavedMemoryUi(
    val id: String,
    val title: String,
    val date: String,
    val author: String,
    val duration: String,
    val isPlaying: Boolean = false,
    /** 0..1, for the 5dp bar drawn inside this row while it plays. */
    val progress: Float = 0f,
    /** Elapsed time, shown beside the duration only while playing. */
    val positionLabel: String? = null,
    /** "Enviada para WhatsApp", once the traveller has picked a destination. */
    val sharedTo: String? = null,
    /** The file behind this row would not play. */
    val unplayable: Boolean = false,
)

/**
 * Builds screen 12.
 *
 * Where the traveller is comes from the packaged trip and from the walk this
 * phone is already running — the same `domain/walk` state screen 07 reads, not
 * a second location path. When the walk is not running, or location was never
 * granted, the city still stands: the promise on the idle screen is that the
 * place and the hour arrive by themselves, and the hour always does (D058).
 */
fun buildMemoryState(
    content: TripContent,
    walkState: WalkModeState,
    recording: MemoryRecordingState,
    saved: List<Memory>,
    localParticipantId: String?,
    elapsedMs: Long,
    nowEpochMs: Long,
    zone: ZoneId,
    playback: MemoryPlaybackState = MemoryPlaybackState(),
    sharedTo: Map<String, String> = emptyMap(),
): MemoryUiState {
    val place = placeName(walkState)
    // The city of the day being lived, from the clock this screen already
    // holds. Taking the first city in the package meant the line at the top
    // named one place and the attribution beneath it named another the moment
    // recording started — the same screen saying two things (D080).
    val city = content
        // `LocalDate.ofInstant` is API 34; this is the same answer at 26.
        .dayFor(Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate())
        // fallback: a day that declares no base still has to name somewhere,
        // and its first city is where it begins.
        ?.let { day -> content.city(day.baseCityId ?: day.cityIds.firstOrNull()) }
        ?.name
    val participant = content.participant(localParticipantId)

    return MemoryUiState(
        contextLine = listOfNotNull(city, place, formatClock(nowEpochMs, zone)).joinToString(" · "),
        phase = recording.phase,
        elapsedLabel = formatDuration(elapsedMs),
        attributionLine = listOfNotNull(
            recording.attribution?.cityName ?: city,
            recording.attribution?.placeName ?: place,
            recording.attribution?.participantName ?: participant?.name,
        ).joinToString(" · "),
        note = recording.failure?.let(::failureNote),
        savedConfirmation = if (recording.phase == MemoryPhase.Saved) {
            SavedConfirmationUi(
                place = listOfNotNull(
                    recording.attribution?.cityName ?: city,
                    recording.attribution?.placeName ?: place,
                ).joinToString(" · ").ifEmpty { "Local não registrado" },
                time = formatClock(recording.startedAtEpochMs ?: nowEpochMs, zone),
                duration = formatDuration(recording.bankedMs),
            )
        } else {
            null
        },
        previous = saved.map { memory -> memory.toRow(zone, playback, sharedTo[memory.id]) },
    )
}

/**
 * What this phone can offer about who and where, at the moment of recording.
 *
 * The city comes from the day being lived, the same way the place comes from
 * the walk actually running. A memory is the one thing in this app that cannot
 * be redone — it goes into the memory and stays there — so a recording made in
 * Mostar on day 15 must not be kept as Sarajevo forever (D077).
 */
fun buildAttribution(
    content: TripContent,
    walkState: WalkModeState,
    localParticipantId: String?,
    date: LocalDate,
): MemoryAttribution? {
    val participant = content.participant(localParticipantId) ?: return null
    return MemoryAttribution(
        participantId = participant.id,
        participantName = participant.name,
        cityName = content.dayFor(date)
            // fallback: a day that declares no base still has to name
            // somewhere, and its first city is where it begins.
            ?.let { day -> content.city(day.baseCityId ?: day.cityIds.firstOrNull()) }
            ?.name,
        placeName = placeName(walkState),
    )
}

/**
 * The stop the walk last arrived at, when there is one.
 *
 * Deliberately the walk's own state and nothing else: Phase 3 already owns
 * location, and a second path to it would be a second thing to keep true.
 */
private fun placeName(walkState: WalkModeState): String? =
    walkState.currentStop?.title ?: walkState.nextStop?.title.takeIf { walkState.isRunning }

private fun failureNote(failure: MemoryFailure): String = when (failure) {
    // D030's shape: a refused permission costs the recording, not the screen.
    MemoryFailure.NoMicrophonePermission ->
        "Sem acesso ao microfone. Você pode liberar nas configurações do aparelho e tentar de novo."
    MemoryFailure.RecorderFailed ->
        "Não foi possível gravar agora. Tente de novo."
    // The one error that must never read as "nothing happened".
    MemoryFailure.SavedFileOnly ->
        "A gravação foi salva no aparelho, mas não entrou na lista desta viagem."
}

private fun Memory.toRow(
    zone: ZoneId,
    playback: MemoryPlaybackState,
    sharedTo: String?,
) = SavedMemoryUi(
    id = id,
    // A memory has no title and is never asked for one. The row is named by
    // where it happened, which is something the app filled in by itself.
    title = placeName ?: cityName ?: "Memória",
    date = formatDate(recordedAtEpochMs, zone),
    author = participantName,
    duration = formatDuration(durationMs),
    isPlaying = playback.memoryId == id && playback.isPlaying,
    progress = if (playback.memoryId == id) playback.progress else 0f,
    positionLabel = if (playback.memoryId == id) formatDuration(playback.positionMs) else null,
    sharedTo = sharedTo?.let { "Enviada para $it" },
    unplayable = playback.unplayableMemoryId == id,
)

/** "02:41", the shape the approved recorder shows. */
fun formatDuration(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0L)
    return String.format(Locale.ROOT, "%02d:%02d", total / 60, total % 60)
}

private fun formatClock(epochMs: Long, zone: ZoneId): String =
    CLOCK.format(Instant.ofEpochMilli(epochMs).atZone(zone))

private fun formatDate(epochMs: Long, zone: ZoneId): String =
    DATE.format(Instant.ofEpochMilli(epochMs).atZone(zone))

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))
private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "BR"))
