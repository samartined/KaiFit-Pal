package com.tfg.kaifit_pal.logic;

import java.util.HashMap;

/**
 * HashMap to store the macronutrient percentages for different modifiers.
 * The key is the modifier name and the value is an array of Double values representing the percentages for Proteins, Fats, and Carbs respectively.
 */
public class MacrosManager {

    private static final HashMap<String, Double[]> modifierMacrosPercentages = new HashMap<>();

    static {
        modifierMacrosPercentages.put("Mantenimiento", new Double[]{0.35, 0.25, 0.45});
        modifierMacrosPercentages.put("Definición", new Double[]{0.35, 0.25, 0.40});
        modifierMacrosPercentages.put("Volumen", new Double[]{0.25, 0.20, 0.55});
    }

    public static Double[] getMacrosPercentagesForModifier(String modifier) {
        return modifierMacrosPercentages.get(modifier);
    }
}