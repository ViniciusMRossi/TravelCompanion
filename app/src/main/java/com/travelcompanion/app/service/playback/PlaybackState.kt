package com.travelcompanion.app.service.playback

/**
 * What the audioguide is doing right now, in terms screens can render.
 *
 * This carries no Media3 types on purpose: ViewModels and Composables observe
 * playback, they never hold a player.
 */
data class PlaybackState(
    val mediaId: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val status: Status = Status.Idle,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val chapters: List<PlaybackChapter> = emptyList(),
    val failure: PlaybackFailure? = null,
) {
    enum class Status { Idle, Buffering, Playing, Paused, Ended, Failed }

    val isPlaying: Boolean get() = status == Status.Playing

    val isActive: Boolean
        get() = status != Status.Idle && status != Status.Failed

    /**
     * States that already explain themselves better than "not playing".
     * A bare pause signal must not overwrite either of them.
     */
    val isTerminal: Boolean
        get() = status == Status.Ended || status == Status.Failed

    /** 0f..1f, safe when the duration is not known yet. */
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    fun isFor(mediaId: String?): Boolean = mediaId != null && this.mediaId == mediaId

    /**
     * Index of the chapter the current position falls in, or null when the
     * guide has no chapters.
     */
    val currentChapterIndex: Int?
        get() = chapterIndexAt(positionMs)

    val currentChapter: PlaybackChapter?
        get() = currentChapterIndex?.let(chapters::get)

    /**
     * The compact player's second line.
     *
     * Follows the approved gallery example — "<context> · capítulo N" — and
     * falls back to naming the chapter when there is no other context.
     */
    fun compactSubtitle(): String? {
        val index = currentChapterIndex ?: return subtitle
        val number = index + 1
        return subtitle?.let { "$it · capítulo $number" } ?: chapters[index].label(number)
    }

    /** The chapter containing [positionMs]: the last one that has started. */
    fun chapterIndexAt(positionMs: Long): Int? {
        if (chapters.isEmpty()) return null
        val index = chapters.indexOfLast { it.startMs <= positionMs }
        return if (index < 0) 0 else index
    }
}

/**
 * A chapter of an audioguide.
 *
 * The approved player names the current chapter on its subtitle line
 * ("Capítulo 3 · A esquina de 1914"), so the number is part of the model.
 */
data class PlaybackChapter(
    val title: String,
    val startMs: Long,
) {
    fun label(number: Int): String = "Capítulo $number · $title"
}

/**
 * Why playback could not proceed.
 *
 * Kept as a small closed set so the UI can phrase each case in the traveller's
 * language instead of surfacing a decoder exception.
 */
enum class PlaybackFailure {
    /** The audio file is not in this build — a content-production gap. */
    AudioNotPackaged,

    /** The file exists but could not be read or decoded. */
    PlaybackFailed,
}
