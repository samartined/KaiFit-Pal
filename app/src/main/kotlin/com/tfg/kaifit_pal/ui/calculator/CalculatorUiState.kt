package com.tfg.kaifit_pal.ui.calculator

import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyFatCalculator
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.MeasurementField
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.domain.ValidationError
import com.tfg.kaifit_pal.ui.components.parseDecimal

/** Why the text of a measurement field is not accepted; shown under the field. */
enum class FieldError {
    /** The text is not a number. */
    INVALID_NUMBER,

    /** The number is outside the accepted range of the field. */
    OUT_OF_RANGE,

    /** The waist is not larger than the neck. */
    WAIST_NOT_ABOVE_NECK,
}

/**
 * Note under the live body fat percentage: how it is estimated, or what it still needs. Without
 * it, the TDEE is computed with Mifflin-St Jeor.
 */
enum class BodyFatNote {
    /** No sex chosen yet; the formula depends on it. */
    SELECT_SEX,

    /** Some field has an error to fix first. */
    FIX_ERRORS,

    /** A man without neck or waist: US Navy needs both. */
    NEEDS_NECK_AND_WAIST,

    /** A woman without neck or waist: US Navy needs both, and the hip. */
    NEEDS_NECK_WAIST_AND_HIP,

    /** A woman with neck and waist but no hip. */
    NEEDS_HIP,

    /** The circumferences are there, but height or weight are missing. */
    NEEDS_HEIGHT_AND_WEIGHT,

    /** Estimated with US Navy; the TDEE will use Katch-McArdle. */
    US_NAVY,

    /** US Navy gives no valid percentage for these measurements. */
    NOT_ESTIMABLE,
}

/**
 * The calculator form as typed, and what follows from it.
 *
 * The numbers are kept as the text of each field, so that an unfinished entry such as "80," is
 * shown as typed. [fieldErrors], [bodyFatPercent], [bodyFatNote] and [canCalculate] are derived
 * from the other properties with [revalidated].
 *
 * @property sex chosen sex, or `null` while none is chosen.
 * @property ageYears age, within [BodyMeasurements.AGE_RANGE].
 * @property hip text of the hip field; only used for women.
 * @property fieldErrors error of each field whose text is not accepted. A field left empty has
 *   no error, even when it is required.
 * @property bodyFatPercent live US Navy estimate, or `null` when it cannot be computed.
 * @property bodyFatNote how [bodyFatPercent] is obtained or what it is missing.
 * @property canCalculate whether the form is complete and valid, so Calcular is enabled.
 */
data class CalculatorUiState(
    val sex: Sex? = null,
    val ageYears: Int = DEFAULT_AGE_YEARS,
    val height: String = "",
    val weight: String = "",
    val neck: String = "",
    val waist: String = "",
    val hip: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    val fieldErrors: Map<MeasurementField, FieldError> = emptyMap(),
    val bodyFatPercent: Double? = null,
    val bodyFatNote: BodyFatNote = BodyFatNote.SELECT_SEX,
    val canCalculate: Boolean = false,
) {

    /** Whether the hip field exists: only for women. */
    val showsHip: Boolean
        get() = sex == Sex.FEMALE

    /**
     * The measurements of the form, or `null` without a sex or a readable height and weight. The
     * hip is only kept for women. It does not check ranges: see [canCalculate].
     */
    fun toMeasurements(): BodyMeasurements? {
        val sex = sex ?: return null
        val heightCm = parseDecimal(height) ?: return null
        val weightKg = parseDecimal(weight) ?: return null
        return BodyMeasurements(
            sex = sex,
            ageYears = ageYears,
            heightCm = heightCm,
            weightKg = weightKg,
            neckCm = parseDecimal(neck),
            waistCm = parseDecimal(waist),
            hipCm = if (sex == Sex.FEMALE) parseDecimal(hip) else null,
        )
    }

    companion object {
        /** Age shown before the user changes it: a stepper always needs a value. */
        const val DEFAULT_AGE_YEARS = 30
    }
}

/**
 * This state with its derived properties computed again from the form fields.
 *
 * - A field with text that is not a number gets [FieldError.INVALID_NUMBER].
 * - The numbers go through [BodyMeasurements.validate]; its errors are shown only on fields with
 *   text, so a form that was just opened is not marked in red.
 * - Calcular needs a sex, a height and a weight, and no error at all.
 * - The body fat percentage is only estimated from measurements without errors.
 */
internal fun CalculatorUiState.revalidated(): CalculatorUiState {
    val texts = buildMap {
        put(MeasurementField.HEIGHT, height)
        put(MeasurementField.WEIGHT, weight)
        put(MeasurementField.NECK, neck)
        put(MeasurementField.WAIST, waist)
        if (showsHip) put(MeasurementField.HIP, hip)
    }
    val errors = mutableMapOf<MeasurementField, FieldError>()
    texts.forEach { (field, text) ->
        if (text.isNotBlank() && parseDecimal(text) == null) errors[field] = FieldError.INVALID_NUMBER
    }

    // Validate what can be read. An empty height or weight becomes NaN, whose error is dropped
    // below because the field has no text; the sex only matters for the hip.
    val probe = BodyMeasurements(
        sex = sex ?: Sex.MALE,
        ageYears = ageYears,
        heightCm = parseDecimal(height) ?: Double.NaN,
        weightKg = parseDecimal(weight) ?: Double.NaN,
        neckCm = parseDecimal(neck),
        waistCm = parseDecimal(waist),
        hipCm = if (showsHip) parseDecimal(hip) else null,
    )
    probe.validate().forEach { error ->
        val text = texts[error.field]
        if (!text.isNullOrBlank() && error.field !in errors) errors[error.field] = error.toFieldError()
    }

    val measurements = toMeasurements()
    val canCalculate = errors.isEmpty() && measurements != null && measurements.validate().isEmpty()
    val (bodyFatPercent, bodyFatNote) = bodyFatEstimate(errors.isNotEmpty(), measurements)
    return copy(
        fieldErrors = errors,
        bodyFatPercent = bodyFatPercent,
        bodyFatNote = bodyFatNote,
        canCalculate = canCalculate,
    )
}

private fun CalculatorUiState.bodyFatEstimate(
    hasErrors: Boolean,
    measurements: BodyMeasurements?,
): Pair<Double?, BodyFatNote> {
    val hasNeckAndWaist = neck.isNotBlank() && waist.isNotBlank()
    return when {
        sex == null -> null to BodyFatNote.SELECT_SEX
        hasErrors -> null to BodyFatNote.FIX_ERRORS
        sex == Sex.MALE && !hasNeckAndWaist -> null to BodyFatNote.NEEDS_NECK_AND_WAIST
        sex == Sex.FEMALE && !hasNeckAndWaist -> null to BodyFatNote.NEEDS_NECK_WAIST_AND_HIP
        sex == Sex.FEMALE && hip.isBlank() -> null to BodyFatNote.NEEDS_HIP
        measurements != null -> {
            val percent = BodyFatCalculator.usNavy(measurements)
            percent to if (percent != null) BodyFatNote.US_NAVY else BodyFatNote.NOT_ESTIMABLE
        }
        else -> null to BodyFatNote.NEEDS_HEIGHT_AND_WEIGHT
    }
}

private fun ValidationError.toFieldError(): FieldError = when (kind) {
    ValidationError.Kind.OUT_OF_RANGE -> FieldError.OUT_OF_RANGE
    ValidationError.Kind.WAIST_NOT_ABOVE_NECK -> FieldError.WAIST_NOT_ABOVE_NECK
}
