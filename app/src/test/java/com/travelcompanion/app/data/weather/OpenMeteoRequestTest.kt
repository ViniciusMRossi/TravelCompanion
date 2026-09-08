package com.travelcompanion.app.data.weather

import com.travelcompanion.app.data.trip.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The request that is actually sent, asserted without sending one.
 *
 * The provider was chosen for one property above all others — **no key and no
 * registration** — so there is nothing secret in this string, and every part of
 * it can be written down and checked. That is also why this test can exist at
 * all: it is the shape of the URL that is being guarded, never a network.
 */
class OpenMeteoRequestTest {

    @Test
    fun `the request names the point, the day's zone and one day`() {
        val url = openMeteoUrl(GeoPoint(42.64, 18.1109), "Europe/Zagreb")

        assertEquals(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=42.64&longitude=18.1109" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum" +
                "&timezone=Europe%2FZagreb" +
                "&forecast_days=1",
            url,
        )
    }

    /** No key, and nothing that could become one. */
    @Test
    fun `the request carries no credential`() {
        val url = openMeteoUrl(GeoPoint(43.8595, 18.431), "Europe/Sarajevo")

        listOf("apikey", "api_key", "key=", "token", "&appid").forEach { secret ->
            assertTrue("the URL must not carry $secret", !url.lowercase().contains(secret))
        }
    }
}
