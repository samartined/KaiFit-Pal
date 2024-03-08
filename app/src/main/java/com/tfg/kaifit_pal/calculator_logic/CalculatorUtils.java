package com.tfg.kaifit_pal.calculator_logic;

public class CalculatorUtils {

    private int age;
    private double weight, height, waist, hip, neck, activityFactor;
    private boolean sex;

    public CalculatorUtils() {
    }

    public CalculatorUtils(boolean sex, double weight, double height, double neck, double waist, double hip) {
        this.sex = sex;
        this.weight = weight;
        this.height = height;
        this.neck = neck;
        this.waist = waist;
        this.hip = hip;
    }

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

    public double getActivityFactor() {
        return activityFactor;
    }

    public void setActivityFactor(double activityFactor) {
        this.activityFactor = activityFactor;
    }

    public boolean isSex() {
        return sex;
    }

    public void setSex(boolean sex) {
        this.sex = sex;
    }
}