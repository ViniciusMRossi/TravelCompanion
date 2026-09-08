package com.travelcompanion.app.service.playback

import com.travelcompanion.app.data.trip.TripContent

/**
 * A request to play one packaged audioguide.
 *
 * Building this is pure: it turns a stable audioguide ID into either a local
 * asset URI or an explicit "not packaged" answer, with no player involved.
 */
sealed interface AudioGuideRequest {

    val mediaId: String
    val title: String

    data class Playable(
        override val mediaId: String,
        override val title: String,
        val subtitle: String?,
        /** Always a local `asset:///` URI. Bundled audio is never streamed. */
        val uri: String,
        val declaredDurationMs: Long,
        val chapters: List<PlaybackChapter>,
    ) : AudioGuideRequest

    /** The guide exists in the trip, but its audio file is not in this build. */
    data class NotPackaged(
        override val mediaId: String,
        override val title: String,
    ) : AudioGuideRequest
}

/**
 * Resolves an audioguide to something the player can act on.
 *
 * Returns null only when the trip has no such audioguide at all.
 *
 * A [subtitle] equal to the guide's own title is dropped rather than passed
 * on: it is not a second line, it is the first one printed twice on the lock
 * screen, which is the whole interface once the screen goes dark. Forty of the
 * trip's forty-seven attraction guides are titled exactly after their
 * attraction, so the collision is the common case and not the exception
 * (D151, and D102 in the family it did not cover). It is dropped here, once,
 * because six call sites hand a subtitle in and the city guides still to come
 * would each have to remember.
 */
fun audioGuideRequest(
    content: TripContent,
    audioGuideId: String?,
    subtitle: String? = null,
): AudioGuideRequest? {
    val guide = content.audioGuide(audioGuideId) ?: return null
    val uri = content.assets.mediaUri(guide.audioAssetId)
        ?: return AudioGuideRequest.NotPackaged(guide.id, guide.title)

    return AudioGuideRequest.Playable(
        mediaId = guide.id,
        title = guide.title,
        subtitle = subtitle?.takeUnless { it.isSameLineAs(guide.title) },
        uri = uri,
        declaredDurationMs = guide.durationSeconds * 1_000L,
        chapters = guide.chapters
            .sortedBy { it.startSeconds }
            .map { PlaybackChapter(title = it.title, startMs = (it.startSeconds * 1_000L).toLong()) },
    )
}

/**
 * Whether two lines would read as the same line.
 *
 * Compared trimmed and without case: the package carries story titles that end
 * in a space, and "equal but for a space" is still one line printed twice.
 */
private fun String.isSameLineAs(other: String): Boolean =
    trim().equals(other.trim(), ignoreCase = true)
