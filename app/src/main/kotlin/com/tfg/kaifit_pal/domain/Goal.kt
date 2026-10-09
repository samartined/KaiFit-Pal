package com.tfg.kaifit_pal.domain

/**
 * Nutrition goal and its default macronutrient split, as whole percentages of the target
 * calories that always add up to 100.
 *
 * @property proteinPercent share of the calories from protein, in %.
 * @property fatPercent share of the calories from fat, in %.
 * @property carbPercent share of the calories from carbohydrates, in %.
 */
enum class Goal(val proteinPercent: Int, val fatPercent: Int, val carbPercent: Int) {
    DEFICIT(proteinPercent = 35, fatPercent = 25, carbPercent = 40),
    MAINTENANCE(proteinPercent = 30, fatPercent = 25, carbPercent = 45),
    SURPLUS(proteinPercent = 25, fatPercent = 20, carbPercent = 55),
    ;

    init {
        require(proteinPercent + fatPercent + carbPercent == 100) {
            "Macro split of $name must add up to 100 %"
        }
    }
}

/**
 * Size of the calorie adjustment of a [GoalModifier], regardless of its sign. Entries are
 * ordered by step: the n-th one stands for |percent| = 5 × n.
 *
 * The visible label is the pair [Goal] × [GoalIntensity]. It maps to the nine labels of the
 * original app (`TdeeMacros.java:237-249` on `backup/original-java`) as follows:
 *
 * | percent | [Goal]        | [GoalIntensity] | Original label             |
 * |--------:|---------------|-----------------|----------------------------|
 * |     −20 | `DEFICIT`     | `HIGH`          | "Déficit intenso"          |
 * |     −15 | `DEFICIT`     | `MODERATE_HIGH` | "Déficit moderado-intenso" |
 * |     −10 | `DEFICIT`     | `MODERATE`      | "Déficit moderado"         |
 * |      −5 | `DEFICIT`     | `LIGHT`         | "Déficit ligero"           |
 * |       0 | `MAINTENANCE` | `NONE`          | "Mantenimiento"            |
 * |      +5 | `SURPLUS`     | `LIGHT`         | "Volumen ligero"           |
 * |     +10 | `SURPLUS`     | `MODERATE`      | "Volumen moderado"         |
 * |     +15 | `SURPLUS`     | `MODERATE_HIGH` | "Volumen moderado-intenso" |
 * |     +20 | `SURPLUS`     | `HIGH`          | "Volumen intenso"          |
 */
enum class GoalIntensity { NONE, LIGHT, MODERATE, MODERATE_HIGH, HIGH }
