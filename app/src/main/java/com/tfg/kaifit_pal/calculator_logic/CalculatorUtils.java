package com.tfg.kaifit_pal.calculator_logic;

import android.widget.EditText;
import android.widget.TextView;

public class CalculatorUtils {

    int inputAge;
    double inputWeight, inputHeight, inputWaist, inputHip, inputNeck;
    double fatPercentage;
    boolean isMale, isFemale;

    // Empty constructor
    public CalculatorUtils() {

    }

    // Constructor for the class with all the UI elements
    public CalculatorUtils(boolean isMale, boolean isFemale, int age, double weight, double height, double neck, double waist, double hip) {

        this.isMale = isMale;
        this.isFemale = isFemale;
        this.inputAge = age;
        this.inputWeight = weight;
        this.inputHeight = height;
        this.inputNeck = neck;
        this.inputWaist = waist;
        this.inputHip = hip;

    }

    // Constructor for the class without the age UI element, used only for % fat calculation
    public CalculatorUtils(boolean isMale, boolean isFemale, double weight, double height, double neck, double waist, double hip) {

        this.isMale = isMale;
        this.isFemale = isFemale;
        this.inputWeight = weight;
        this.inputHeight = height;
        this.inputNeck = neck;
        this.inputWaist = waist;
        this.inputHip = hip;

    }

    public double calculateFatPercentage(boolean isMale, boolean isFemale, double weight, double height, double neck, double waist, double hip) {
        // Calculate fat percentage
        double fatPercentage = 0;

        if (isMale) {
            // Fórmula para hombres
            fatPercentage = 86.010 * Math.log10(waist - neck) - 70.041 * Math.log10(height) + 36.76;
        } else if (isFemale) {
            // Fórmula para mujeres
            fatPercentage = 163.205 * Math.log10(waist + hip - neck) - 97.684 * Math.log10(height) - 78.387;
        }
        return fatPercentage;
    }
}
