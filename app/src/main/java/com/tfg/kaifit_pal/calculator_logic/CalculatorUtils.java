package com.tfg.kaifit_pal.calculator_logic;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

public class CalculatorUtils extends BaseObservable {

    int age;
    double weight, height, waist, hip, neck;
    double fatPercentage;
    boolean male, female;

    public CalculatorUtils() {
    }

    // Constructor for the class with all the UI elements
    public CalculatorUtils(boolean isMale, boolean isFemale, int age, double weight, double height, double neck, double waist, double hip) {

        this.male = isMale;
        this.female = isFemale;
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.neck = neck;
        this.waist = waist;
        this.hip = hip;

    }

    // Constructor for the class without the age UI element, used only for % fat calculation
    public CalculatorUtils(boolean isMale, boolean isFemale, double weight, double height, double neck, double waist, double hip) {

        this.male = isMale;
        this.female = isFemale;
        this.weight = weight;
        this.height = height;
        this.neck = neck;
        this.waist = waist;
        this.hip = hip;

    }

    @Bindable
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Bindable
    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Bindable
    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    @Bindable
    public double getWaist() {
        return waist;
    }

    public void setWaist(double waist) {
        this.waist = waist;
    }

    @Bindable
    public double getHip() {
        return hip;
    }

    public void setHip(double hip) {
        this.hip = hip;
    }

    @Bindable
    public double getNeck() {
        return neck;
    }

    public void setNeck(double neck) {
        this.neck = neck;
    }

    @Bindable
    public double getFatPercentage() {
        return fatPercentage;
    }

    public void setFatPercentage(double fatPercentage) {
        this.fatPercentage = fatPercentage;
    }


    public double calculateFatPercentage(boolean isMale, boolean isFemale) {
        // Calculate fat percentage
        double fatPercentage = 0;

        if (isMale) {
            // Formula for masculine people
            fatPercentage = 86.010 * Math.log10(waist - neck) - 70.041 * Math.log10(height) + 36.76;
        } else if (isFemale) {
            // Formula for female people
            fatPercentage = 163.205 * Math.log10(waist + hip - neck) - 97.684 * Math.log10(height) - 78.387;
        }
        return fatPercentage;
    }
}
