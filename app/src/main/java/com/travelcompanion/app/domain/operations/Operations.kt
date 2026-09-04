package com.travelcompanion.app.domain.operations

import com.travelcompanion.app.data.trip.CriticalItem
import com.travelcompanion.app.data.trip.PlanB
import com.travelcompanion.app.data.trip.PlanBStep
import java.time.LocalTime

/**
 * Where a critical item stands against the clock.
 *
 * Critical timing is kept visually apart from editorial content everywhere in
 * this app, and this is the part of that separation that is a decision rather
 * than a layout: the traveller needs to know whether the moment to act is
 * ahead, now, or behind.
 */
enum class ActionWindow {
    /** The window has not opened; the nominal time is still comfortably away. */
    Ahead,

    /** Past the act-by time and before the nominal time. This is the moment. */
    Now,

    /** The nominal time has passed. */
    Passed,
}

/**
 * Reads a critical item against a clock, with no Android and no screen.
 *
 * `actionByTime` is the instruction's deadline ("esteja na estação até 19:00")
 * and `nominalTime` is when the thing itself happens (19:30). They are
 * different facts and the approved screens draw them apart, so they are
 * compared apart here too.
 */
fun actionWindow(item: CriticalItem, now: LocalTime): ActionWindow {
    val nominal = item.nominalTime?.let(::parseTime)
    val actBy = item.actionByTime?.let(::parseTime)

    if (nominal != null && !now.isBefore(nominal)) return ActionWindow.Passed
    if (actBy != null && !now.isBefore(actBy)) return ActionWindow.Now
    return ActionWindow.Ahead
}

/**
 * The step of a Plan B that is still worth trying.
 *
 * The approved screen numbers the steps in order of attempt, and each may
 * carry a deadline. The first whose deadline has not passed is the one the
 * traveller is on; when none of them declare a deadline, that is the first
 * step, because nothing has expired.
 */
fun currentStep(plan: PlanB, now: LocalTime): PlanBStep? =
    currentStepIndex(plan, now)?.let(plan.steps::get)

/** Index of [currentStep], for the screen to mark the row. */
fun currentStepIndex(plan: PlanB, now: LocalTime): Int? =
    plan.steps.indexOfFirst { step ->
        val deadline = step.deadline?.let(::parseTime) ?: return@indexOfFirst true
        now.isBefore(deadline)
    }.takeIf { it >= 0 }

/** "19:30" as a time, or null when the package wrote something else. */
private fun parseTime(value: String): LocalTime? =
    runCatching { LocalTime.parse(value) }.getOrNull()
