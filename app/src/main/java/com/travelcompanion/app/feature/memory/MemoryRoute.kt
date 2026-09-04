package com.travelcompanion.app.feature.memory

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.memory.MemoryPhase
import com.travelcompanion.app.service.memory.MemoryController
import com.travelcompanion.app.service.walk.WalkModeController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.ZoneId

/**
 * Screen 12 as a route.
 *
 * Reached from the "Gravar memória" shortcut on screen 02, which has existed
 * since Phase 1. The canonical flow is 11 → 12, and screen 11 is still out of
 * scope, so the entry that already exists is the one that is used (D058).
 */
@Composable
fun MemoryRoute(
    content: TripContent,
    participantId: String,
    walkModeController: WalkModeController,
    memoryController: MemoryController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recording by memoryController.state.collectAsStateWithLifecycle()
    val walkState by walkModeController.state.collectAsStateWithLifecycle()
    val level by memoryController.level.collectAsStateWithLifecycle()
    val saved by remember { memoryController.saved() }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // The elapsed clock the screen shows. Read from the machine rather than
    // counted here, so pausing and resuming cannot drift away from the file.
    var elapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(recording.phase, recording.runStartedAtMs) {
        while (true) {
            elapsedMs = recording.elapsedMs(android.os.SystemClock.elapsedRealtime())
            if (recording.phase != MemoryPhase.Recording) break
            delay(200L)
        }
    }

    val attribution = buildAttribution(content, walkState, participantId)

    // Asked immediately before it is needed, never at launch — the timing
    // brief §23 asks for, and the same shape Phase 3 uses for location. A
    // refusal costs the recording and not the screen (D030).
    val microphone = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && attribution != null) {
            memoryController.start(attribution)
        } else {
            memoryController.microphoneRefused()
        }
    }

    val state = buildMemoryState(
        content = content,
        walkState = walkState,
        recording = recording,
        saved = saved,
        localParticipantId = participantId,
        elapsedMs = elapsedMs,
        nowEpochMs = System.currentTimeMillis(),
        zone = ZoneId.systemDefault(),
    )

    MemoryScreen(
        state = state,
        level = level,
        onBack = onBack,
        onRecord = {
            val allowed = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            when {
                allowed && attribution != null -> memoryController.start(attribution)
                else -> microphone.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onPause = memoryController::pause,
        onResume = memoryController::resume,
        onFinish = memoryController::finish,
        onCancel = memoryController::cancel,
        onDone = {
            memoryController.acknowledge()
            onBack()
        },
        modifier = modifier,
    )
}

/** Kept so a preview or a test can stand in for the controller's level flow. */
internal fun flatLevel(value: Float) = MutableStateFlow(value)
