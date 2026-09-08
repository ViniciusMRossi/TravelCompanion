package com.travelcompanion.app.feature.today

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.domain.today.TodayUiState
import com.travelcompanion.app.domain.today.TodayUseCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * The line that says the deadlines are approximate, in both of its states.
 *
 * From Android 13 `SCHEDULE_EXACT_ALARM` starts denied, and the app asks for
 * it once and never again (D093). When the answer is no, every packaged
 * deadline is registered with `setAndAllowWhileIdle` and the system may hold
 * it until its next maintenance window — five of the trip's limits are before
 * 07:00, and the earliest is a bag drop at 04:45. Until D181 nothing on any
 * screen said so; the only witness was a line in logcat.
 *
 * Composed rather than asserted on the state, for the same reason
 * `TodayWeatherCardTest` is: the data class is identical either way, and the
 * property that matters is whether the sentence is on the screen the
 * deadlines live on. The second half is the one that keeps this honest — a
 * warning that cannot go away is a warning nobody reads.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class TodayApproximateAlertsTest {

    @get:Rule
    val compose = createComposeRule()

    private val content = packagedContent()
    private val date: LocalDate = LocalDate.parse(content.days.first().date)

    private fun todayState(): TodayUiState =
        TodayUseCase(content)("vinicius", date, LocalTime.of(12, 0))!!

    private var opened = 0

    private fun screen(state: TodayUiState) {
        compose.setContent {
            TodayScreen(
                state = state,
                onOpenAttraction = {},
                onOpenMaps = {},
                onOpenTimelineItem = {},
                onOpenFullDay = {},
                onOpenShortcut = {},
                onOpenAlarmSettings = { opened++ },
            )
        }
    }

    private fun seeing(text: String) = compose.onAllNodesWithText(text, substring = true)
        .onFirst().performScrollTo().assertIsDisplayed()

    private fun notSeeing(text: String) =
        compose.onAllNodesWithText(text, substring = true).assertCountEquals(0)

    /** Denied: the line is on screen, and it says what to do about it. */
    @Test
    fun `when the alarm cannot be exact the screen says so`() {
        screen(todayState().copy(alertsAreApproximate = true))

        seeing("AVISOS APROXIMADOS")
        seeing("O sistema pode atrasar os avisos")
        seeing("Alarmes e lembretes")
    }

    /**
     * Granted: **no node at all** in that place.
     *
     * Not a hidden node, not an empty card holding its height — the row is
     * simply not composed, so screen 02 in the good state is byte for byte
     * the screen that shipped before D181.
     */
    @Test
    fun `when the alarm can be exact there is nothing in its place`() {
        screen(todayState().copy(alertsAreApproximate = false))

        notSeeing("AVISOS APROXIMADOS")
        notSeeing("O sistema pode atrasar os avisos")
        notSeeing("Alarmes e lembretes")
    }

    /**
     * Tapping opens Android's own screen, and that is not a second ask.
     *
     * D093's rule is that the *app* never opens the settings screen on its own
     * more than once. A row the traveller chooses to touch is the traveller
     * asking, which is the opposite of the app insisting.
     */
    @Test
    fun `the line is the traveller's way back to the settings screen`() {
        screen(todayState().copy(alertsAreApproximate = true))

        compose.onAllNodesWithText("AVISOS APROXIMADOS", substring = true)
            .onFirst().performScrollTo().performClick()

        assertEquals("one tap, one settings screen", 1, opened)
    }

    /** The default is the good state: nothing is claimed without being read. */
    @Test
    fun `a state built by the use case alone never claims the alarms are degraded`() {
        assertEquals(false, todayState().alertsAreApproximate)
    }
}
