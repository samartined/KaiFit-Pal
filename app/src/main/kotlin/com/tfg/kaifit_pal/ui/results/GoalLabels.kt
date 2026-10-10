package com.tfg.kaifit_pal.ui.results

import androidx.annotation.StringRes
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.domain.Goal
import com.tfg.kaifit_pal.domain.GoalIntensity
import com.tfg.kaifit_pal.domain.GoalModifier

/**
 * Name of the goal of [modifier], from "Déficit intenso" at −20 % to "Volumen intenso" at +20 %,
 * following the [Goal] × [GoalIntensity] table of `Goal.kt`.
 */
@StringRes
fun goalLabelRes(modifier: GoalModifier): Int = when (modifier.goal) {
    Goal.MAINTENANCE -> R.string.goal_maintenance
    Goal.DEFICIT -> when (modifier.intensity) {
        GoalIntensity.HIGH -> R.string.goal_deficit_high
        GoalIntensity.MODERATE_HIGH -> R.string.goal_deficit_moderate_high
        GoalIntensity.MODERATE -> R.string.goal_deficit_moderate
        // NONE only goes with MAINTENANCE; it is grouped here to keep the `when` exhaustive.
        GoalIntensity.LIGHT, GoalIntensity.NONE -> R.string.goal_deficit_light
    }
    Goal.SURPLUS -> when (modifier.intensity) {
        GoalIntensity.HIGH -> R.string.goal_surplus_high
        GoalIntensity.MODERATE_HIGH -> R.string.goal_surplus_moderate_high
        GoalIntensity.MODERATE -> R.string.goal_surplus_moderate
        GoalIntensity.LIGHT, GoalIntensity.NONE -> R.string.goal_surplus_light
    }
}
