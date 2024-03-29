package com.tfg.kaifit_pal.calculator_logic;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.io.Serializable;
import java.security.Provider;
import java.text.DecimalFormat;

public class CalculatorUtils implements Serializable {

    private int age;
    private double weight, waist, hip, neck, height;
    private double fatPercentage, activityFactor;
    private boolean sex;

    // Static factory method
    @NonNull
    public static CalculatorUtils createInstance(boolean sex, int age, double height, double weight, double neck, double waist, double hip) {
        CalculatorUtils instance = new CalculatorUtils();
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

    public static double calculateFatPercentage(boolean sex, double height, double waist, double neck, double hip) {

        // We convert the values from cm to inches
        height /= 2.54;
        neck /= 2.54;
        waist /= 2.54;
        hip /= 2.54;


        // Calculate fat percentage
        double fatPercentage = 0;

        if (!sex) {
            return 86.010 * Math.log10(waist - neck) - 70.041 * Math.log10(height) + 36.76; // Male
        } else {
            return 163.205 * Math.log10(waist + hip - neck) - 97.684 * Math.log10(height) - 78.387; // Female
        }
    }

    public int calculateTDEE() {

        // We'll use the Katch-McArdle formula to calculate the BMR.
        double BMR = 370 + (21.6 * this.weight * (1 - this.fatPercentage / 100));

        // Now, we calculate the TDEE using the BMR and the activity factor
        return (int) (BMR * this.activityFactor);
    }

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