package com.travelcompanion.app.feature.attraction

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.service.playback.FakeAudioEngine
import com.travelcompanion.app.service.playback.InMemoryPlaybackPositionStore
import com.travelcompanion.app.service.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The two lines of the lock screen, on a shape built by hand.
 *
 * [AttractionLockScreenPackagedTest] asserts the same rule against the real
 * package, which is not in the repository. This one builds the collision that
 * package happens to contain 40 times, so the guarantee is checked on every
 * machine and not only on the one the trip is packaged on.
 */
class AttractionLockScreenTest {

    /** The packaged sample, with Kotor's names on it. */
    private fun kotor(guideTitle: String, attractionName: String = "Muralhas de Kotor"): TripContent {
        val packaged = packagedContent(exists = { true })
        val trip = packaged.trip
        val attraction = trip.attractions.single { it.id == "bascarsija" }
        val guide = trip.audioGuides.single { it.id == attraction.audioGuideId }
        return TripContent(
            trip.copy(
                cities = trip.cities.map {
                    if (it.id == attraction.cityId) {
                        it.copy(name = "Kotor", countryName = "Montenegro")
                    } else {
                        it
                    }
                },
                attractions = trip.attractions.map {
                    if (it.id == attraction.id) it.copy(name = attractionName) else it
                },
                audioGuides = trip.audioGuides.map {
                    if (it.id == guide.id) it.copy(title = guideTitle) else it
                },
            ),
            packaged.assets,
        )
    }

    private fun lockScreenOf(content: TripContent): Pair<String?, String?> {
        val engine = FakeAudioEngine()
        val controller = PlaybackController(
            engine = engine,
            positions = InMemoryPlaybackPositionStore(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        AttractionViewModel(
            content = content,
            attractionId = "bascarsija",
            playback = controller,
            today = LocalDate.parse(content.days.first().date),
        ).onPlayAudioGuide()
        return engine.preparedTitle to engine.preparedSubtitle
    }

    @Test
    fun `a guide named after its attraction still gets two different lines`() {
        assertEquals(
            "Muralhas de Kotor" to "Kotor · Montenegro",
            lockScreenOf(kotor(guideTitle = "Muralhas de Kotor")),
        )
    }

    @Test
    fun `a guide named differently gets the same second line`() {
        // One rule for all 47: the seven whose titles already differed do not
        // get a branch of their own. A conditional whose arm depends on two
        // strings happening to be equal is the shape D089 warned about.
        assertEquals(
            "Caverna Betina" to "Kotor · Montenegro",
            lockScreenOf(kotor(guideTitle = "Caverna Betina")),
        )
    }
}
