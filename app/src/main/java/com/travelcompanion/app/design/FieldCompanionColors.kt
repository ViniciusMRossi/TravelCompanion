package com.travelcompanion.app.design

import androidx.compose.ui.graphics.Color

/**
 * Field Companion palette, taken from the approved prototype.
 *
 * The whole app uses two backgrounds: [Paper] and [Ink].
 * Semantic families (teal / oxblood / gold / moss) each carry a fill, a
 * border and a readable on-fill ink so status never depends on hue alone.
 */
object FieldCompanionColors {
    val Ink = Color(0xFF16232E)
    val Paper = Color(0xFFF5F1E8)
    val Surface = Color(0xFFFFFDF8)
    val White = Color(0xFFFFFFFF)

    val Teal = Color(0xFF1F6F78)
    val TealDark = Color(0xFF185860)
    val TealSoft = Color(0xFFE4F0F0)
    val TealBorder = Color(0xFFC8DEDE)

    /** Unfilled portion of the audioguide progress track. */
    val TealTrack = Color(0xFFC3D9D9)

    val Gold = Color(0xFFB8863B)
    val GoldSoft = Color(0xFFF3E8D0)
    val GoldBorder = Color(0xFFE4D2AD)
    val GoldInk = Color(0xFF684817)
    val GoldBody = Color(0xFF5D4B2C)

    val Oxblood = Color(0xFF7A2E2E)
    val CriticalSoft = Color(0xFFFDECEC)
    val CriticalBorder = Color(0xFFE7C9C7)
    val CriticalBody = Color(0xFF673B3A)

    val Moss = Color(0xFF4C6444)
    val MossSoft = Color(0xFFE7EDE3)
    val MossInk = Color(0xFF3D5337)

    val Neutral800 = Color(0xFF33414B)
    val Neutral700 = Color(0xFF4B5962)
    val Neutral600 = Color(0xFF65717A)
    val Neutral500 = Color(0xFF7E898F)
    val Neutral400 = Color(0xFFA4ACA9)
    val Neutral300 = Color(0xFFC8CCC5)
    val Neutral200 = Color(0xFFDDDCD4)
    val Neutral100 = Color(0xFFECE9E1)

    /** Text tiers used on top of the ink Now card. */
    val OnInkEyebrow = Color(0xFFBFD1D8)
    val OnInkBody = Color(0xFFDCE4E8)
    val OnInkMeta = Color(0xFFC6D3D8)

    /**
     * Walk Mode's ink surfaces (screen 07), where ink is the whole background
     * rather than one card. The alpha fills are the prototype's white
     * overlays: they keep their relationship to ink without introducing new
     * opaque greys into the palette.
     */
    val InkDeep = Color(0xFF101A21)
    val TealBright = Color(0xFF67B6BB)
    val OnInkAccent = Color(0xFF8DC5C8)
    val OnInkPill = Color(0x14FFFFFF)
    val OnInkCard = Color(0x12FFFFFF)
    val OnInkFill = Color(0x0FFFFFFF)
    val OnInkBorder = Color(0x40FFFFFF)
    val OnInkHairline = Color(0x1FFFFFFF)
    val OnInkTrack = Color(0x2EFFFFFF)

    /**
     * Striped placeholder used wherever trip photography is not packaged yet.
     * Replacing the photo must not change the layout around it.
     */
    val PlaceholderPaperA = Color(0xFFD7CFBC)
    val PlaceholderPaperB = Color(0xFFCDC4AE)
    val PlaceholderCoolA = Color(0xFF8FA3A0)
    val PlaceholderCoolB = Color(0xFF7F948F)
    val PlaceholderCaptionScrim = Color(0x6B16232E)
    val PlaceholderCaptionInk = Color(0xFFF2EFE7)

    /**
     * The veil under a hero's photograph, and only under a photograph.
     *
     * The hero's title is white and sits at the bottom of the frame. Against
     * the striped placeholder it always had contrast, because the stripes
     * supply their own; against a photograph it can land on pale limestone or
     * a white hull and stop being readable, which is what the 152 photographs
     * of D166 exposed on screens 05 and 16.
     *
     * Both stops are [Ink], the same ink as [PlaceholderCaptionScrim], so the
     * veil is the app's own dark rather than a grey of its own. It runs
     * transparent to opaque *downwards* because the title is bottom-anchored:
     * the top of the photograph is what the traveller is looking at, and it is
     * left alone.
     */
    val HeroScrimTop = Color(0x0016232E)
    val HeroScrimBottom = Color(0xB316232E)
}
