package com.tfg.kaifit_pal.logic;

import java.util.HashMap;

/**
 * Class to manage macronutrient percentages for different modifiers.
 */
public class MacrosManager {

    /**
     * HashMap to store the macronutrient percentages for different modifiers.
     * The key is the modifier name and the value is an array of Double values representing the percentages for Proteins, Fats, and Carbs respectively.
     */
    private static final HashMap<String, Double[]> modifierMacrosPercentages = new HashMap<>();

    static {
        // Initialize modifier macros percentages, in this order: Proteins, Fats, Carbs. The key is modifier name - value.
        modifierMacrosPercentages.put("Mantenimiento", new Double[]{0.35, 0.25, 0.45});
        modifierMacrosPercentages.put("Definición", new Double[]{0.35, 0.25, 0.40});
        modifierMacrosPercentages.put("Volumen", new Double[]{0.25, 0.20, 0.55});
    }

    /**
     * Method to get the macronutrient percentages for a given modifier.
     *
     * @param modifier The modifier for which to get the macronutrient percentages.
     * @return An array of Double values representing the percentages for Proteins, Fats, and Carbs respectively.
     */
    public static Double[] getMacrosPercentagesForModifier(String modifier) {
        return modifierMacrosPercentages.get(modifier);
    }
}