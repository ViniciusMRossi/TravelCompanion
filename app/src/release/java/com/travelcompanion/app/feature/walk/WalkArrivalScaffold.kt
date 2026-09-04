package com.travelcompanion.app.feature.walk

import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.service.walk.WalkModeController

/**
 * No scaffold in a release build.
 *
 * The counterpart in `src/debug` builds the real one. Splitting by source set
 * is what actually keeps the coordinate injector and its label out of the
 * shipped binary — a `BuildConfig.DEBUG` branch in `main` left both in the
 * release DEX, unreachable but present.
 */
@Suppress("UNUSED_PARAMETER")
fun walkArrivalScaffolds(
    content: TripContent,
    controller: WalkModeController,
): List<WalkPrototypeScaffold> = emptyList()
