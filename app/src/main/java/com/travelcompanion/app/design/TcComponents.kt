package com.travelcompanion.app.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/* ---------------------------------------------------------------- geometry */

/** 14dp — cards and buttons. */
val TcCardShape = RoundedCornerShape(14.dp)

/** 20dp — heroes and editorial blocks. */
val TcHeroShape = RoundedCornerShape(20.dp)

val TcButtonShape = RoundedCornerShape(14.dp)

/** 999 — chips and avatars. */
val TcPillShape = CircleShape

/* ------------------------------------------------------------------- chips */

enum class TcChipTone { Teal, Neutral, Gold, Moss, Critical, OnHero }

private fun TcChipTone.fill(): Color = when (this) {
    TcChipTone.Teal -> FieldCompanionColors.TealSoft
    TcChipTone.Neutral -> FieldCompanionColors.Neutral100
    TcChipTone.Gold -> FieldCompanionColors.GoldSoft
    TcChipTone.Moss -> FieldCompanionColors.MossSoft
    TcChipTone.Critical -> FieldCompanionColors.CriticalSoft
    TcChipTone.OnHero -> Color.Transparent
}

private fun TcChipTone.ink(): Color = when (this) {
    TcChipTone.Teal -> FieldCompanionColors.TealDark
    TcChipTone.Neutral -> FieldCompanionColors.Neutral700
    TcChipTone.Gold -> FieldCompanionColors.GoldInk
    TcChipTone.Moss -> FieldCompanionColors.MossInk
    TcChipTone.Critical -> FieldCompanionColors.Oxblood
    TcChipTone.OnHero -> FieldCompanionColors.White
}

/**
 * Status pill. Booking status and criticality are separate dimensions, so a
 * card may legitimately carry two of these side by side.
 */
@Composable
fun TcChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: TcChipTone = TcChipTone.Neutral,
    icon: ImageVector? = null,
    border: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(TcPillShape)
            .background(tone.fill())
            .then(
                if (border) {
                    Modifier.border(1.dp, FieldCompanionColors.Neutral200, TcPillShape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 11.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tone.ink(), modifier = Modifier.size(14.dp))
        }
        // A chip is a qualifier: when the line it sits on runs out of room it
        // gives way to whatever names the thing, rather than pushing that off
        // the screen (D055). Its icon is measured first and stays, so the chip
        // never becomes a bare word with no sign of what it is about.
        Text(
            text,
            style = TcType.label,
            color = tone.ink(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * "Offline · conteúdo disponível".
 *
 * Offline is a normal operating mode: this is a neutral pill, never an alarm
 * banner, and it always says what still works.
 */
@Composable
fun TcOfflineChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    TcChip(text = text, modifier = modifier, tone = TcChipTone.Neutral, icon = TcIcons.Offline, border = true)
}

/**
 * The dot's own size, named because a row that must never lose it has to be
 * able to say how small "never lose it" is (D055, F3).
 */
val TcStateDotSize: androidx.compose.ui.unit.Dp = 8.dp

/**
 * A state dot plus its word. State is never carried by colour alone.
 */
@Composable
fun TcStateDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = TcStateDotSize,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(TcPillShape)
            .background(color),
    )
}

/* ----------------------------------------------------------------- buttons */

/** 52dp teal action. The primary travel action on a paper surface. */
@Composable
fun TcPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    minHeight: androidx.compose.ui.unit.Dp = 52.dp,
    content: @Composable RowScope.() -> Unit,
) {
    TcButtonSurface(
        onClick = onClick,
        modifier = modifier,
        minHeight = minHeight,
        background = FieldCompanionColors.Teal,
        contentColor = FieldCompanionColors.White,
        border = null,
        icon = icon,
        content = content,
    )
}

/** 52dp outlined action on a paper surface. */
@Composable
fun TcSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    minHeight: androidx.compose.ui.unit.Dp = 52.dp,
    content: @Composable RowScope.() -> Unit,
) {
    TcButtonSurface(
        onClick = onClick,
        modifier = modifier,
        minHeight = minHeight,
        background = FieldCompanionColors.Surface,
        contentColor = FieldCompanionColors.Ink,
        border = BorderStroke(1.dp, FieldCompanionColors.Neutral300),
        icon = icon,
        content = content,
    )
}

/** Soft teal action, e.g. "Ouvir audioguia · 12 min". */
@Composable
fun TcAudioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = TcIcons.Play,
    content: @Composable RowScope.() -> Unit,
) {
    TcButtonSurface(
        onClick = onClick,
        modifier = modifier,
        minHeight = 52.dp,
        background = FieldCompanionColors.TealSoft,
        contentColor = FieldCompanionColors.TealDark,
        border = null,
        icon = icon,
        content = content,
    )
}

/** 44dp white action used inside the ink Now card. */
@Composable
fun TcOnInkPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    TcButtonSurface(
        onClick = onClick,
        modifier = modifier,
        minHeight = 44.dp,
        background = FieldCompanionColors.White,
        contentColor = FieldCompanionColors.Ink,
        border = null,
        icon = null,
        content = content,
    )
}

/** 44dp translucent action used inside the ink Now card. */
@Composable
fun TcOnInkSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    TcButtonSurface(
        onClick = onClick,
        modifier = modifier,
        minHeight = 44.dp,
        background = Color.White.copy(alpha = 0.06f),
        contentColor = FieldCompanionColors.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        icon = null,
        content = content,
    )
}

@Composable
private fun TcButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    minHeight: androidx.compose.ui.unit.Dp,
    background: Color,
    contentColor: Color,
    border: BorderStroke?,
    icon: ImageVector?,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .heightIn(min = minHeight)
            .clip(TcButtonShape)
            .background(background)
            .then(if (border != null) Modifier.border(border, TcButtonShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        }
        TcProvideActionStyle(contentColor) { content() }
    }
}

@Composable
private fun TcProvideActionStyle(color: Color, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides color,
        androidx.compose.material3.LocalTextStyle provides TcType.action.copy(color = color),
    ) { content() }
}

/* ------------------------------------------------------------------- cards */

/**
 * Paper card: surface fill, hairline border, 14dp radius.
 *
 * Cards are never nested — sibling blocks are separated by gap, never by a
 * frame inside a frame.
 */
@Composable
fun TcCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = TcCardShape,
    fill: Color = FieldCompanionColors.Surface,
    borderColor: Color = FieldCompanionColors.Neutral200,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, borderColor, shape)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * The single dark block on Today. The teal bloom in the top-right corner is
 * part of the approved composition.
 */
@Composable
fun TcInkCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(22.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(TcHeroShape)
            .background(FieldCompanionColors.Ink),
    ) {
        // Decorative bloom, clipped by the card: right:-60px / top:-80px.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-80).dp)
                .size(170.dp)
                .clip(TcPillShape)
                .background(FieldCompanionColors.Teal.copy(alpha = 0.45f)),
        )
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Gold recovery block. Calm contingency, never emergency styling. */
@Composable
fun TcPlanBCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    eyebrow: String = "Plano B",
) {
    TcCard(
        modifier = modifier,
        fill = FieldCompanionColors.GoldSoft,
        borderColor = FieldCompanionColors.GoldBorder,
    ) {
        Text(eyebrow.uppercase(), style = TcType.eyebrow, color = FieldCompanionColors.GoldInk)
        Spacer(Modifier.height(6.dp))
        Text(title, style = TcType.bodyStrong, color = FieldCompanionColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(body, style = TcType.meta, color = FieldCompanionColors.GoldBody)
    }
}

/* ---------------------------------------------------------------- sections */

@Composable
fun TcSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = TcType.sectionTitle, color = FieldCompanionColors.Ink)
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = TcType.metaStrong,
                color = FieldCompanionColors.Teal,
                modifier = Modifier
                    .clip(TcPillShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * 56dp shortcut row: icon, label, optional trailing note, chevron.
 */
@Composable
fun TcShortcutRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingNote: String? = null,
    iconTint: Color = FieldCompanionColors.Teal,
    fill: Color = FieldCompanionColors.Surface,
    borderColor: Color = FieldCompanionColors.Neutral200,
    chevronTint: Color = FieldCompanionColors.Neutral500,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(TcCardShape)
            .background(fill)
            .border(1.dp, borderColor, TcCardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            style = TcType.action,
            color = FieldCompanionColors.Ink,
            modifier = Modifier.weight(1f),
        )
        if (trailingNote != null) {
            Text(trailingNote, style = TcType.label, color = FieldCompanionColors.Neutral600)
        }
        Icon(
            TcIcons.ChevronRight,
            contentDescription = null,
            tint = chevronTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Gold-numbered observation list — short lines, made for looking, not reading. */
@Composable
fun TcNumberedNotes(
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, item ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = (index + 1).toString().padStart(2, '0'),
                    style = TcType.bodyStrong,
                    color = FieldCompanionColors.Gold,
                )
                Text(item, style = TcType.body, color = FieldCompanionColors.Neutral800)
            }
        }
    }
}

/* ------------------------------------------------------------------ heroes */

/**
 * The hero's backdrop layer — the photograph, or the striped placeholder.
 *
 * Named so a test can assert the one thing about it that nothing else can see:
 * that it covers the hero rather than measuring to nothing (D056).
 */
const val TcHeroBackdropTag: String = "tc-hero-backdrop"

/**
 * Editorial hero.
 *
 * Renders packaged photography when the binary is present, and otherwise the
 * approved striped placeholder with a mono caption naming the expected image.
 * Swapping in the real photo must not change the surrounding layout.
 */
@Composable
fun TcHero(
    modifier: Modifier = Modifier,
    imageAssetPath: String? = null,
    placeholderCaption: String? = null,
    cool: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    TcHeroWith(
        modifier = modifier,
        photograph = rememberPackagedImage(imageAssetPath),
        placeholderCaption = placeholderCaption,
        cool = cool,
        content = content,
    )
}

/**
 * The hero's layering, with the photograph already resolved.
 *
 * Separate for one reason: no photograph ships in this build, so the branch
 * that draws one cannot be measured through [TcHero], and it is the branch that
 * becomes the live one the day photography arrives. `internal`, because it is
 * the seam a test needs and nothing else (D056).
 */
@Composable
internal fun TcHeroWith(
    modifier: Modifier = Modifier,
    photograph: ImageBitmap? = null,
    placeholderCaption: String? = null,
    cool: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier) {
        val bitmap = photograph
        // `matchParentSize`, not `fillMaxSize`: the photograph and the
        // placeholder behind it must cover whatever the hero turns out to be,
        // without being the thing that decides it. A hero whose height comes
        // from its own content — screen 05's `heightIn(min = 200.dp)` — sits
        // inside a scrolling column, where the incoming maximum height is
        // infinite and `fillMaxSize` quietly resolves to nothing. Screen 05's
        // stripes were therefore never drawn at all: the card's surface showed
        // through and the white title on top of it was invisible (D051).
        //
        // Both layers carry [TcHeroBackdropTag] so that invariant is something
        // a test can ask about, rather than something only a person looking at
        // the screen can notice (D056).
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().testTag(TcHeroBackdropTag),
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .testTag(TcHeroBackdropTag)
                    .background(stripes(cool)),
            )
            if (placeholderCaption != null) {
                Text(
                    text = placeholderCaption,
                    style = TcType.placeholderCaption,
                    color = FieldCompanionColors.PlaceholderCaptionInk,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(FieldCompanionColors.PlaceholderCaptionScrim)
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                )
            }
        }
        content()
    }
}

/**
 * 115° diagonal stripes in 12px bands, matching the approved placeholder.
 *
 * CSS measures the angle clockwise from "to top", so the repeat vector is
 * (sin 115°, -cos 115°) scaled to the 24px period.
 */
private fun stripes(cool: Boolean): Brush {
    val a = if (cool) FieldCompanionColors.PlaceholderCoolA else FieldCompanionColors.PlaceholderPaperA
    val b = if (cool) FieldCompanionColors.PlaceholderCoolB else FieldCompanionColors.PlaceholderPaperB
    return Brush.linearGradient(
        colorStops = arrayOf(0f to a, 0.4999f to a, 0.5f to b, 1f to b),
        start = Offset.Zero,
        end = Offset(STRIPE_PERIOD_PX * 0.9063f, STRIPE_PERIOD_PX * 0.4226f),
        tileMode = TileMode.Repeated,
    )
}

private const val STRIPE_PERIOD_PX = 24f

/** Circular participant avatar showing the initial. */
@Composable
fun TcAvatar(
    initial: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    fill: Color = FieldCompanionColors.Ink,
    contentColor: Color = FieldCompanionColors.Surface,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(TcPillShape)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = TcType.action.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** Fixed bottom action bar used by immersive detail screens. */
@Composable
fun TcBottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FieldCompanionColors.Surface)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
fun TcHairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(FieldCompanionColors.Neutral200),
    )
}

@Composable
fun TcVerticalSpacer(height: androidx.compose.ui.unit.Dp) {
    Spacer(Modifier.height(height))
}

@Composable
fun TcHorizontalSpacer(width: androidx.compose.ui.unit.Dp) {
    Spacer(Modifier.width(width))
}
