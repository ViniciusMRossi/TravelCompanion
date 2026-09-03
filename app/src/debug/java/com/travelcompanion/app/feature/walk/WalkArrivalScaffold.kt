package com.travelcompanion.app.feature.walk

import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.walk.DeviceLocation
import com.travelcompanion.app.service.walk.WalkModeController

/**
 * The approved prototype's "Protótipo ·" scaffold, debug builds only.
 *
 * It feeds the next stop's own packaged coordinates through
 * [WalkModeController.onLocation] — the same entry point a GPS fix uses — so
 * what it exercises is the real trigger decision rather than a shortcut around
 * it. Without it, checking that story triggering works means standing in
 * Baščaršija.
 *
 * This lives in `src/debug` rather than behind `BuildConfig.DEBUG` in `main`.
 * The flag only makes the branch unreachable; the injector and its label still
 * ship. That is not a guess: with the flag, `simulateArrival` and the string
 * "simular chegada" were both found in the release DEX.
 */
fun walkArrivalScaffold(
    content: TripContent,
    controller: WalkModeController,
): WalkPrototypeScaffold? = WalkPrototypeScaffold(
    label = "Protótipo · simular chegada num ponto de história →",
    onActivate = {
        val nextStoryId = controller.state.value.nextStop?.storyId
        val geo = nextStoryId?.let { content.story(it)?.trigger?.geo }
        if (geo != null) {
            controller.onLocation(DeviceLocation(point = geo, accuracyMeters = 5f))
        }
    },
)
