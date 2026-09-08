package com.travelcompanion.app.feature.shell

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.travelcompanion.app.service.location.PassiveStoryDiscovery
import com.travelcompanion.app.service.notification.CriticalAlertScheduler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.travelcompanion.app.domain.explore.exploreCitiesOf
import com.travelcompanion.app.domain.food.buildFoodState
import com.travelcompanion.app.domain.food.currentDayIndex
import com.travelcompanion.app.domain.today.ShortcutUi
import com.travelcompanion.app.feature.attraction.AttractionScreen
import com.travelcompanion.app.feature.attraction.AttractionViewModel
import com.travelcompanion.app.feature.placeholder.PlaceholderScreen
import com.travelcompanion.app.feature.today.TodayScreen
import com.travelcompanion.app.domain.today.DayWeather
import com.travelcompanion.app.feature.today.TodayViewModel
import com.travelcompanion.app.feature.document.DocumentRoute
import com.travelcompanion.app.feature.emergency.EmergencyScreen
import com.travelcompanion.app.feature.food.FoodScreen
import com.travelcompanion.app.feature.fullday.FullDayScreen
import com.travelcompanion.app.feature.city.CityScreen
import com.travelcompanion.app.feature.city.buildCityState
import com.travelcompanion.app.feature.city.rememberExploreCityCursor
import com.travelcompanion.app.feature.more.MoreScreen
import com.travelcompanion.app.domain.more.buildMoreState
import com.travelcompanion.app.domain.fullday.FullDayUseCase
import com.travelcompanion.app.feature.emergency.buildEmergencyState
import com.travelcompanion.app.feature.planb.PlanBScreen
import com.travelcompanion.app.feature.planb.buildPlanBState
import com.travelcompanion.app.feature.stay.StayScreen
import com.travelcompanion.app.feature.stay.buildStayState
import com.travelcompanion.app.feature.transport.TransportScreen
import com.travelcompanion.app.feature.transport.buildTransportState
import com.travelcompanion.app.feature.memory.MemoryRoute
import com.travelcompanion.app.feature.wallet.WalletScreen
import com.travelcompanion.app.domain.wallet.buildWalletState
import com.travelcompanion.app.feature.together.TogetherRoute
import com.travelcompanion.app.feature.walk.WalkRoute
import com.travelcompanion.app.service.external.ExternalActionLauncher
import com.travelcompanion.app.service.playback.AudioGuideRequest
import com.travelcompanion.app.service.playback.PlaybackController
import com.travelcompanion.app.service.playback.audioGuideRequest
import com.travelcompanion.app.service.memory.MemoryController
import com.travelcompanion.app.service.sync.GroupSessionController
import com.travelcompanion.app.service.walk.WalkModeController
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import java.time.LocalTime
import com.travelcompanion.app.service.playback.PlaybackState

/**
 * The navigation graph's own names for its destinations.
 *
 * Internal rather than private so a notification can name the screen its
 * deadline belongs to without a second copy of these strings: two places that
 * must agree about "stay/{stayId}" is the defect, not the coupling (D092).
 */
internal object Routes {
    const val TODAY = "today"
    const val TRIP = "trip"
    const val EXPLORE = "explore"
    const val WALLET = "wallet"
    const val MORE = "more"

    const val FULL_DAY = "full-day"
    const val ATTRACTION = "attraction/{attractionId}"
    const val DOCUMENT = "document/{documentId}"
    const val PLAN_B = "plan-b/{planBId}"
    const val EMERGENCY = "emergency"
    const val TRANSPORT = "transport/{transportId}"
    const val STAY = "stay/{stayId}"
    const val WALK = "walk/{walkId}"
    const val LISTEN_TOGETHER = "listen-together"
    const val MEMORY = "memory"
    const val FOOD = "food"

    fun attraction(id: String) = "attraction/$id"
    fun document(id: String) = "document/$id"
    fun planB(id: String) = "plan-b/$id"
    fun transport(id: String) = "transport/$id"
    fun stay(id: String) = "stay/$id"
    fun walk(id: String) = "walk/$id"
}

/**
 * Where a timeline row leads, or null when the row is not a door.
 *
 * D065 made a row tappable only when it points at a screen, and that rule was
 * written in two places: a set of openable kinds beside screen 02's row, and a
 * `when` over the same kinds in each route that answers the tap. Both lists
 * said transport and accommodation, and `walk` was in neither — so the packaged
 * 11:00 line on day 9 read as ordinary text, and the only way into screen 06
 * was Explorar -> Cidade -> card, which is not where anyone stands at eleven in
 * the morning. One function is the whole rule now: the affordance and the
 * destination are the same answer, and a kind added to one cannot go missing
 * from the other (D065, D092, D096).
 *
 * `attraction` stays out on purpose. Screen 05 is reached from Today's "now"
 * block, which carries "Ver atracao" for the attraction actually under way, so
 * that row is not the dead line the walk was; giving it a second door is a
 * design change and is not one this asked for.
 */
internal fun timelineDestination(kind: String, refId: String?): String? = when {
    refId == null -> null
    kind == "transport" -> Routes.transport(refId)
    kind == "accommodation" -> Routes.stay(refId)
    kind == "walk" -> Routes.walk(refId)
    else -> null
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
    walkModeController: WalkModeController,
    groupSessionController: GroupSessionController,
    memoryController: MemoryController,
    dayWeather: DayWeather,
    criticalAlertScheduler: CriticalAlertScheduler,
    passiveStoryDiscovery: PassiveStoryDiscovery,
    requestedRoute: String?,
    onRouteHandled: () -> Unit,
    onResetParticipant: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showsBottomNav = rootDestinations.any { it.route == route }

    val context = LocalContext.current
    val launcher = remember(context) { ExternalActionLauncher(context) }

    // Android's own screen for exact alarms, opened once and never again.
    // A deadline delivered in the next maintenance window is the defect the
    // warning exists to prevent, and there is no other way to be exact: Play
    // policy reserves USE_EXACT_ALARM for alarm clocks. No screen is drawn
    // here — this is the system's, like the dialer and the share sheet — and
    // a refusal is respected for good (D093).
    val askForExactAlarms = {
        if (criticalAlertScheduler.shouldAskForExact()) {
            criticalAlertScheduler.markExactAsked()
            openExactAlarmSettings(context)
        }
        Unit
    }

    // Asked once the traveller has said who they are, which is the first
    // moment the app has a deadline to announce — never at launch, the same
    // timing Phase 3 used for location and Phase 5 for the microphone.
    val notifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Whatever the answer. The two asks are sequential on purpose: opening
        // the settings screen while the permission dialog was still up put a
        // full-screen Activity over a question the traveller had not answered.
        askForExactAlarms()
    }

    // Android's own screen, for the permission Android will not put in a
    // dialog. From Android 11 requesting ACCESS_BACKGROUND_LOCATION shows the
    // traveller nothing and returns a refusal, so pointing at the settings
    // page is the only honest path — the same posture D093 took for exact
    // alarms. Android 10 still asks, so there it is asked.
    val backgroundLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    // Whether the three story circles can be watched at all right now, read
    // again every time the app comes back on screen: the answer changes in
    // Android's settings, which is a place this app can point at and never
    // control. Held in state so the row on screen 19 disappears the moment it
    // stops being true (D104).
    var storiesDiscoverable by remember { mutableStateOf(passiveStoryDiscovery.canDiscover()) }
    val discoveryScope = rememberCoroutineScope()

    LifecycleResumeEffect(Unit) {
        storiesDiscoverable = passiveStoryDiscovery.canDiscover()
        // Geofences are the system's, not this process's: they survive the app
        // being killed and are lost on a reboot. Bringing the registered set in
        // line here is what covers a permission just granted, a story that
        // fired while the app was closed, and a first launch.
        discoveryScope.launch { passiveStoryDiscovery.refresh() }
        onPauseOrDispose { }
    }

    // The set is replaced whenever the content or the traveller changes: a
    // deadline that moved must not keep its old alarm as well as its new one.
    LaunchedEffect(content, participantId) {
        criticalAlertScheduler.reschedule()

        val needsAsking = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsAsking) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        else askForExactAlarms()
    }

    val enableStoryDiscovery = {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            backgroundLocation.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            Unit
        }
    }

    // A notification names the screen its deadline belongs to — 15 for a
    // transport, 16 for a stay. Consumed once, so returning to the app later
    // does not jump back there.
    LaunchedEffect(requestedRoute) {
        val target = requestedRoute ?: return@LaunchedEffect
        runCatching { navController.navigate(target) }
        onRouteHandled()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper)
            // Both system bar insets are reserved here, once, for every route.
            // The compact player and fixed bars like "Iniciar passeio" appear
            // on routes that have no bottom navigation, and nothing else in the
            // shell would keep them clear of the navigation bar. Paper is
            // painted before the padding, so it still reaches the screen edges
            // behind the system bars.
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Routes.TODAY,
            ) {
                composable(Routes.TODAY) {
                    TodayRoute(
                        content,
                        participantId,
                        dayWeather,
                        criticalAlertScheduler,
                        navController,
                        launcher,
                    )
                }
                composable(Routes.TRIP) {
                    // The tab and "Ver dia completo" reach the same screen; the
                    // tab is a root, so it has no back row of its own.
                    FullDayRoute(content, navController, launcher, onBack = null)
                }
                composable(Routes.EXPLORE) {
                    CityRoute(content, playbackController, navController, launcher)
                }
                composable(Routes.WALLET) {
                    WalletScreen(
                        state = buildWalletState(content, LocalDate.now()),
                        onOpenDocument = { id -> navController.navigate(Routes.document(id)) },
                    )
                }
                composable(Routes.MORE) {
                    val group by groupSessionController.state.collectAsStateWithLifecycle()
                    MoreScreen(
                        state = buildMoreState(
                            content = content,
                            // The state the app already holds. Opening "Mais"
                            // never joins the group (D071).
                            known = group.participants,
                            localParticipantId = participantId,
                        ),
                        onOpenEmergency = { navController.navigate(Routes.EMERGENCY) },
                        onOpenPlanB = { id -> navController.navigate(Routes.planB(id)) },
                        onOpenAction = { action -> launcher.open(action.uri, action.fallbackUri) },
                        onResetParticipant = onResetParticipant,
                        // Shown only while passive discovery is off, and gone
                        // the moment it is on. This is the app's one sentence
                        // about the background permission: it never
                        // interrupts, never returns after it is answered, and
                        // says plainly that refusing it costs nothing but the
                        // automatic notice (D104).
                        onEnableStoryDiscovery = { enableStoryDiscovery() }
                            .takeIf { !storiesDiscoverable },
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
                    FullDayRoute(
                        content = content,
                        navController = navController,
                        launcher = launcher,
                        onBack = navController::popBackStack,
                    )
                }
                composable(Routes.DOCUMENT) { entry ->
                    DocumentRoute(
                        content = content,
                        documentId = entry.arguments?.getString("documentId"),
                        launcher = launcher,
                        onBack = navController::popBackStack,
                    )
                }
                composable(Routes.TRANSPORT) { entry ->
                    val state = buildTransportState(
                        content = content,
                        transportId = entry.arguments?.getString("transportId"),
                        now = LocalTime.now(),
                    )
                    if (state == null) {
                        PlaceholderScreen(
                            title = "Transporte",
                            message = "Este transporte não existe no conteúdo desta viagem.",
                            actionLabel = "Voltar",
                            onAction = navController::popBackStack,
                        )
                    } else {
                        TransportScreen(
                            state = state,
                            onBack = navController::popBackStack,
                            onAction = { action -> launcher.open(action.uri) },
                            // ACTION_DIAL, never ACTION_CALL: the traveller
                            // confirms, and no permission is asked for.
                            onDial = { number -> launcher.dial(number) },
                            onOpenDocument = { id -> navController.navigate(Routes.document(id)) },
                            onOpenPlanB = { id -> navController.navigate(Routes.planB(id)) },
                        )
                    }
                }
                composable(Routes.STAY) { entry ->
                    val state = buildStayState(
                        content = content,
                        stayId = entry.arguments?.getString("stayId"),
                        now = LocalTime.now(),
                    )
                    if (state == null) {
                        PlaceholderScreen(
                            title = "Hospedagem",
                            message = "Esta hospedagem não existe no conteúdo desta viagem.",
                            actionLabel = "Voltar",
                            onAction = navController::popBackStack,
                        )
                    } else {
                        StayScreen(
                            state = state,
                            onBack = navController::popBackStack,
                            onAction = { action -> launcher.open(action.uri) },
                            onDial = { number -> launcher.dial(number) },
                            onOpenVoucher = { id -> navController.navigate(Routes.document(id)) },
                        )
                    }
                }
                composable(Routes.EMERGENCY) {
                    val state = buildEmergencyState(content, LocalDate.now())
                    if (state == null) {
                        PlaceholderScreen(
                            title = "Emergência",
                            message = "Esta viagem não traz um perfil de emergência.",
                            actionLabel = "Voltar",
                            onAction = navController::popBackStack,
                        )
                    } else {
                        EmergencyScreen(
                            state = state,
                            onBack = navController::popBackStack,
                            onDial = { number -> launcher.dial(number) },
                        )
                    }
                }
                composable(Routes.FOOD) {
                    FoodRoute(content = content, navController = navController)
                }
                composable(Routes.PLAN_B) { entry ->
                    val state = buildPlanBState(
                        content = content,
                        planBId = entry.arguments?.getString("planBId"),
                        now = LocalTime.now(),
                    )
                    if (state == null) {
                        PlaceholderScreen(
                            title = "Plano B",
                            message = "Este plano não está no pacote desta viagem.",
                            actionLabel = "Voltar",
                            onAction = navController::popBackStack,
                        )
                    } else {
                        PlanBScreen(
                            state = state,
                            onBack = navController::popBackStack,
                            onAction = { action -> launcher.open(action.uri, action.fallbackUri) },
                            onDial = { number -> launcher.dial(number) },
                            onOpenDocument = { id -> navController.navigate(Routes.document(id)) },
                        )
                    }
                }
                composable(Routes.WALK) { entry ->
                    WalkRoute(
                        content = content,
                        walkId = entry.arguments?.getString("walkId").orEmpty(),
                        participantId = participantId,
                        walkModeController = walkModeController,
                        playbackController = playbackController,
                        groupSessionController = groupSessionController,
                        onExit = navController::popBackStack,
                        onListenTogether = { navController.navigate(Routes.LISTEN_TOGETHER) },
                        // The canonical 11 -> 12 -> 02 path, which existed in
                        // the design and had nowhere to start from until 11
                        // did. Screen 02's shortcut stays: it is a legitimate
                        // entry and already tested (D079).
                        onRecordMemory = {
                            navController.navigate(Routes.MEMORY) {
                                // 11 -> 12 -> 02: the walk is over, so back
                                // from the memory screen is Today and not a
                                // walk that no longer exists.
                                popUpTo(Routes.TODAY)
                            }
                        },
                    )
                }
                composable(Routes.LISTEN_TOGETHER) {
                    TogetherRoute(
                        content = content,
                        participantId = participantId,
                        playbackController = playbackController,
                        walkModeController = walkModeController,
                        groupSessionController = groupSessionController,
                        onBack = navController::popBackStack,
                    )
                }
                composable(Routes.MEMORY) {
                    MemoryRoute(
                        content = content,
                        participantId = participantId,
                        walkModeController = walkModeController,
                        memoryController = memoryController,
                        onBack = navController::popBackStack,
                    )
                }
            }
        }

        val playback by playbackController.state.collectAsStateWithLifecycle()
        val walkState by walkModeController.state.collectAsStateWithLifecycle()
        // Screen 07 carries its own transport on an ink field; showing the
        // persistent player under it would offer the same controls twice.
        if (playback.isActive && !walkState.isRunning) {
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

/**
 * Screen 20's day cursor lives here, and nowhere else.
 *
 * It starts on the day being lived every time the screen opens, and it is not
 * the value screen 02 reads for its food shortcut. Those are two different
 * facts that happen to be equal at the moment the screen opens, and a value
 * that usually equals another is not a guard, it is a coincidence (D089).
 */
@Composable
private fun FoodRoute(
    content: TripContent,
    navController: NavHostController,
) {
    val startIndex = remember(content) { currentDayIndex(content, LocalDate.now()) }
    var dayIndex by rememberSaveable(content) { mutableIntStateOf(startIndex) }
    val state = buildFoodState(content, dayIndex, Locale.forLanguageTag(content.info.locale))

    if (state == null) {
        PlaceholderScreen(
            title = "Comer aqui",
            message = "Esta viagem ainda não traz dias para mostrar.",
            actionLabel = "Voltar",
            onAction = navController::popBackStack,
        )
        return
    }

    FoodScreen(
        state = state,
        onBack = navController::popBackStack,
        onPreviousDay = { dayIndex = (dayIndex - 1).coerceAtLeast(0) },
        onNextDay = { dayIndex = (dayIndex + 1).coerceAtMost(content.days.lastIndex) },
    )
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

/**
 * Screen 03, with the day it is showing.
 *
 * The selected day is the only state here and it survives rotation; everything
 * else is a pure function of that date and the clock.
 */
/**
 * Screen 04, for the city the day is spent in — which is not always the city
 * the traveller sleeps in.
 *
 * The band of chips above the hero is an addition to the approved plate, made
 * with approval, and it is drawn only when the day carries two cities with
 * something to read (D152, D153). Its cursor is local to this screen and
 * starts over on every entry: screens 02, 03, 17 and 20 keep reading the base,
 * and they agree with this one on sixteen days of twenty, which is exactly the
 * coincidence D089 warns about.
 *
 * The city guide and the stories play through the same `PlaybackController`
 * every other screen uses, and the chapter list drives `seekToChapter`, which
 * has had no approved surface until now (D025).
 */
@Composable
private fun CityRoute(
    content: TripContent,
    playbackController: PlaybackController,
    navController: NavHostController,
    launcher: ExternalActionLauncher,
) {
    val today = LocalDate.now()
    // Explorar resolves its city from the day's timeline, and not from the
    // base the way screens 02, 03, 17 and 20 do — different question, D152.
    val cities = remember(content, today) { exploreCitiesOf(content, content.dayFor(today)) }
    val cursor = rememberExploreCityCursor(cities)
    val state = buildCityState(content, cursor.value, today)

    if (state == null) {
        PlaceholderScreen(
            title = "Explorar",
            message = "Esta viagem ainda não traz a cidade em conteúdo.",
        )
        return
    }

    CityScreen(
        state = state,
        // A root tab has nothing behind it, so the hero's back button is not
        // drawn here.
        onBack = null,
        onPlayGuide = {
            audioGuideRequest(content, state.guide?.id, subtitle = state.name)
                ?.let(playbackController::playAudioGuide)
        },
        onSeekToChapter = { index ->
            // Selecting a chapter of a guide that is not loaded loads it first
            // and starts there, rather than seeking whatever happens to be
            // playing — the list belongs to this guide.
            val request = audioGuideRequest(content, state.guide?.id, subtitle = state.name)
            if (playbackController.state.value.mediaId == state.guide?.id) {
                playbackController.seekToChapter(index)
            } else if (request is AudioGuideRequest.Playable) {
                playbackController.playAudioGuide(
                    request,
                    startPositionMs = request.chapters.getOrNull(index)?.startMs,
                )
            }
        },
        onOpenAttraction = { id -> navController.navigate(Routes.attraction(id)) },
        onStartWalk = { id -> navController.navigate(Routes.walk(id)) },
        onPlayStory = { storyId ->
            audioGuideRequest(
                content,
                content.story(storyId)?.audioGuideId,
                subtitle = content.story(storyId)?.title,
            )?.let(playbackController::playAudioGuide)
        },
        onOpenAction = { action -> launcher.open(action.uri, action.fallbackUri) },
        cities = cities.chips,
        onSelectCity = { cityId -> cursor.value = cityId },
    )
}

@Composable
private fun FullDayRoute(
    content: TripContent,
    navController: NavHostController,
    launcher: ExternalActionLauncher,
    onBack: (() -> Unit)?,
) {
    val useCase = remember(content) { FullDayUseCase(content) }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val state = useCase(LocalDate.parse(selected), LocalTime.now(), currentDate = LocalDate.now())

    if (state == null) {
        PlaceholderScreen(
            title = "Dia completo",
            message = "Esta viagem ainda não traz os dias em conteúdo.",
            actionLabel = onBack?.let { "Voltar" },
            onAction = onBack,
        )
        return
    }

    FullDayScreen(
        state = state,
        onBack = onBack,
        onGoToDate = { date -> selected = date.toString() },
        onOpenMaps = { uri -> launcher.open(uri) },
        onOpenDocument = { id -> navController.navigate(Routes.document(id)) },
        onOpenPlanB = { id -> navController.navigate(Routes.planB(id)) },
        onOpenTransport = { id -> navController.navigate(Routes.transport(id)) },
        onOpenStay = { id -> navController.navigate(Routes.stay(id)) },
        onOpenTimelineItem = { kind, id ->
            timelineDestination(kind, id)?.let(navController::navigate)
        },
    )
}

/**
 * Android's own exact-alarm settings screen.
 *
 * The whole app has two reasons to reach it and one place that names the
 * constant: the once-ever ask of D093, and the row on screen 02 that the
 * traveller taps (D181). Which of the two is allowed to run is the caller's
 * decision and not this function's — it opens the screen and nothing else.
 */
private fun openExactAlarmSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
    }
}

@Composable
private fun TodayRoute(
    content: TripContent,
    participantId: String,
    dayWeather: DayWeather,
    criticalAlertScheduler: CriticalAlertScheduler,
    navController: NavHostController,
    launcher: ExternalActionLauncher,
) {
    val context = LocalContext.current
    val viewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.factory(
            content,
            participantId,
            dayWeather,
            // The same road `dayWeather` takes: the shell already holds the
            // scheduler, so screen 02 learns whether its warnings can be exact
            // without `TodayUseCase` ever meeting `AlarmManager` (D181).
            exactAlarms = criticalAlertScheduler::canBeExact,
        ),
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
        onOpenTimelineItem = { row ->
            timelineDestination(row.item.kind, row.item.refId)?.let(navController::navigate)
        },
        onOpenShortcut = { shortcut ->
            when (shortcut.kind) {
                ShortcutUi.Kind.Document -> navController.navigate(Routes.document(shortcut.id))
                ShortcutUi.Kind.PlanB -> navController.navigate(Routes.planB(shortcut.id))
                ShortcutUi.Kind.Food -> navController.navigate(Routes.FOOD)
                ShortcutUi.Kind.Memory -> navController.navigate(Routes.MEMORY)
            }
        },
        // Opened because the traveller touched the row, which is the traveller
        // asking. D093's rule is that the *app* opens this screen once and
        // never again of its own accord, and that rule is untouched: nothing
        // here runs without a tap, and `markExactAsked` is never consulted
        // (D181).
        onOpenAlarmSettings = { openExactAlarmSettings(context) },
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
    }
}

private fun NavHostController.navigateToRoot(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
