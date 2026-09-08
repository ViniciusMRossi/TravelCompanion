package com.travelcompanion.app.domain.more

import com.travelcompanion.app.data.sync.GroupParticipant
import com.travelcompanion.app.data.sync.ParticipantSync
import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.data.trip.TripContent

/** One traveller in the Grupo section, with the word for their state. */
data class GroupMemberUi(
    val initial: String,
    val name: String,
    val label: String,
    val sync: ParticipantSync?,
)

/** A Plan B, as the "Na estrada" list links to it. */
data class PlanBLinkUi(val id: String, val scenario: String)

/** An app the package suggests for this trip. */
data class UsefulAppUi(val name: String, val action: ActionLink)

/** One line of "conteúdo salvo", with what it occupies on the device. */
data class SavedContentUi(val label: String, val count: Int, val bytes: Long)

/** Screen 19 state. */
data class MoreUiState(
    val planBs: List<PlanBLinkUi>,
    val apps: List<UsefulAppUi>,
    val members: List<GroupMemberUi>,
    val groupNote: String,
    val tripTitle: String,
    val tripSubtitle: String?,
    val tripDates: String,
    val savedContent: List<SavedContentUi>,
    val savedTotalLabel: String,
)

/**
 * Builds screen 19.
 *
 * The Grupo section reads the state the app already holds and never asks for a
 * new one: joining the group costs a write every five seconds and D039 keeps
 * that to screen 09, which is the screen where being in the group is the point.
 * A traveller who has not opened 09 in this session sees their group as offline
 * and the sentence below explains what that means — which is what the approved
 * sheet asks for, a promise about §3.3 rather than a live reading (D071).
 */
fun buildMoreState(
    content: TripContent,
    known: List<GroupParticipant>,
    localParticipantId: String?,
): MoreUiState {
    val syncById = known.associate { it.id to it.sync }

    return MoreUiState(
        planBs = content.trip.planBs.map { PlanBLinkUi(it.id, it.scenario) },
        // `usefulApps.countryCodes` is read by nobody, and on purpose. The
        // package declares seven apps for six countries, and five of them
        // carry codes; filtering the list by the day's country would hide the
        // translator on 14/09 in Amsterdam — the very morning to download
        // Albanian, Bosnian, Croatian and Greek before there is no signal —
        // and hide Ferryhopper on every day but the Corfu crossing it sold.
        // Seven lines fit on one screen. The field documents coverage; it is
        // not an instruction to hide (D178).
        apps = content.trip.usefulApps.map { UsefulAppUi(it.name, it.action) },
        members = content.info.participants.map { person ->
            val sync = syncById[person.id]
            GroupMemberUi(
                initial = person.initial,
                name = if (person.id == localParticipantId) "${person.name} · você" else person.name,
                label = sync.label(),
                sync = sync,
            )
        },
        groupNote = GROUP_NOTE,
        tripTitle = content.info.title,
        tripSubtitle = content.info.subtitle,
        tripDates = "${content.info.startDate} — ${content.info.endDate}",
        savedContent = savedContent(content),
        savedTotalLabel = formatBytes(savedContent(content).sumOf { it.bytes }),
    )
}

/**
 * What is actually on the device, by kind.
 *
 * Only files this build really carries are counted: the wallet already refuses
 * to badge a promised file as offline (D013), and a screen that reports megabytes
 * of content nobody can open would be the same untruth one level up.
 */
private fun savedContent(content: TripContent): List<SavedContentUi> =
    content.trip.assets
        .mapNotNull { asset ->
            content.assets.sizeInBytes(asset.id)?.let { bytes -> asset.type to bytes }
        }
        .groupBy({ it.first }, { it.second })
        .mapNotNull { (type, sizes) ->
            labelFor(type)?.let { SavedContentUi(it, sizes.size, sizes.sum()) }
        }
        .sortedByDescending { it.bytes }

private fun labelFor(type: String): String? = when (type) {
    "audio" -> "Áudio"
    "image" -> "Imagens"
    "document" -> "Documentos"
    else -> null
}

/** The approved words for each state; "Offline" is S2's fourth. */
private fun ParticipantSync?.label(): String = when (this) {
    ParticipantSync.Synchronized -> "Sincronizado"
    ParticipantSync.Reconnecting -> "Sincronizando novamente"
    null -> "Offline"
}

/** "12,4 MB". Rounded the way a person reads a size, not to the byte. */
fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> "${bytes / 1_000} kB"
    else -> "$bytes B"
}

private const val GROUP_NOTE =
    "O grupo se reencontra sozinho quando houver internet, sem que nada pare de funcionar."
