package com.travelcompanion.app.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcAudioPlayer
import com.travelcompanion.app.design.TcAudioPlayerVariant
import com.travelcompanion.app.design.TcHairline
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.design.formatPlaybackTime
import com.travelcompanion.app.domain.today.ShortcutUi
import com.travelcompanion.app.feature.attraction.AttractionScreen
import com.travelcompanion.app.feature.attraction.AttractionViewModel
import com.travelcompanion.app.feature.placeholder.PlaceholderScreen
import com.travelcompanion.app.feature.today.TodayScreen
import com.travelcompanion.app.feature.today.TodayViewModel
import com.travelcompanion.app.service.external.ExternalActionLauncher
import com.travelcompanion.app.service.playback.PlaybackController
import com.travelcompanion.app.service.playback.PlaybackState

private object Routes {
    const val TODAY = "today"
    const val TRIP = "trip"
    const val EXPLORE = "explore"
    const val WALLET = "wallet"
    const val MORE = "more"

    const val FULL_DAY = "full-day"
    const val ATTRACTION = "attraction/{attractionId}"
    const val DOCUMENT = "document/{documentId}"
    const val PLAN_B = "plan-b/{planBId}"
    const val WALK = "walk/{walkId}"
    const val MEMORY = "memory"

    fun attraction(id: String) = "attraction/$id"
    fun document(id: String) = "document/$id"
    fun planB(id: String) = "plan-b/$id"
    fun walk(id: String) = "walk/$id"
}

private data class RootDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val rootDestinations = listOf(
    RootDestination(Routes.TODAY, "Hoje", TcIcons.Sun),
    RootDestination(Routes.TRIP, "Viagem", TcIcons.Calendar),
    RootDestination(Routes.EXPLORE, "Explorar", TcIcons.Compass),
    RootDestination(Routes.WALLET, "Carteira", TcIcons.Wallet),
    RootDestination(Routes.MORE, "Mais", TcIcons.MoreHoriz),
)

@Composable
fun AppNavigation(
    content: TripContent,
    participantId: String,
    playbackController: PlaybackController,
    onResetParticipant: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showsBottomNav = rootDestinations.any { it.route == route }

    val context = LocalContext.current
    val launcher = remember(context) { ExternalActionLauncher(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Routes.TODAY,
            ) {
                composable(Routes.TODAY) {
                    TodayRoute(content, participantId, navController, launcher)
                }
                composable(Routes.TRIP) {
                    PlaceholderScreen(
                        title = "Viagem",
                        message = "Telas 03 Dia completo e roteiro da viagem — fase 6.",
                    )
                }
                composable(Routes.EXPLORE) {
                    PlaceholderScreen(
                        title = "Explorar",
                        message = "Tela 04 Cidade e navegação editorial — fase 6.",
                    )
                }
                composable(Routes.WALLET) {
                    PlaceholderScreen(
                        title = "Carteira",
                        message = "Telas 13 Carteira e 14 Documento/QR — fase 6.",
                    )
                }
                composable(Routes.MORE) {
                    PlaceholderScreen(
                        title = "Mais",
                        message = "Emergência, frases, apps, grupo e configurações — fase 6.",
                        actionLabel = "Trocar participante",
                        onAction = onResetParticipant,
                    )
                }

                composable(Routes.ATTRACTION) { entry ->
                    val attractionId = entry.arguments?.getString("attractionId").orEmpty()
                    AttractionRoute(
                        content = content,
                        attractionId = attractionId,
                        playbackController = playbackController,
                        navController = navController,
                        launcher = launcher,
                    )
                }

                composable(Routes.FULL_DAY) {
                    PlaceholderScreen(
                        title = "Dia completo",
                        message = "Tela 03 — fase 6.",
                        actionLabel = "Voltar",
                        onAction = navController::popBackStack,
                    )
                }
                composable(Routes.DOCUMENT) { entry ->
                    PlaceholderScreen(
                        title = "Documento",
                        message = "Tela 14 Documento/QR — fase 6. " +
                            "Documento: ${entry.arguments?.getString("documentId").orEmpty()}",
                        actionLabel = "Voltar",
                        onAction = navController::popBackStack,
                    )
                }
                composable(Routes.PLAN_B) {
                    PlaceholderScreen(
                        title = "Plano B",
                        message = "Tela 18 — fase 6.",
                        actionLabel = "Voltar",
                        onAction = navController::popBackStack,
                    )
                }
                composable(Routes.WALK) {
                    PlaceholderScreen(
                        title = "Iniciar passeio",
                        message = "Telas 06 a 11 do Modo Passeio — fase 3.",
                        actionLabel = "Voltar",
                        onAction = navController::popBackStack,
                    )
                }
                composable(Routes.MEMORY) {
                    PlaceholderScreen(
                        title = "Gravar memória",
                        message = "Tela 12 Memória por voz — fase 5.",
                        actionLabel = "Voltar",
                        onAction = navController::popBackStack,
                    )
                }
            }
        }

        val playback by playbackController.state.collectAsStateWithLifecycle()
        if (playback.isActive) {
            TcAudioPlayer(
                title = playback.title.orEmpty(),
                subtitle = playback.compactSubtitle(),
                progress = playback.progress,
                elapsed = formatPlaybackTime(playback.positionMs),
                total = formatPlaybackTime(playback.durationMs),
                isPlaying = playback.isPlaying,
                isBuffering = playback.status == PlaybackState.Status.Buffering,
                variant = TcAudioPlayerVariant.Compact,
                onTogglePlayPause = playbackController::togglePlayPause,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        if (showsBottomNav) {
            TcBottomNavigation(
                currentRoute = route,
                onSelect = { destination -> navController.navigateToRoot(destination.route) },
            )
        }
    }
}

@Composable
private fun AttractionRoute(
    content: TripContent,
    attractionId: String,
    playbackController: PlaybackController,
    navController: NavHostController,
    launcher: ExternalActionLauncher,
) {
    val viewModel: AttractionViewModel = viewModel(
        key = attractionId,
        factory = AttractionViewModel.factory(content, attractionId, playbackController),
    )
    val state = viewModel.state
    if (state == null) {
        PlaceholderScreen(
            title = "Atração",
            message = "Este ponto não existe no conteúdo desta viagem.",
            actionLabel = "Voltar",
            onAction = navController::popBackStack,
        )
        return
    }

    AttractionScreen(
        state = state,
        onBack = navController::popBackStack,
        onPlayAudioGuide = viewModel::onPlayAudioGuide,
        onOpenMaps = { uri -> launcher.open(uri) },
        onDirections = { uri, fallback -> launcher.open(uri, fallback) },
        onStartWalk = { walkId -> navController.navigate(Routes.walk(walkId)) },
    )
}

@Composable
private fun TodayRoute(
    content: TripContent,
    participantId: String,
    navController: NavHostController,
    launcher: ExternalActionLauncher,
) {
    val viewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.factory(content, participantId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Today is a function of the clock, so it is recomputed whenever the
    // traveller comes back to the screen.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val today = state
    if (today == null) {
        PlaceholderScreen(
            title = "Hoje",
            message = "Esta viagem ainda não tem dias configurados.",
        )
        return
    }

    TodayScreen(
        state = today,
        onOpenAttraction = { id -> navController.navigate(Routes.attraction(id)) },
        onOpenMaps = { uri -> launcher.open(uri) },
        onOpenFullDay = { navController.navigate(Routes.FULL_DAY) },
        onOpenShortcut = { shortcut ->
            when (shortcut.kind) {
                ShortcutUi.Kind.Document -> navController.navigate(Routes.document(shortcut.id))
                ShortcutUi.Kind.PlanB -> navController.navigate(Routes.planB(shortcut.id))
                ShortcutUi.Kind.Memory -> navController.navigate(Routes.MEMORY)
            }
        },
    )
}

/**
 * Bottom navigation, drawn to the approved specification rather than with
 * default Material styling: 66dp, hairline top rule, teal for the active root.
 */
@Composable
private fun TcBottomNavigation(
    currentRoute: String?,
    onSelect: (RootDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldCompanionColors.Surface),
    ) {
        TcHairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            rootDestinations.forEach { destination ->
                val selected = currentRoute == destination.route
                val tint = if (selected) FieldCompanionColors.Teal else FieldCompanionColors.Neutral600
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onSelect(destination) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        tint = tint,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(text = destination.label, style = TcType.navLabel, color = tint)
                }
            }
        }
        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

private fun NavHostController.navigateToRoot(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
