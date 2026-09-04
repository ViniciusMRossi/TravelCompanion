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
 * There are two, because the decision has two outcomes and the packaged trip
 * only ever produces one of them: every story in it declares `autoPlayInWalk`
 * and the walk declares `automaticStoriesDefault`, so a real arrival always
 * plays and screen 10 — the *offered* case — never rises. The second entry
 * turns automatic stories off first, which is the same switch a package can
 * throw on any story, and then arrives (D078). Neither invents a state: both
 * go through `onLocation` and the real decision.
 *
 * This lives in `src/debug` rather than behind `BuildConfig.DEBUG` in `main`.
 * The flag only makes the branch unreachable; the injector and its label still
 * ship. That is not a guess: with the flag, `simulateArrival` and the string
 * "simular chegada" were both found in the release DEX.
 */
fun walkArrivalScaffolds(
    content: TripContent,
    controller: WalkModeController,
): List<WalkPrototypeScaffold> = listOf(
    WalkPrototypeScaffold(
        label = "Protótipo · simular chegada num ponto de história →",
        onActivate = { arriveAtNextStop(content, controller) },
    ),
    WalkPrototypeScaffold(
        label = "Protótipo · simular chegada oferecida (tela 10) →",
        onActivate = {
            controller.setAutomaticStories(false)
            arriveAtNextStop(content, controller)
        },
    ),
)

private fun arriveAtNextStop(content: TripContent, controller: WalkModeController) {
    val nextStoryId = controller.state.value.nextStop?.storyId
    val geo = nextStoryId?.let { content.story(it)?.trigger?.geo }
    if (geo != null) {
        controller.onLocation(DeviceLocation(point = geo, accuracyMeters = 5f))
    }
}
