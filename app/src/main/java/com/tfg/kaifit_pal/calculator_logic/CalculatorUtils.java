package com.tfg.kaifit_pal.calculator_logic;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.text.DecimalFormat;

public class CalculatorUtils {

    private int age;
    private double weight, waist, hip, neck, height;
    private double fatPercentage, activityFactor;
    private boolean sex;


    public CalculatorUtils() {
    }

    // Constructor for the class with all the UI elements
    public CalculatorUtils(boolean sex, int age, double height, double weight, double neck, double waist, double hip, double fatPercentage, double activityFactor) {
        this.sex = sex;
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.neck = neck;
        this.waist = waist;
        this.hip = hip;
        this.fatPercentage = calculateFatPercentage();
        this.activityFactor = activityFactor;
    }

    // Constructor for the class without the age UI element, used only for % fat calculation.
    public CalculatorUtils(boolean sex, int height, double weight, double neck, double waist, double hip) {
        this.sex = sex;
        this.weight = weight;
        this.height = height;
        this.neck = neck;
        this.waist = waist;
        this.hip = hip;
        this.fatPercentage = calculateFatPercentage();
    }

    public double calculateFatPercentage() {

        // We convert the values from cm to inches
        this.height = this.height / 2.54;
        this.neck = this.neck / 2.54;
        this.waist = this.waist / 2.54;
        this.hip = this.hip / 2.54;


        // Calculate fat percentage
        double fatPercentage = 0;

        if (!sex) {
            return 86.010 * Math.log10(this.waist - this.neck) - 70.041 * Math.log10(this.height) + 36.76; // Male
        } else {
            return 163.205 * Math.log10(this.waist + this.hip - this.neck) - 97.684 * Math.log10(this.height) - 78.387; // Female
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

    public double getFatPercentage() {
        return fatPercentage;
    }

    public void setFatPercentage(double fatPercentage) {
        this.fatPercentage = fatPercentage;
    }
}