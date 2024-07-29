package com.tfg.kaifit_pal.logic

/**
 * HashMap to store the macronutrient percentages for different modifiers.
 * The key is the modifier name and the value is an array of Double values representing the percentages for Proteins, Fats, and Carbs respectively.
 */
object MacrosManager {
    private val modifierMacrosPercentages = HashMap<String, Array<Double>>()

    init {
        modifierMacrosPercentages["Mantenimiento"] =
            arrayOf(0.35, 0.25, 0.45)
        modifierMacrosPercentages["Definición"] =
            arrayOf(0.35, 0.25, 0.40)
        modifierMacrosPercentages["Volumen"] = arrayOf(0.25, 0.20, 0.55)
    }

    fun getMacrosPercentagesForModifier(modifier: String): Array<Double> {
        return modifierMacrosPercentages[modifier]!!
    }
}