package com.travelcompanion.app.feature.planb

import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.data.trip.PlanBStep
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.operations.PhoneUi
import com.travelcompanion.app.domain.operations.currentStepIndex
import com.travelcompanion.app.domain.operations.phone
import java.time.LocalTime

/** One numbered step, and whether it is the one still worth trying. */
data class PlanBStepUi(val step: PlanBStep, val isCurrent: Boolean)

/** Something already kept for this scenario — a ticket, a voucher. */
data class AlternativeUi(val documentId: String, val title: String)

/** Screen 18 state. */
data class PlanBUiState(
    val scenario: String,
    val reassurance: String,
    val steps: List<PlanBStepUi>,
    val alternatives: List<AlternativeUi>,
    /** Phones the steps declare, gated like every other packaged number. */
    val phones: Map<String, PhoneUi>,
)

/**
 * Builds screen 18.
 *
 * Which step is current is a decision about the clock and belongs in
 * `domain/operations`; this only asks it. The alternatives are the documents
 * the package already keeps for this scenario, so "já guardado" means it
 * really is.
 */
fun buildPlanBState(
    content: TripContent,
    planBId: String?,
    now: LocalTime,
): PlanBUiState? {
    val plan = content.planB(planBId) ?: return null
    val current = currentStepIndex(plan, now)

    return PlanBUiState(
        scenario = plan.scenario,
        reassurance = plan.reassurance,
        steps = plan.steps.mapIndexed { index, step -> PlanBStepUi(step, index == current) },
        alternatives = plan.relatedDocumentIds.mapNotNull { id ->
            content.document(id)?.let { AlternativeUi(id, it.title) }
        },
        phones = plan.steps
            .flatMap { it.actions }
            .filter { it.kind == "phone" }
            .associate { action ->
                action.uri to phone(
                    label = action.label,
                    number = action.uri.removePrefix("tel:"),
                    isMockContent = content.trip.metadata.isMockContent,
                )
            },
    )
}
