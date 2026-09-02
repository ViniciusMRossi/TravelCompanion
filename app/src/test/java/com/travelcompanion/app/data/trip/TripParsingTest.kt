package com.travelcompanion.app.data.trip

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TripParsingTest {

    @Test
    fun parsesStarterProjectionAndIgnoresFutureFields() {
        val source = """
            {
              "schemaVersion": "1.0",
              "metadata": {
                "contentStatus": "prototype",
                "generatedAt": "2026-09-01T18:00:00Z"
              },
              "trip": {
                "id": "test",
                "title": "Test Trip",
                "startDate": "2026-09-01",
                "endDate": "2026-09-02",
                "locale": "pt-BR",
                "defaultCountryCode": "BR",
                "participants": [
                  { "id": "a", "name": "A" }
                ],
                "sync": {
                  "enabled": true,
                  "groupId": "test"
                }
              },
              "cities": [],
              "days": [],
              "futureField": {
                "safeToIgnore": true
              }
            }
        """.trimIndent()

        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        val parsed = json.decodeFromString<TripPackage>(source)

        assertEquals("Test Trip", parsed.trip.title)
        assertEquals("A", parsed.trip.participants.single().name)
    }
}
