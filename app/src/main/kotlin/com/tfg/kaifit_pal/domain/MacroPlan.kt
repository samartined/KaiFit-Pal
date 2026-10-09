package com.tfg.kaifit_pal.domain

import kotlin.math.roundToInt

/**
 * Daily macronutrients, in grams, for a calorie target in kcal/day, with the Atwater factors
 * of 4 kcal/g for protein and carbohydrates and 9 kcal/g for fat.
 *
 * Carbohydrates are always the remainder, `round((target − 4 × protein − 9 × fat) / 4)`, never
 * below 0. Protein and fat are capped so that this remainder never goes negative, which keeps
 * [totalKcal] within [targetKcal] ± 2 kcal.
 *
 * @property targetKcal calorie target, in kcal/day. Must be positive.
 * @property proteinGrams protein, in g/day.
 * @property fatGrams fat, in g/day.
 * @property carbGrams carbohydrates, in g/day.
 */
data class MacroPlan(
    val targetKcal: Int,
    val proteinGrams: Int,
    val fatGrams: Int,
    val carbGrams: Int,
) {

    init {
        require(targetKcal > 0) { "Target calories must be positive, was $targetKcal" }
        require(proteinGrams >= 0 && fatGrams >= 0 && carbGrams >= 0) {
            "Grams must not be negative: protein $proteinGrams, fat $fatGrams, carbs $carbGrams"
        }
    }

    /** Calories of the three macronutrients, in kcal/day. */
    val totalKcal: Int
        get() = KCAL_PER_GRAM_PROTEIN * proteinGrams + KCAL_PER_GRAM_FAT * fatGrams + KCAL_PER_GRAM_CARB * carbGrams

    /** Most protein, in g, that fits in [targetKcal] next to the current fat (integer division). */
    val maxProteinGrams: Int
        get() = ((targetKcal - KCAL_PER_GRAM_FAT * fatGrams) / KCAL_PER_GRAM_PROTEIN).coerceAtLeast(0)

    /** Most fat, in g, that fits in [targetKcal] next to the current protein (integer division). */
    val maxFatGrams: Int
        get() = ((targetKcal - KCAL_PER_GRAM_PROTEIN * proteinGrams) / KCAL_PER_GRAM_FAT).coerceAtLeast(0)

    /** Sets protein to [grams], limited to 0..[maxProteinGrams], keeps fat and recomputes carbohydrates. */
    fun withProteinGrams(grams: Int): MacroPlan {
        val protein = grams.coerceIn(0, maxProteinGrams)
        return copy(proteinGrams = protein, carbGrams = remainingCarbGrams(targetKcal, protein, fatGrams))
    }

    /** Sets fat to [grams], limited to 0..[maxFatGrams], keeps protein and recomputes carbohydrates. */
    fun withFatGrams(grams: Int): MacroPlan {
        val fat = grams.coerceIn(0, maxFatGrams)
        return copy(fatGrams = fat, carbGrams = remainingCarbGrams(targetKcal, proteinGrams, fat))
    }

    companion object {
        const val KCAL_PER_GRAM_PROTEIN = 4
        const val KCAL_PER_GRAM_FAT = 9
        const val KCAL_PER_GRAM_CARB = 4

        /**
         * Default plan for [goal]: protein and fat from its percentages of [targetKcal], each
         * rounded to the nearest gram, and carbohydrates as the remainder.
         *
         * @throws IllegalArgumentException if [targetKcal] is not positive.
         */
        fun forGoal(targetKcal: Int, goal: Goal): MacroPlan {
            val protein = (targetKcal * goal.proteinPercent / (100.0 * KCAL_PER_GRAM_PROTEIN)).roundToInt()
            val fat = (targetKcal * goal.fatPercent / (100.0 * KCAL_PER_GRAM_FAT)).roundToInt()
            return MacroPlan(targetKcal, protein, fat, remainingCarbGrams(targetKcal, protein, fat))
        }

        private fun remainingCarbGrams(targetKcal: Int, proteinGrams: Int, fatGrams: Int): Int {
            val remainingKcal = targetKcal - KCAL_PER_GRAM_PROTEIN * proteinGrams - KCAL_PER_GRAM_FAT * fatGrams
            return (remainingKcal / KCAL_PER_GRAM_CARB.toDouble()).roundToInt().coerceAtLeast(0)
        }
    }
}
