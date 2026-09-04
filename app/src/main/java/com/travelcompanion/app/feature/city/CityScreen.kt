package com.travelcompanion.app.feature.city

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcChip
import com.travelcompanion.app.design.TcChipTone
import com.travelcompanion.app.design.TcHero
import com.travelcompanion.app.design.TcHeroShape
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcInkCard
import com.travelcompanion.app.design.TcOnInkSecondaryButton
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcSecondaryButton
import com.travelcompanion.app.design.TcType

/**
 * Screen 04 — Cidade.
 *
 * The editorial layer: context, not operation. No departure, no platform, no
 * deadline reaches this screen; the only hour on it is where an attraction
 * sits in the itinerary, which is orientation rather than timing.
 *
 * Two Phase 1 substitutes show through and are meant to (D005): Fraunces is
 * not packaged, so the name and the chapter titles use the serif fallback, and
 * no photography is packaged, so the hero and the attraction thumbnails draw
 * the approved striped placeholder.
 */
@Composable
fun CityScreen(
    state: CityUiState,
    onBack: (() -> Unit)?,
    onPlayGuide: () -> Unit,
    onSeekToChapter: (Int) -> Unit,
    onOpenAttraction: (String) -> Unit,
    onStartWalk: (String) -> Unit,
    onPlayStory: (String) -> Unit,
    onOpenAction: (ActionLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Hero(state, onBack)

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                text = state.intro,
                style = TcType.editorialBody,
                color = FieldCompanionColors.Neutral800,
            )

            state.guide?.let { guide -> GuideCard(guide, onPlayGuide, onSeekToChapter) }

            if (state.attractions.isNotEmpty()) {
                SectionTitle("O que ver")
            }
        }

        if (state.attractions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                state.attractions.forEach { attraction ->
                    AttractionCard(attraction) { onOpenAttraction(attraction.id) }
                }
            }
        }

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            state.walk?.let { walk -> WalkCard(walk) { onStartWalk(walk.id) } }

            if (state.restaurants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Onde comer")
                    state.restaurants.forEach { restaurant ->
                        RestaurantRow(restaurant, onOpenAction)
                    }
                }
            }

            if (state.stories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Histórias curtas")
                    state.stories.forEach { story -> StoryCard(story, onPlayStory) }
                }
            }
        }
    }
}

@Composable
private fun Hero(state: CityUiState, onBack: (() -> Unit)?) {
    TcHero(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(TcHeroShape),
        imageAssetPath = state.heroAssetPath,
        placeholderCaption = state.heroCaption,
        // The same placeholder screen 05 uses under hero text: the paper
        // stripes are too light to carry a caption, and on the emulator the
        // country and the dates all but disappeared into them (D075).
        cool = true,
    ) {
        // The approved hero carries a 48dp back button, and it is drawn when
        // there is somewhere to go back to. Reached as the "Explorar" tab
        // there is not, and a root tab with a back arrow would be a lie.
        onBack?.let { back ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(48.dp)
                    .clip(TcPillShape)
                    .background(FieldCompanionColors.Ink.copy(alpha = 0.45f))
                    .clickable(onClick = back)
                    .semantics { contentDescription = "Voltar" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    TcIcons.ArrowLeft,
                    contentDescription = null,
                    tint = FieldCompanionColors.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.countryName.uppercase(),
                style = TcType.eyebrow,
                color = FieldCompanionColors.White.copy(alpha = 0.85f),
            )
            Text(
                text = state.name,
                style = TcType.displayEditorial,
                color = FieldCompanionColors.White,
            )
            state.stayLabel?.let {
                Text(text = it, style = TcType.meta, color = FieldCompanionColors.White)
            }
        }
    }
}

/**
 * The city guide, and the chapter list D025 has been waiting for.
 *
 * The list is collapsed until asked for: it is the longest thing on the screen
 * and the reader who wants chapter four is deliberate about it.
 */
@Composable
private fun GuideCard(
    guide: CityGuideUi,
    onPlay: () -> Unit,
    onSeekToChapter: (Int) -> Unit,
) {
    var expanded by remember(guide.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Teal)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(TcPillShape)
                    .background(FieldCompanionColors.White)
                    .clickable(onClick = onPlay)
                    .semantics { contentDescription = "Ouvir ${guide.title}" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    TcIcons.Play,
                    contentDescription = null,
                    tint = FieldCompanionColors.Teal,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(guide.title, style = TcType.action, color = FieldCompanionColors.White)
                Text(
                    text = if (guide.isPackaged) {
                        "${guide.summary} · salvo offline"
                    } else {
                        // D021: the guide exists in the trip, the file does not
                        // exist in this build, and the card says which.
                        "${guide.summary} · ainda não salvo neste aparelho"
                    },
                    style = TcType.meta,
                    color = FieldCompanionColors.OnInkMeta,
                )
            }
        }

        if (guide.chapters.isNotEmpty()) {
            TcOnInkSecondaryButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Ocultar capítulos" else "Capítulos")
            }
        }

        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                guide.chapters.forEach { chapter -> ChapterRow(chapter, onSeekToChapter) }
            }
        }
    }
}

@Composable
private fun ChapterRow(chapter: CityChapterUi, onSeekToChapter: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .clickable { onSeekToChapter(chapter.index) }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "${chapter.index + 1}",
            style = TcType.metaStrong,
            color = FieldCompanionColors.OnInkMeta,
            modifier = Modifier.width(20.dp),
        )
        Text(
            text = chapter.title,
            style = TcType.subheadline.copy(fontSize = 17.sp),
            color = FieldCompanionColors.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = chapter.durationLabel,
            style = TcType.meta,
            color = FieldCompanionColors.OnInkMeta,
        )
    }
}

@Composable
private fun AttractionCard(attraction: CityAttractionUi, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .clickable(onClick = onOpen),
    ) {
        TcHero(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            imageAssetPath = attraction.heroAssetPath,
            placeholderCaption = attraction.heroCaption,
            cool = true,
        )
        Column(
            modifier = Modifier
                .padding(14.dp)
                .testTag(AttractionCardTag),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = attraction.name,
                style = TcType.subheadline,
                color = FieldCompanionColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            attraction.scheduleTime?.let {
                Text(
                    text = "No roteiro às $it",
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral600,
                )
            }
            if (attraction.pills.isNotEmpty()) {
                // A pill carries a whole label or it says nothing useful, so a
                // long one takes the next line rather than being squeezed or
                // pushed past the card's edge — D055's rule, and the treatment
                // screens 06 and 15 already use.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    attraction.pills.forEach { pill ->
                        TcChip(text = pill, tone = TcChipTone.Neutral)
                    }
                }
            }
        }
    }
}

@Composable
private fun WalkCard(walk: CityWalkUi, onStart: () -> Unit) {
    TcInkCard(modifier = Modifier.fillMaxWidth()) {
        Text("PASSEIO DO DIA", style = TcType.eyebrow, color = FieldCompanionColors.OnInkAccent)
        Text(
            text = walk.title,
            style = TcType.subheadline,
            color = FieldCompanionColors.White,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = "${walk.distanceLabel} · ${walk.durationLabel} · ${walk.storiesLabel}",
            style = TcType.meta,
            color = FieldCompanionColors.OnInkMeta,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        TcOnInkSecondaryButton(onClick = onStart) { Text("Ver passeio") }
    }
}

@Composable
private fun RestaurantRow(restaurant: CityRestaurantUi, onOpenAction: (ActionLink) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Top-aligned, not centred: with the system font at 1.5 the name takes
        // two lines and a centred distance floats into the middle of them.
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = restaurant.name,
                style = TcType.body.copy(fontWeight = FontWeight.Bold),
                color = FieldCompanionColors.Ink,
                modifier = Modifier.weight(1f),
            )
            restaurant.distanceLabel?.let {
                Text(it, style = TcType.meta, color = FieldCompanionColors.Neutral600)
            }
        }
        restaurant.description?.let {
            Text(it, style = TcType.meta, color = FieldCompanionColors.Neutral700)
        }
        restaurant.practicalNote?.let {
            Text(it, style = TcType.meta, color = FieldCompanionColors.Neutral600)
        }
        restaurant.action?.let { action ->
            TcSecondaryButton(onClick = { onOpenAction(action) }) { Text(action.label) }
        }
    }
}

/**
 * A short story, with the two buttons the sheet gives it.
 *
 * *Ler* opens the text where it stands: the canonical reading surface is
 * screen 10, which is not in this block, and expanding in place adds no
 * twentieth screen while still letting the story be read.
 */
@Composable
private fun StoryCard(story: CityStoryUi, onPlay: (String) -> Unit) {
    var open by remember(story.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        story.durationLabel?.let {
            Text(
                text = "LEITURA DE $it".uppercase(),
                style = TcType.eyebrow,
                color = FieldCompanionColors.Neutral600,
            )
        }
        Text(story.title, style = TcType.subheadline, color = FieldCompanionColors.Ink)
        Text(
            text = if (open) story.body else story.hook,
            style = TcType.editorialBody,
            color = FieldCompanionColors.Neutral800,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            story.audioGuideId?.let {
                TcSecondaryButton(onClick = { onPlay(story.id) }) { Text("Ouvir") }
            }
            TcSecondaryButton(onClick = { open = !open }) {
                Text(if (open) "Fechar" else "Ler")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = TcType.eyebrow,
        color = FieldCompanionColors.Neutral600,
    )
}

/** The seam the layout test measures; nothing else uses it. */
internal const val AttractionCardTag: String = "city-attraction-card"
