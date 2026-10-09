package com.tfg.kaifit_pal.domain

import kotlin.math.roundToInt

/** Equation used to estimate the basal metabolic rate. */
enum class TdeeMethod { KATCH_MCARDLE, MIFFLIN_ST_JEOR }

/**
 * Total daily energy expenditure and how it was obtained.
 *
 * @property tdeeKcal total daily energy expenditure, in kcal/day.
 * @property bmrKcal basal metabolic rate, in kcal/day, unrounded.
 * @property method equation used for [bmrKcal].
 * @property bodyFatPercent US Navy body fat percentage used by Katch-McArdle, or `null` when the
 *   method is [TdeeMethod.MIFFLIN_ST_JEOR].
 */
data class TdeeResult(
    val tdeeKcal: Int,
    val bmrKcal: Double,
    val method: TdeeMethod,
    val bodyFatPercent: Double?,
)

/**
 * Total daily energy expenditure (TDEE = BMR × activity factor), in kcal/day.
 *
 * The basal metabolic rate (BMR) comes from:
 * - Katch-McArdle, when the body fat percentage is known:
 *   `370 + 21.6 × lean body mass (kg)`;
 * - Mifflin-St Jeor (Mifflin et al., 1990) otherwise, which uses age instead of body fat:
 *   `10 × weight (kg) + 6.25 × height (cm) − 5 × age (years) + s`, with `s = +5` for men and
 *   `−161` for women.
 *
 * Only the TDEE is rounded, to the nearest kcal.
 */
object TdeeCalculator {

    /** Katch-McArdle BMR, in kcal/day, for [weightKg] in kg and [bodyFatPercent] in %. */
    fun katchMcArdleBmr(weightKg: Double, bodyFatPercent: Double): Double =
        370 + 21.6 * weightKg * (1 - bodyFatPercent / 100)

    /** Mifflin-St Jeor BMR, in kcal/day, for [ageYears] in years, [heightCm] in cm and [weightKg] in kg. */
    fun mifflinStJeorBmr(sex: Sex, ageYears: Int, heightCm: Double, weightKg: Double): Double {
        val sexConstant = when (sex) {
            Sex.MALE -> 5
            Sex.FEMALE -> -161
        }
        return 10 * weightKg + 6.25 * heightCm - 5 * ageYears + sexConstant
    }

    /** TDEE, in kcal/day rounded to the nearest unit, for a BMR in kcal/day. */
    fun tdeeKcal(bmrKcal: Double, activityLevel: ActivityLevel): Int =
        (bmrKcal * activityLevel.factor).roundToInt()

    /**
     * Picks the equation from the available data: Katch-McArdle with the US Navy body fat
     * percentage when [BodyFatCalculator.usNavy] can estimate it, Mifflin-St Jeor otherwise.
     *
     * Returns `null`, and never throws, when [BodyMeasurements.validate] reports any error.
     */
    fun calculate(measurements: BodyMeasurements, activityLevel: ActivityLevel): TdeeResult? {
        if (measurements.validate().isNotEmpty()) return null
        val bodyFatPercent = BodyFatCalculator.usNavy(measurements)
        return if (bodyFatPercent != null) {
            val bmr = katchMcArdleBmr(measurements.weightKg, bodyFatPercent)
            TdeeResult(tdeeKcal(bmr, activityLevel), bmr, TdeeMethod.KATCH_MCARDLE, bodyFatPercent)
        } else {
            val bmr = with(measurements) { mifflinStJeorBmr(sex, ageYears, heightCm, weightKg) }
            TdeeResult(tdeeKcal(bmr, activityLevel), bmr, TdeeMethod.MIFFLIN_ST_JEOR, bodyFatPercent = null)
        }
    }
}
