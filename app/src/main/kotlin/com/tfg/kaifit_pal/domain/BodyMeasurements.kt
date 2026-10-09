package com.tfg.kaifit_pal.domain

/** Biological sex, which selects the US Navy and Mifflin-St Jeor variants. */
enum class Sex { MALE, FEMALE }

/**
 * Physical activity level and the factor that turns the basal metabolic rate into the total
 * daily energy expenditure (TDEE = BMR × [factor]).
 */
enum class ActivityLevel(val factor: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9),
}

/** Field of [BodyMeasurements] that a [ValidationError] refers to. */
enum class MeasurementField { AGE, HEIGHT, WEIGHT, NECK, WAIST, HIP }

/** A rule broken by one field of [BodyMeasurements]. */
data class ValidationError(val field: MeasurementField, val kind: Kind) {

    enum class Kind {
        /** The value is outside its valid range, `NaN` or infinite. */
        OUT_OF_RANGE,

        /** The waist is not larger than the neck, so the US Navy formula has no meaning. */
        WAIST_NOT_ABOVE_NECK,
    }
}

/**
 * Body measurements entered by the user, and the checks they must pass before any calculation.
 *
 * Units: age in years, lengths in centimetres, weight in kilograms. Circumferences are optional:
 * `null` means "not entered", which is different from an invalid `0`. The activity level is not
 * a measurement and lives apart, in [ActivityLevel].
 *
 * @property ageYears age, in whole years.
 * @property heightCm height, in cm.
 * @property weightKg weight, in kg.
 * @property neckCm neck circumference, in cm, or `null` when not entered.
 * @property waistCm waist circumference, in cm, or `null` when not entered.
 * @property hipCm hip circumference, in cm, or `null` when not entered. Only used for women.
 */
data class BodyMeasurements(
    val sex: Sex,
    val ageYears: Int,
    val heightCm: Double,
    val weightKg: Double,
    val neckCm: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
) {

    /**
     * Returns every broken rule, or an empty list when the measurements are valid. Never throws.
     *
     * - Age, height and weight are required: out of range or not finite gives
     *   [ValidationError.Kind.OUT_OF_RANGE].
     * - A circumference that is present and out of range or not finite gives the same error;
     *   a `null` one is fine. The hip is only checked for [Sex.FEMALE].
     * - When both are present, a waist that is not larger than the neck gives
     *   [ValidationError.Kind.WAIST_NOT_ABOVE_NECK] on [MeasurementField.WAIST].
     *
     * All range limits are inclusive.
     */
    fun validate(): List<ValidationError> = buildList {
        if (ageYears !in AGE_RANGE) add(outOfRange(MeasurementField.AGE))
        if (!heightCm.isWithin(HEIGHT_RANGE_CM)) add(outOfRange(MeasurementField.HEIGHT))
        if (!weightKg.isWithin(WEIGHT_RANGE_KG)) add(outOfRange(MeasurementField.WEIGHT))
        if (neckCm != null && !neckCm.isWithin(NECK_RANGE_CM)) add(outOfRange(MeasurementField.NECK))
        if (waistCm != null && !waistCm.isWithin(WAIST_RANGE_CM)) add(outOfRange(MeasurementField.WAIST))
        if (sex == Sex.FEMALE && hipCm != null && !hipCm.isWithin(HIP_RANGE_CM)) {
            add(outOfRange(MeasurementField.HIP))
        }
        if (neckCm != null && waistCm != null && waistCm <= neckCm) {
            add(ValidationError(MeasurementField.WAIST, ValidationError.Kind.WAIST_NOT_ABOVE_NECK))
        }
    }

    companion object {
        /** Accepted age, in years. */
        val AGE_RANGE: IntRange = 18..90

        /** Accepted height, in cm. */
        val HEIGHT_RANGE_CM: ClosedFloatingPointRange<Double> = 100.0..250.0

        /** Accepted weight, in kg. */
        val WEIGHT_RANGE_KG: ClosedFloatingPointRange<Double> = 30.0..300.0

        /** Accepted neck circumference, in cm. */
        val NECK_RANGE_CM: ClosedFloatingPointRange<Double> = 20.0..80.0

        /** Accepted waist circumference, in cm. */
        val WAIST_RANGE_CM: ClosedFloatingPointRange<Double> = 40.0..200.0

        /** Accepted hip circumference, in cm. */
        val HIP_RANGE_CM: ClosedFloatingPointRange<Double> = 50.0..200.0
    }
}

private fun outOfRange(field: MeasurementField) = ValidationError(field, ValidationError.Kind.OUT_OF_RANGE)

private fun Double.isWithin(range: ClosedFloatingPointRange<Double>): Boolean = isFinite() && this in range
