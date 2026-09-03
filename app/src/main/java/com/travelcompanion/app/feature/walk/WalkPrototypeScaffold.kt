package com.travelcompanion.app.feature.walk

/**
 * The approved prototype's scaffold on screen 07, when the build has one.
 *
 * Carries its own label so the release variant contributes neither the action
 * nor the words: this type is an empty shell there.
 */
data class WalkPrototypeScaffold(
    val label: String,
    val onActivate: () -> Unit,
)

