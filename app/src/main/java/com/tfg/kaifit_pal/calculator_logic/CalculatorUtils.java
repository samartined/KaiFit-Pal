package com.tfg.kaifit_pal.calculator_logic;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import java.text.DecimalFormat;

public class CalculatorUtils {

    private int age;
    private double weight, height, waist, hip, neck;
    private double fatPercentage;
    private boolean sex;


    public CalculatorUtils() {
    }

    // Constructor for the class with all the UI elements
    public CalculatorUtils(boolean sex, int age, double weight, double height, double neck, double waist, double hip) {

        this.sex = sex;
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.neck = neck;
        this.waist = waist;
        this.hip = hip;

    }

    // Constructor for the class without the age UI element, used only for % fat calculation
    public CalculatorUtils(boolean sex, double weight, double height, double neck, double waist, double hip) {

        this.sex = sex;
        this.weight = weight;
        this.height = height;
        this.neck = neck;
        this.waist = waist;
        this.hip = hip;

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

    public void setHeight(double height) {
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


    public double calculateFatPercentage(boolean sex) {
        // Calculate fat percentage
        double fatPercentage = 0;

        if (sex) {
            // Formula for masculine people
            fatPercentage = 86.010 * Math.log10(waist - neck) - 70.041 * Math.log10(height) + 36.76;
        } else {
            // Formula for female people
            fatPercentage = 163.205 * Math.log10(waist + hip - neck) - 97.684 * Math.log10(height) - 78.387;
        }
        return fatPercentage;
    }
}
