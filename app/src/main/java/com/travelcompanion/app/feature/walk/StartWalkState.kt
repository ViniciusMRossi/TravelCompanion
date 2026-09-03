package com.travelcompanion.app.feature.walk

import com.travelcompanion.app.data.trip.Participant
import com.travelcompanion.app.data.trip.TripContent
import java.time.LocalDate

/** One line of screen 06's readiness checklist. */
data class ReadinessLine(
    val label: String,
    val status: String?,
    val statusTone: ReadinessTone,
)

enum class ReadinessTone { Ready, Enabled, Muted }

/**
 * Screen 06 state.
 *
 * Everything here is read from the packaged trip and from the device itself.
 * Nothing is asked of the network, because this is the screen the traveller
 * reads before putting the phone away — very often with the radios off.
 */
data class StartWalkUiState(
    val walkId: String,
    val eyebrow: String,
    val title: String,
    val distanceLabel: String,
    val durationLabel: String,
    val storyCountLabel: String,
    val routeLabel: String?,
    val headphonesConnected: Boolean,
    val automaticStories: Boolean,
    val locationGranted: Boolean,
    val participants: List<Participant>,
) {
    val readiness: List<ReadinessLine>
        get() = listOf(
            ReadinessLine(
                label = "Fones conectados",
                status = if (headphonesConnected) "Prontos" else "Nenhum",
                statusTone = if (headphonesConnected) ReadinessTone.Ready else ReadinessTone.Muted,
            ),
            ReadinessLine(
                label = "Histórias pelo caminho",
                // The line reports what will actually happen, not what was
                // asked for: without location the app cannot notice arrivals,
                // and saying "Ativado" then would be a promise it cannot keep.
                status = when {
                    !automaticStories -> "Desativado"
                    locationGranted -> "Ativado"
                    else -> "Sem localização"
                },
                statusTone = if (automaticStories && locationGranted) {
                    ReadinessTone.Enabled
                } else {
                    ReadinessTone.Muted
                },
            ),
        )

    /** The paragraph under the checklist, which changes with what is actually on. */
    val offlineNote: String
        get() = if (automaticStories && locationGranted) {
            "Durante o passeio, o app avisa quando vocês chegarem perto de um lugar " +
                "interessante, mesmo com a tela apagada. Os áudios já estão no aparelho."
        } else {
            "O passeio funciona do mesmo jeito: o áudio já está no aparelho. Sem " +
                "localização, as histórias não aparecem sozinhas pelo caminho."
        }
}

/**
 * Builds screen 06 from content.
 *
 * The start time comes from the day's timeline entry for this walk, the same
 * association screen 05 uses; a walk that is not on today's timeline simply
 * shows no time rather than inventing one.
 */
fun buildStartWalkState(
    content: TripContent,
    walkId: String,
    date: LocalDate,
    headphonesConnected: Boolean,
    automaticStories: Boolean,
    locationGranted: Boolean,
): StartWalkUiState? {
    val walk = content.walk(walkId) ?: return null
    val startTime = content.dayFor(date)
        ?.timeline
        ?.firstOrNull { it.kind == "walk" && it.refId == walk.id }
        ?.startTime

    val stops = walk.stops.size
    return StartWalkUiState(
        walkId = walk.id,
        eyebrow = startTime?.let { "Caminhada guiada · começa às $it" } ?: "Caminhada guiada",
        title = walk.title,
        distanceLabel = formatDistance(walk.distanceMeters),
        durationLabel = "${walk.durationMinutes} min",
        storyCountLabel = if (stops == 1) "1 história" else "$stops histórias",
        routeLabel = walk.routeLabel,
        headphonesConnected = headphonesConnected,
        automaticStories = automaticStories,
        locationGranted = locationGranted,
        participants = content.info.participants,
    )
}

/** "1,8 km" past a kilometre, plain metres below it — as the prototype reads. */
fun formatDistance(meters: Int): String =
    if (meters >= 1000) {
        val km = meters / 1000.0
        String.format(java.util.Locale("pt", "BR"), "%.1f km", km)
    } else {
        "$meters m"
    }
