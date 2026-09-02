package com.travelcompanion.app.data.trip

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Omitted booleans must decode to the default declared in trip.schema.json.
 *
 * A Kotlin default that disagrees with the schema is invisible — the content
 * validates, the app compiles, and the behaviour is quietly wrong. The two
 * that matter most are safety defaults: an action is assumed to need the
 * network, and a story is assumed to interrupt only once per trip.
 */
class SchemaDefaultsTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun actionLinkRequiresInternetDefaultsToTrueWhenOmitted() {
        val action = json.decodeFromString<ActionLink>(
            """{ "label": "Abrir no Maps", "kind": "maps", "uri": "geo:0,0" }"""
        )

        assertTrue(
            "schema default is true; assuming offline would over-promise",
            action.requiresInternet,
        )
    }

    @Test
    fun actionLinkRequiresInternetStillHonoursAnExplicitFalse() {
        val action = json.decodeFromString<ActionLink>(
            """{ "label": "Abrir no Maps", "kind": "maps", "uri": "geo:0,0", "requiresInternet": false }"""
        )

        assertFalse(action.requiresInternet)
    }

    @Test
    fun locationTriggerNotifyOncePerTripDefaultsToTrueWhenOmitted() {
        val trigger = json.decodeFromString<LocationTrigger>(
            """{ "geo": { "latitude": 43.8, "longitude": 18.4 }, "radiusMeters": 120 }"""
        )

        assertTrue("schema default is true", trigger.notifyOncePerTrip)
        assertFalse("schema default is false", trigger.autoPlayInWalk)
    }

    @Test
    fun theRemainingSchemaDefaultsAlsoMatch() {
        val asset = json.decodeFromString<Asset>(
            """{ "id": "a", "type": "image", "path": "images/a.jpg" }"""
        )
        val document = json.decodeFromString<TripDocument>(
            """{ "id": "d", "type": "ticket", "title": "T", "assetId": "a", "availableOffline": true }"""
        )
        val walk = json.decodeFromString<Walk>(
            """
            {
              "id": "w", "cityId": "c", "title": "W",
              "distanceMeters": 100, "durationMinutes": 10,
              "stops": [ { "storyId": "s", "order": 1 } ]
            }
            """.trimIndent()
        )

        assertFalse("asset.sensitive default is false", asset.sensitive)
        assertFalse("document.sensitive default is false", document.sensitive)
        assertTrue("walk.automaticStoriesDefault default is true", walk.automaticStoriesDefault)
        assertTrue("walk.sharedAudioSupported default is true", walk.sharedAudioSupported)
        assertEquals("a walk need not name a starting attraction", null, walk.startAttractionId)
    }
}
