package com.travelcompanion.app.feature.shell

import com.travelcompanion.app.data.trip.PackagedTripTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The rule D065 stated and the code stopped keeping: a timeline row that names
 * a screen opens it.
 *
 * The packaged case is the one that matters. Screen 02 is where the traveller
 * stands during the day, and day 9's 11:00 line names a walk the package
 * carries — it rendered with its hour and its marker and answered nothing,
 * because "walk" was missing both from the kinds screen 02 lets you press and
 * from the `when` behind the press. A test over the real file is the only one
 * that can fail for that reason; a fixture would have been written with the
 * branch in mind.
 */
class TimelineDestinationTest {

    @Test
    fun theWalkOnTheTimelineOpensItsScreen() {
        assertEquals(
            "walk/walk.sarajevo.historical",
            timelineDestination("walk", "walk.sarajevo.historical"),
        )
    }

    @Test
    fun transportAndAccommodationKeepTheirScreens() {
        assertEquals("transport/t1", timelineDestination("transport", "t1"))
        assertEquals("stay/s1", timelineDestination("accommodation", "s1"))
    }

    @Test
    fun aRowWithNothingToOpenIsNotADoor() {
        // No reference, so there is nothing to navigate to even for a kind
        // that has a screen; and a kind with no screen never opens one.
        assertNull(timelineDestination("walk", null))
        assertNull(timelineDestination("transport", null))
        assertNull(timelineDestination("attraction", "bascarsija"))
        assertNull(timelineDestination("custom", "whatever"))
    }

    @Test
    fun thePackagedElevenOClockLineOpensTheWalkScreen() {
        val content = PackagedTripTest.packagedContent()

        val day = content.days.single { it.id == "day09" }
        val item = day.timeline.single { it.startTime == "11:00" }

        assertEquals("walk", item.kind)
        assertEquals(
            "the packaged 11:00 line must open screen 06",
            "walk/walk.sarajevo.historical",
            timelineDestination(item.kind, item.refId),
        )
    }

    @Test
    fun everyPackagedRowThatOpensSomethingNamesAContentIdThatExists() {
        // The destination is built from `refId` verbatim, so a row that opens a
        // route naming content the package does not carry would land on an
        // empty screen rather than fail here.
        val content = PackagedTripTest.packagedContent()

        content.days.forEach { day ->
            day.timeline.forEach { item ->
                val destination = timelineDestination(item.kind, item.refId) ?: return@forEach
                val refId = item.refId!!
                val target: Any? = when (item.kind) {
                    "walk" -> content.walk(refId)
                    "transport" -> content.transport(refId)
                    "accommodation" -> content.accommodation(refId)
                    else -> null
                }
                assertEquals(
                    "$destination must name content that exists",
                    true,
                    target != null,
                )
            }
        }
    }
}
