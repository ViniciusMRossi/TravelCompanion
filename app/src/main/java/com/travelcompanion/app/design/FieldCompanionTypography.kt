package com.travelcompanion.app.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Editorial voice: city, attraction, walk and story names.
 *
 * Fraunces 600 is the approved face. Its binary is intentionally not
 * distributed in this repository (docs/technical/FONTS.md, D005), so serif
 * stands in. This is the single swap point — do not branch typography
 * elsewhere to compensate for the fallback.
 */
val FrauncesFallback: FontFamily = FontFamily.Serif

/** Operational voice: actions, metadata, timing. Android's sans is Roboto. */
val RobotoFamily: FontFamily = FontFamily.Default

/**
 * Field Companion type ramp, sized from the approved prototype.
 *
 * Anything that states a time, duration, price or locator uses [tabular] so
 * digits stop shifting between states.
 */
object TcType {
    private val tabular = "tnum"

    /** 01 — "Quem é você?" */
    val displayEditorial = TextStyle(
        fontFamily = FrauncesFallback,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
    )

    /** 02 — city name on Today. */
    val titleEditorial = TextStyle(
        fontFamily = FrauncesFallback,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    )

    /** 05 — attraction name over the hero. */
    val heroEditorial = TextStyle(
        fontFamily = FrauncesFallback,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 33.sp,
    )

    /** Uppercase operational eyebrow, e.g. "AGORA · 09:40". */
    val eyebrow = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.em,
    )

    /** Wider eyebrow used on the first-run screen. */
    val eyebrowWide = eyebrow.copy(letterSpacing = 0.12.em)

    /** The hour on the Now card. */
    val clockLarge = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 34.sp,
        fontFeatureSettings = tabular,
    )

    /** The hour on a critical item, and weather temperature. */
    val clockMedium = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontFeatureSettings = tabular,
    )

    /** Timeline hour. */
    val clockSmall = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = tabular,
    )

    /** Now card subject. */
    val headline = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    )

    /** Critical item subject. */
    val subheadline = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    )

    /** Section header, e.g. "O dia inteiro", "O que observar". */
    val sectionTitle = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    )

    /** Editorial paragraph. Never carries operational timing. */
    val editorialBody = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 27.sp,
    )

    val body = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    val bodyStrong = body.copy(fontWeight = FontWeight.Bold)

    /** Button and row labels. */
    val action = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    )

    /** Operational detail line under a timeline title. */
    val meta = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val metaStrong = meta.copy(fontWeight = FontWeight.Medium)

    /** Chip / pill text and freshness notes. */
    val label = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    /** Bottom navigation. */
    val navLabel = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )

    /** Placeholder captions describing photography still to be produced. */
    val placeholderCaption = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    )
}

/**
 * Material primitives inherit the Field Companion ramp so a stray Material
 * component never falls back to default Material styling.
 */
val FieldCompanionTypography = Typography(
    displayLarge = TcType.displayEditorial,
    displayMedium = TcType.titleEditorial,
    displaySmall = TcType.heroEditorial,
    headlineLarge = TcType.headline,
    headlineMedium = TcType.subheadline,
    headlineSmall = TcType.sectionTitle,
    titleLarge = TcType.sectionTitle,
    titleMedium = TcType.bodyStrong,
    titleSmall = TcType.metaStrong,
    bodyLarge = TcType.editorialBody,
    bodyMedium = TcType.body,
    bodySmall = TcType.meta,
    labelLarge = TcType.action,
    labelMedium = TcType.metaStrong,
    labelSmall = TcType.label,
)
