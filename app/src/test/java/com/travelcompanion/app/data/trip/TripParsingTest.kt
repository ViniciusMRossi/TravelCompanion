package com.travelcompanion.app.data.trip

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun parsesStarterProjectionAndIgnoresFutureFields() {
        val source = """
            {
              "schemaVersion": "1.1",
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
              "cities": [
                {
                  "id": "c",
                  "name": "C",
                  "countryCode": "BA",
                  "countryName": "Country",
                  "timeZone": "Europe/Sarajevo",
                  "intro": "Intro"
                }
              ],
              "days": [
                {
                  "id": "d1",
                  "date": "2026-09-01",
                  "dayNumber": 1,
                  "timeZone": "Europe/Sarajevo",
                  "cityIds": [ "c" ],
                  "timeline": [
                    {
                      "id": "d1.morning",
                      "startTime": "09:00",
                      "kind": "custom",
                      "title": "Inherits the day"
                    },
                    {
                      "id": "d1.nightbus",
                      "startTime": "23:30",
                      "kind": "transport",
                      "title": "Crosses a border",
                      "timeZone": "Europe/Belgrade"
                    }
                  ]
                }
              ],
              "futureField": {
                "safeToIgnore": true
              }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<TripPackage>(source)

        assertEquals("1.1", parsed.schemaVersion)
        assertEquals("Test Trip", parsed.trip.title)
        assertEquals("A", parsed.trip.participants.single().name)
    }

    /**
     * Schema 1.1 makes the zone explicit on a city and on a day, and optional on
     * a timeline item. The optional one has to stay null when it is omitted: an
     * item that silently invented its own zone would move a departure time.
     */
    @Test
    fun schema11TimeZonesAreReadAndOnlyOverriddenWhenDeclared() {
        val source = """
            {
              "schemaVersion": "1.1",
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
                "defaultCountryCode": "BA",
                "participants": [ { "id": "a", "name": "A" } ],
                "sync": { "enabled": true, "groupId": "test" }
              },
              "cities": [
                {
                  "id": "sarajevo",
                  "name": "Sarajevo",
                  "countryCode": "BA",
                  "countryName": "Bósnia e Herzegovina",
                  "timeZone": "Europe/Sarajevo",
                  "intro": "Intro"
                }
              ],
              "transports": [
                {
                  "id": "t.night",
                  "type": "bus",
                  "origin": {
                    "name": "Sarajevo",
                    "dateTime": "2026-09-01T23:30",
                    "timeZone": "Europe/Sarajevo"
                  },
                  "destination": {
                    "name": "Beograd",
                    "dateTime": "2026-09-02T07:10",
                    "timeZone": "Europe/Belgrade"
                  }
                }
              ],
              "days": [
                {
                  "id": "d1",
                  "date": "2026-09-01",
                  "dayNumber": 1,
                  "timeZone": "Europe/Sarajevo",
                  "cityIds": [ "sarajevo" ],
                  "timeline": [
                    {
                      "id": "d1.morning",
                      "startTime": "09:00",
                      "kind": "custom",
                      "title": "Inherits the day"
                    },
                    {
                      "id": "d1.nightbus",
                      "startTime": "23:30",
                      "kind": "transport",
                      "title": "Crosses a border",
                      "refId": "t.night",
                      "timeZone": "Europe/Belgrade"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<TripPackage>(source)
        val day = parsed.days.single()
        val transport = parsed.transports.single()

        assertEquals("Europe/Sarajevo", parsed.cities.single().timeZone)
        assertEquals("Europe/Sarajevo", day.timeZone)

        // An endpoint carries its own zone, because one leg can cross two.
        assertEquals("Europe/Sarajevo", transport.origin.timeZone)
        assertEquals("Europe/Belgrade", transport.destination.timeZone)

        assertNull(
            "an item without an override must inherit the day, not guess",
            day.timeline.single { it.id == "d1.morning" }.timeZone,
        )
        assertEquals(
            "Europe/Belgrade",
            day.timeline.single { it.id == "d1.nightbus" }.timeZone,
        )
    }
}
