package com.travelcompanion.app.feature.shell

import com.travelcompanion.app.service.playback.PlaybackState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When leaving a screen ends the audioguide, and when it must not.
 *
 * `PlaybackController.stop` existed and had no callers, so the compact bar had
 * no ending: it appears whenever playback `isActive`, and paused counts as
 * active. After the first audioguide of the app's life the bar stood on every
 * screen for ever, over a guide nobody was going to resume.
 *
 * The rule lives in the shell because the shell is what watches the route
 * change, and it is asserted here as a predicate for the same reason the
 * decision is one: three answers on one line, none of them requiring a
 * NavHost, nineteen screens and a trip package to reach.
 */
class CompactPlayerLifetimeTest {

    private fun playback(status: PlaybackState.Status) = PlaybackState(
        mediaId = "ag.bascarsija",
        title = "Audioguia de Baščaršija",
        status = status,
    )

    @Test
    fun `paused audio does not follow the traveller to the next screen`() {
        assertTrue(
            endsOnRouteChange(playback(PlaybackState.Status.Paused), walkIsRunning = false),
        )
    }

    /**
     * The half that keeps the fix from becoming a defect. The bar exists so a
     * guide survives navigation — headphones in, phone in a pocket — and
     * stopping playing audio because a screen changed would remove the only
     * reason the bar was drawn.
     */
    @Test
    fun `audio that is playing survives the screen it was started on`() {
        assertFalse(
            endsOnRouteChange(playback(PlaybackState.Status.Playing), walkIsRunning = false),
        )
        assertFalse(
            endsOnRouteChange(playback(PlaybackState.Status.Buffering), walkIsRunning = false),
        )
    }

    /**
     * And the third: during a walk neither half applies. Screen 07 hides the
     * bar and carries its own transport, and pausing a story to cross a street
     * and coming back must not end the walk's audio.
     */
    @Test
    fun `a running walk is not touched by this rule at all`() {
        assertFalse(
            endsOnRouteChange(playback(PlaybackState.Status.Paused), walkIsRunning = true),
        )
        assertFalse(
            endsOnRouteChange(playback(PlaybackState.Status.Playing), walkIsRunning = true),
        )
    }

    @Test
    fun `with nothing loaded there is nothing to end`() {
        assertFalse(endsOnRouteChange(PlaybackState(), walkIsRunning = false))
    }
}
