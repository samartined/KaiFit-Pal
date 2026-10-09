package com.tfg.kaifit_pal.domain

import kotlin.math.log10

/**
 * Body fat percentage from body circumferences, with the US Navy equations of Hodgdon & Beckett
 * (Naval Health Research Center, 1984), in their inch form. Every length is converted from cm to
 * inches (÷ 2.54) before applying them:
 *
 * - men: `86.010 × log10(waist − neck) − 70.041 × log10(height) + 36.76`;
 * - women: `163.205 × log10(waist + hip − neck) − 97.684 × log10(height) − 78.387`.
 *
 * This is the variant of the original app. It does not depend on the validation ranges of
 * [BodyMeasurements.validate].
 */
object BodyFatCalculator {

    private const val CM_PER_INCH = 2.54

    /**
     * Returns the body fat percentage, unrounded, or `null` when it cannot be estimated:
     * a needed measurement is missing (neck and waist, plus hip for women), any needed length
     * (height included) is not positive or not finite, the logarithm argument is not positive,
     * or the result is not finite or outside (0, 100).
     */
    fun usNavy(m: BodyMeasurements): Double? {
        val neckCm = m.neckCm ?: return null
        val waistCm = m.waistCm ?: return null
        val percent = when (m.sex) {
            Sex.MALE -> {
                if (!allPositiveAndFinite(m.heightCm, neckCm, waistCm)) return null
                val logArgument = inches(waistCm) - inches(neckCm)
                if (logArgument <= 0.0) return null
                86.010 * log10(logArgument) - 70.041 * log10(inches(m.heightCm)) + 36.76
            }

            Sex.FEMALE -> {
                val hipCm = m.hipCm ?: return null
                if (!allPositiveAndFinite(m.heightCm, neckCm, waistCm, hipCm)) return null
                val logArgument = inches(waistCm) + inches(hipCm) - inches(neckCm)
                if (logArgument <= 0.0) return null
                163.205 * log10(logArgument) - 97.684 * log10(inches(m.heightCm)) - 78.387
            }
        }
        return percent.takeIf { it.isFinite() && it > 0.0 && it < 100.0 }
    }

    private fun inches(cm: Double): Double = cm / CM_PER_INCH

    private fun allPositiveAndFinite(vararg values: Double): Boolean = values.all { it.isFinite() && it > 0.0 }
}
