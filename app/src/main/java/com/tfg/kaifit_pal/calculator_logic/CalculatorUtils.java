package com.tfg.kaifit_pal.calculator_logic;

import android.widget.EditText;
import android.widget.TextView;

public class CalculatorUtils {

    TextView inputAge;
    EditText inputWeight, inputHeight, inputWaist, inputHip, inputNeck;
    double fatPercentage;
    boolean isMale, isFemale;

    public CalculatorUtils(TextView age, EditText weight, EditText height, EditText neck, EditText waist, EditText hip, boolean isMale, boolean isFemale) {

        this.inputAge = age;
        this.inputWeight = weight;
        this.inputHeight = height;
        this.inputNeck = neck;
        this.inputWaist = waist;
        this.inputHip = hip;
        this.isMale = isMale;
        this.isFemale = isFemale;
    }

    public double calculateFatPercentage(boolean isMale, boolean isFemale, EditText weight, EditText height, EditText neck, EditText waist, EditText hip, TextView age) {

        double weightValue = Double.parseDouble(weight.getText().toString());
        double heightValue = Double.parseDouble(height.getText().toString());
        double neckValue = Double.parseDouble(neck.getText().toString());
        double waistValue = Double.parseDouble(waist.getText().toString());
        double hipValue = Double.parseDouble(hip.getText().toString());
        double ageValue = Double.parseDouble(age.getText().toString());

        double fatPercentage = 0;

        if (isMale) {
            // Fórmula para hombres
            fatPercentage = 86.010 * Math.log10(waistValue - neckValue) - 70.041 * Math.log10(heightValue) + 36.76;
        } else if (isFemale) {
            // Fórmula para mujeres
            fatPercentage = 163.205 * Math.log10(waistValue + hipValue - neckValue) - 97.684 * Math.log10(heightValue) - 78.387;
        }
        return fatPercentage;
    }
}
