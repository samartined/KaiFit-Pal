package com.tfg.kaifit_pal.ui.navigation

import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.Sex
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
 * Results detail destination: the measurements and activity level entered in the calculator.
 *
 * The results screen computes the TDEE again from them, so it needs nothing else and can save the
 * whole profile. Being navigation arguments, they also survive process death. Units are those of
 * [BodyMeasurements]; a `null` circumference was not entered.
 */
@Serializable
data class ResultsRoute(
    val sex: Sex,
    val ageYears: Int,
    val heightCm: Double,
    val weightKg: Double,
    val neckCm: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val activityLevel: ActivityLevel,
) {

    /** The measurements carried by this route. */
    fun toMeasurements(): BodyMeasurements =
        BodyMeasurements(sex, ageYears, heightCm, weightKg, neckCm, waistCm, hipCm)

    companion object {
        /** The route that shows the results of [measurements] with [activityLevel]. */
        fun from(measurements: BodyMeasurements, activityLevel: ActivityLevel): ResultsRoute = ResultsRoute(
            sex = measurements.sex,
            ageYears = measurements.ageYears,
            heightCm = measurements.heightCm,
            weightKg = measurements.weightKg,
            neckCm = measurements.neckCm,
            waistCm = measurements.waistCm,
            hipCm = measurements.hipCm,
            activityLevel = activityLevel,
        )
    }
}
