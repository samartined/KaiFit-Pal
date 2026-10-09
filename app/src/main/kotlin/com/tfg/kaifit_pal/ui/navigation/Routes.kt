package com.tfg.kaifit_pal.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe navigation routes. Top-level tabs carry no arguments.

@Serializable
data object ProfileRoute

@Serializable
data object CalculatorRoute

@Serializable
data object AssistantRoute

@Serializable
data object HelpRoute

@Serializable
data object SettingsRoute

/**
 * Results detail destination.
 *
 * @param tdee total daily energy expenditure, in kcal/day.
 * @param bodyFatPercent body fat percentage, when it could be estimated.
 * @param method name of the formula used to get [tdee].
 */
@Serializable
data class ResultsRoute(
    val tdee: Int,
    val bodyFatPercent: Float? = null,
    val method: String,
)
