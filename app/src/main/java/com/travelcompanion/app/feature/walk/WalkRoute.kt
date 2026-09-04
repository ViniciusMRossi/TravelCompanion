package com.travelcompanion.app.feature.walk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.walk.WalkPhase
import com.travelcompanion.app.feature.placeholder.PlaceholderScreen
import com.travelcompanion.app.feature.story.StoryTriggerSheet
import com.travelcompanion.app.feature.story.buildStoryTriggerState
import com.travelcompanion.app.service.sync.GroupSessionController
import com.travelcompanion.app.service.playback.PlaybackController
import com.travelcompanion.app.service.walk.WalkModeController
import java.time.LocalDate
import java.time.LocalTime

/**
 * Screens 06 and 07 as one route.
 *
 * They are one Walk Mode state machine seen from two angles, so the phase
 * decides which is on screen rather than the back stack: a walk that is
 * running must not be something the traveller can leave behind by navigating.
 */
@Composable
fun WalkRoute(
    content: TripContent,
    walkId: String,
    participantId: String,
    walkModeController: WalkModeController,
    playbackController: PlaybackController,
    groupSessionController: GroupSessionController,
    onExit: () -> Unit,
    onListenTogether: () -> Unit,
    onRecordMemory: () -> Unit,
) {
    val context = LocalContext.current
    val walkState by walkModeController.state.collectAsStateWithLifecycle()
    val playback by playbackController.state.collectAsStateWithLifecycle()

    val walk = content.walk(walkId)
    var headphonesConnected by remember { mutableStateOf(hasHeadphones(context)) }

    // Loaded once per walk; re-entering the screen must not reset a walk that
    // is already running.
    LaunchedEffect(walkId) {
        if (!walkModeController.state.value.isRunning) {
            walkModeController.prepare(content, walkId)
        }
    }

    // Both answers can change while the traveller is on this screen — they may
    // step into settings to grant location, or plug in headphones.
    LifecycleResumeEffect(walkId) {
        headphonesConnected = hasHeadphones(context)
        walkModeController.refreshLocationQuality()
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        // Granted or refused, the walk starts either way: location stories are
        // enrichment, and refusing them must not cost the traveller the walk.
        walkModeController.refreshLocationQuality()
        walkModeController.start()
    }

    DisposableEffect(walkId) {
        onDispose {
            // Leaving screen 06 without starting releases the walk; leaving
            // while it runs does not, because the phone is in a pocket by then.
            if (!walkModeController.state.value.isRunning) walkModeController.clear()
        }
    }

    if (walk == null) {
        PlaceholderScreen(
            title = "Passeio",
            message = "Este passeio não existe no conteúdo desta viagem.",
            actionLabel = "Voltar",
            onAction = onExit,
        )
        return
    }

    when (walkState.phase) {
        WalkPhase.Active, WalkPhase.Paused -> {
            val activeState = buildActiveWalkState(walkState, playback, walk)
            Box(modifier = Modifier.fillMaxSize()) {
                ActiveWalkScreen(
                    state = activeState,
                    onClose = walkModeController::finish,
                    onTogglePlayPause = playbackController::togglePlayPause,
                    onSkipBack = { playbackController.seekBy(-SKIP_MILLIS) },
                    onSkipForward = { playbackController.seekBy(SKIP_MILLIS) },
                    onListenTogether = onListenTogether,
                    // Supplied by the debug variant and absent from the release
                    // one; `main` never names it (D031).
                    scaffolds = walkArrivalScaffolds(content, walkModeController),
                )

                // Screen 10 rises over the walk rather than replacing it: the
                // sheet's whole composition is the walk still visible above,
                // dimmed, with the story below (D078).
                buildStoryTriggerState(content, walkState)?.let { story ->
                    StoryTriggerSheet(
                        state = story,
                        onListen = walkModeController::playPendingStory,
                        onLater = walkModeController::dismissPendingStory,
                    )
                }
            }
        }

        WalkPhase.Completed -> {
            val group by groupSessionController.state.collectAsStateWithLifecycle()
            val finished = buildWalkFinishedState(
                content = content,
                walkState = walkState,
                localParticipantId = participantId,
                // The state the app already holds; opening this screen never
                // joins the group (D071).
                knownGroup = group.participants,
                date = LocalDate.now(),
                time = LocalTime.now(),
            )
            if (finished == null) {
                onExit()
            } else {
                WalkFinishedScreen(
                    state = finished,
                    onRecordMemory = onRecordMemory,
                    onBackToToday = {
                        walkModeController.clear()
                        onExit()
                    },
                )
            }
        }

        else -> {
            val state = buildStartWalkState(
                content = content,
                walkId = walkId,
                date = LocalDate.now(),
                headphonesConnected = headphonesConnected,
                automaticStories = walkState.automaticStories,
                locationGranted = walkState.locationQuality != com.travelcompanion.app.domain.walk.LocationQuality.Denied,
                localParticipantId = participantId,
            )
            if (state == null) {
                PlaceholderScreen(
                    title = "Passeio",
                    message = "Este passeio não existe no conteúdo desta viagem.",
                    actionLabel = "Voltar",
                    onAction = onExit,
                )
                return
            }
            StartWalkScreen(
                state = state,
                onBack = onExit,
                onStart = {
                    // The brief's timing: asked here, immediately before Walk
                    // Mode needs it, never at first launch.
                    permissionLauncher.launch(walkPermissions())
                },
                onOpenLocationSettings = { openAppSettings(context) },
            )
        }
    }
}

/** −15 / +15 on screen 07, the transport D025 deferred seek to. */
private const val SKIP_MILLIS = 15_000L

/**
 * The only way back from a permanently refused permission.
 *
 * Android stops showing the dialog after a second refusal, so the system
 * settings page is where the traveller has to go — the app can point at it,
 * never grant it.
 */
private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun walkPermissions(): Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    // The walk's persistent notification is what Android requires to keep
    // location running with the screen off, so it is asked for here too.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

/**
 * Whether audio would actually reach a pair of ears right now.
 *
 * Walk Mode assumes headphones, so screen 06 reports what is really connected
 * instead of asserting "Prontos" and being wrong.
 */
private fun hasHeadphones(context: Context): Boolean {
    val manager = context.getSystemService(AudioManager::class.java) ?: return false
    return manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
        when (device.type) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            -> true
            AudioDeviceInfo.TYPE_BLE_HEADSET ->
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            else -> false
        }
    }
}
