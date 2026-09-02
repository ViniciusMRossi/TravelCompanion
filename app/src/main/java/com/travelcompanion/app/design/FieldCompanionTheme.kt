package com.travelcompanion.app.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FieldCompanionLightColors = lightColorScheme(
    primary = FieldCompanionColors.Teal,
    onPrimary = FieldCompanionColors.Surface,
    secondary = FieldCompanionColors.Gold,
    onSecondary = FieldCompanionColors.Ink,
    error = FieldCompanionColors.Oxblood,
    onError = FieldCompanionColors.Surface,
    background = FieldCompanionColors.Paper,
    onBackground = FieldCompanionColors.Ink,
    surface = FieldCompanionColors.Surface,
    onSurface = FieldCompanionColors.Ink,
)

@Composable
fun FieldCompanionTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FieldCompanionLightColors,
        typography = FieldCompanionTypography,
        content = content,
    )
}
