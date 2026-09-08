package com.travelcompanion.app.feature.food

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The price pill's corner, once the price is allowed to wrap.
 *
 * `TcPillShape` is `CircleShape`, whose radius is half the *smaller* side. That
 * is the stadium the plate draws while the price is one line — 18sp of line
 * plus 6dp above and below, so 30dp tall and a 15dp radius. Once the ceiling of
 * D167 let a sixty-five-character price wrap to four lines, the same expression
 * produced a radius over 50dp on a box over 100dp tall, the corner cut inwards
 * further than the 11dp of horizontal padding, and the first glyphs of the top
 * and bottom lines were drawn outside the fill. The telephone found it; no unit
 * test could have, because nothing about it is a position or a size.
 *
 * What *is* checkable is the arithmetic, and it has two halves: below the cap
 * the value must equal what `CircleShape` computes, to the float, so no pill
 * that fits on a line moves; above it the value must stop growing.
 */
class DishPriceShapeTest {

    private val density = Density(density = 3f, fontScale = 1f)
    private val corner = CappedCornerSize(24.dp)

    private fun circle(size: Size) = CircleShape.topStart.toPx(size, density)

    /** A one-line pill: 30dp tall at this density. Nothing may change here. */
    @Test
    fun `below the cap it is exactly what CircleShape computes`() {
        val oneLine = Size(width = 150f * 3, height = 30f * 3)

        assertEquals(
            "a pill that fits on a line must keep the plate's stadium",
            circle(oneLine),
            corner.toPx(oneLine, density),
            0f,
        )
        assertEquals(15f * 3, corner.toPx(oneLine, density), 0.01f)
    }

    /** And at a large font scale, where the pill grows but is still one line. */
    @Test
    fun `a taller one-line pill is still exactly CircleShape`() {
        val enlarged = Size(width = 150f * 3, height = 46f * 3)

        assertEquals(circle(enlarged), corner.toPx(enlarged, density), 0f)
    }

    /**
     * The wrapped price: `CircleShape` would round it by more than the padding
     * that keeps the text inside.
     */
    @Test
    fun `above the cap it stops curving`() {
        val wrapped = Size(width = 163.5f * 3, height = 104f * 3)

        val capped = corner.toPx(wrapped, density)
        assertEquals("the cap is the whole of what is above it", 24f * 3, capped, 0.01f)
        assertTrue(
            "and it must be well under what CircleShape wanted, which was " +
                "${circle(wrapped) / 3}dp",
            capped < circle(wrapped),
        )
        assertTrue(
            "the corner must not cut in past the 11dp of horizontal padding on " +
                "the first line of text",
            insetAtFirstLine(radiusPx = capped, density = density) < 11f * 3,
        )
    }

    /**
     * How far the rounded corner eats into the box at the vertical middle of
     * the first line of text — 9dp down, being 6dp of padding and half of an
     * 18sp line.
     *
     * This is the number that actually broke: at a 50dp radius it came to about
     * 14dp, and the text starts at 11dp.
     */
    private fun insetAtFirstLine(radiusPx: Float, density: Density): Float {
        val y = with(density) { 9.dp.toPx() }
        if (y >= radiusPx) return 0f
        val dy = radiusPx - y
        return radiusPx - kotlin.math.sqrt(radiusPx * radiusPx - dy * dy)
    }

    /** The same measurement on the shape that was there, to name what was wrong. */
    @Test
    fun `CircleShape on a wrapped price cut in past the padding`() {
        val wrapped = Size(width = 163.5f * 3, height = 104f * 3)

        assertTrue(
            "this is the defect the telephone showed, kept here so the cap has " +
                "something to be measured against",
            insetAtFirstLine(circle(wrapped), density) > 11f * 3,
        )
    }
}
