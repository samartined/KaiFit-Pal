package com.tfg.kaifit_pal.logic;

import androidx.annotation.NonNull;

/**
 * This class contains the logic to calculate the TDEE, the fat percentage and the macros.
 * It uses the Katch-McArdle formula to calculate the BMR and then the TDEE.
 * It also uses the Katch-McArdle formula to calculate the fat percentage.
 * Finally, it calculates the macros using the TDEE.
 * This class uses the Singleton pattern to avoid creating multiple instances of it.
 */
public class CalculatorLogic {

    private int age;
    private double weight, waist, hip, neck, height;
    private double fatPercentage, activityFactor;
    private boolean sex;

    /**
     * We make the constructor private to avoid creating instances of this class.
     */
    private CalculatorLogic() {
    }

    /**
     * Creates an instance of the CalculatorLogic class with the provided parameters.
     *
     * @param sex    The sex of the person. True for female, false for male.
     * @param age    The age of the person in years.
     * @param height The height of the person in centimeters.
     * @param weight The weight of the person in kilograms.
     * @param neck   The neck measurement of the person in centimeters.
     * @param waist  The waist measurement of the person in centimeters.
     * @param hip    The hip measurement of the person in centimeters.
     * @return A new instance of the CalculatorLogic class with the provided parameters and the calculated fat percentage.
     */
    @NonNull
    public static CalculatorLogic createInstance(boolean sex, int age, double height, double weight, double neck, double waist, double hip) {
        CalculatorLogic instance = new CalculatorLogic();
        instance.sex = sex;
        instance.age = age;
        instance.weight = weight;
        instance.height = height;
        instance.neck = neck;
        instance.waist = waist;
        instance.hip = hip;
        instance.fatPercentage = calculateFatPercentage(sex, height, waist, neck, hip);
        return instance;
    }

    /**
     * Calculates the body fat percentage based on the given measurements.
     * The measurements are first converted from centimeters to inches.
     * The body fat percentage is then calculated using different formulas for males and females.
     */
    public static double calculateFatPercentage(boolean sex, double height, double waist, double neck, double hip) {

        // We convert the values from cm to inches
        height /= 2.54;
        neck /= 2.54;
        waist /= 2.54;
        hip /= 2.54;

        // Calculate fat percentage
        double fatPercentage = 0;

        if (!sex) {
            // For males, the formula is: 86.010 * log10(waist - neck) - 70.041 * log10(height) + 36.76
            return 86.010 * Math.log10(waist - neck) - 70.041 * Math.log10(height) + 36.76;
        } else {
            // For females, the formula is: 163.205 * log10(waist + hip - neck) - 97.684 * log10(height) - 78.387
            return 163.205 * Math.log10(waist + hip - neck) - 97.684 * Math.log10(height) - 78.387;
        }
    }

    /**
     * Calculates the Total Daily Energy Expenditure (TDEE) for the person.
     * The calculation is based on the Katch-McArdle formula for Basal Metabolic Rate (BMR),
     * which is then multiplied by the activity factor to get the TDEE.
     *
     * @return The calculated TDEE.
     */
    public int calculateTDEE() {

        // We'll use the Katch-McArdle formula to calculate the BMR.
        double BMR = 370 + (21.6 * this.weight * (1 - this.fatPercentage / 100));

        // Now, we calculate the TDEE using the BMR and the activity factor
        return (int) (BMR * this.activityFactor);
    }

    /*
    GETTERS AND SETTERS
     */
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public double getWaist() {
        return waist;
    }

    public void setWaist(double waist) {
        this.waist = waist;
    }

    public double getHip() {
        return hip;
    }

    public void setHip(double hip) {
        this.hip = hip;
    }

    public double getNeck() {
        return neck;
    }

    public void setNeck(double neck) {
        this.neck = neck;
    }

    public boolean isSex() {
        return sex;
    }

    public void setSex(boolean sex) {
        this.sex = sex;
    }

    public double getActivityFactor() {
        return activityFactor;
    }

    public void setActivityFactor(double activityFactor) {
        this.activityFactor = activityFactor;
    }

    public double getFatPercentage() {
        return fatPercentage;
    }
}