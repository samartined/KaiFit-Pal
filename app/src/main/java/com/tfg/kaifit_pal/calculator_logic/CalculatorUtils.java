package com.tfg.kaifit_pal.calculator_logic;

public class CalculatorUtils {

    private int age;
    private double weight, height, waist, hip, neck, activityFactor;
    private boolean sex;

    public CalculatorUtils(boolean sex, int age, double weight, double height, double neck, double waist, double hip, double activityFactor) {
        this.sex = sex;
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.neck = neck;
        this.waist = waist;
        this.hip = hip;
        this.activityFactor = activityFactor;
    }

    public double calculateFatPercentage() {
        return sex ? 86.010 * Math.log10(waist - neck) - 70.041 * Math.log10(height) + 36.76
                : 163.205 * Math.log10(waist + hip - neck) - 97.684 * Math.log10(height) - 78.387;
    }
}