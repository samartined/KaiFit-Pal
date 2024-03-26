package com.tfg.kaifit_pal.logic;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashMap;

public class MacrosManager {


    // Default macronutrient percentages
    private final double defaultCarbPercentage = 0.55;
    private final double defaultProteinPercentage = 0.25;
    private final double defaultFatPercentage = 0.20;

    // Macronutrient percentages for different modifiers
    private final HashMap<String, Double[]> modifierMacrosPercentages = new HashMap<>();

    // Constructor
    public MacrosManager(Context context) {
        // Initialize modifier macros percentages
        modifierMacrosPercentages.put("Mantenimiento", new Double[]{0.55, 0.25, 0.20});
        modifierMacrosPercentages.put("Definición", new Double[]{0.40, 0.40, 0.20});
        modifierMacrosPercentages.put("Volumen", new Double[]{0.60, 0.20, 0.20});
    }

    // Method to get macronutrient percentages for a given modifier
    public Double[] getMacrosPercentagesForModifier(String modifier) {
        return modifierMacrosPercentages.get(modifier);
    }
}
