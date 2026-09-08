package com.travelcompanion.app.feature.attraction

import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripPackage
import com.travelcompanion.app.service.playback.FakeAudioEngine
import com.travelcompanion.app.service.playback.InMemoryPlaybackPositionStore
import com.travelcompanion.app.service.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * What the lock screen prints for every attraction of the real trip.
 *
 * Walk Mode's premise is headphones and a dark screen, so the two lines of the
 * media notification are the whole interface. In 40 of the 47 attractions that
 * carry a guide, `audioGuide.title` *is* `attraction.name` word for word — so
 * a subtitle taken from the attraction's name printed the same line twice
 * (D151). This is D102 in the family it did not cover.
 *
 * Like [com.travelcompanion.app.domain.explore.ExplorePackagedDaysTest], this
 * reads `assets/trip-production/`, which is not in the repository (D027,
 * D028), and asserts nothing when the package is absent.
 * [AttractionLockScreenTest] builds the colliding shape by hand and is the
 * test that must always run.
 */
class AttractionLockScreenPackagedTest {

    private val file = File("src/main/assets/trip-production/trip.json")

    private fun packaged(): TripContent? {
        if (!file.exists()) return null
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val trip = json.decodeFromString<TripPackage>(file.readText())
        // Every binary present: the question is what the notification says,
        // not whether the file shipped.
        return TripContent(trip, AssetResolver(trip.assets, exists = { true }))
    }

    /** Title and subtitle handed to the engine for one attraction. */
    private fun lockScreenOf(content: TripContent, attractionId: String): Pair<String?, String?> {
        val engine = FakeAudioEngine()
        val controller = PlaybackController(
            engine = engine,
            positions = InMemoryPlaybackPositionStore(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        AttractionViewModel(
            content = content,
            attractionId = attractionId,
            playback = controller,
            today = LocalDate.parse(content.days.first().date),
        ).onPlayAudioGuide()
        return engine.preparedTitle to engine.preparedSubtitle
    }

    @Test
    fun `no attraction of the real trip prints the same line twice`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val withGuide = content.trip.attractions.filter { it.audioGuideId != null }
        assertEquals("the trip's attractions that carry a guide", 47, withGuide.size)

        val repeated = withGuide.mapNotNull { attraction ->
            val (title, subtitle) = lockScreenOf(content, attraction.id)
            val same = title != null &&
                subtitle != null &&
                title.trim().equals(subtitle.trim(), ignoreCase = true)
            if (same) "${attraction.id}: $title / $subtitle" else null
        }

        assertEquals(
            "attractions whose lock screen prints one line twice:\n" +
                repeated.joinToString("\n"),
            emptyList<String>(),
            repeated,
        )
    }

    @Test
    fun `the second line names the city and the country`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        // One of the seven whose guide title already differed from the
        // attraction's name: the rule is the same one for all 47, so it loses
        // the old pair and gains the city line like everybody else.
        val walls = content.trip.attractions.single { it.audioGuideId == "ag.kotor.muralhas" }
        assertEquals(
            "Muralhas de Kotor" to "Kotor · Montenegro",
            lockScreenOf(content, walls.id),
        )
    }

    @Test
    fun `every attraction with a guide gets a second line`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        // Discarding a repeated subtitle on its own would leave 40 of these
        // blank, and blank is not better than repeated — it is only less
        // wrong. The city line is what the lock screen was missing.
        val blank = content.trip.attractions
            .filter { it.audioGuideId != null }
            .filter { lockScreenOf(content, it.id).second.isNullOrBlank() }
            .map { it.id }

        assertTrue("attractions left without a second line: $blank", blank.isEmpty())
    }
}
