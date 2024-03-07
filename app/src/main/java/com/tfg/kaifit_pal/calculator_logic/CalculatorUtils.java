package com.tfg.kaifit_pal.calculator_logic;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import com.tfg.kaifit_pal.BR;

public class CalculatorUtils extends BaseObservable {

    private int age;
    private double weight, height, waist, hip, neck, fatPercentage;

    private String heightString, weightString, waistString, hipString, neckString; // We need the string values of the variables to use them in bidirectional data binding
    private boolean sex;

    public CalculatorUtils() {
    }

    // Constructor for the class with all the UI elements
    public CalculatorUtils(boolean sex, int age, double weight, double height, double neck, double waist, double hip, double fatPercentage) {

        this.sex = sex;
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.neck = neck;
        this.waist = waist;
        this.hip = hip;
        this.fatPercentage = 0;

        this.heightString = String.valueOf(height);
        this.weightString = String.valueOf(weight);
        this.waistString = String.valueOf(waist);
        this.hipString = String.valueOf(hip);
        this.neckString = String.valueOf(neck);

    }

    // Constructor for the class without the age UI element, used only for % fat calculation
    public CalculatorUtils(boolean sex, double weight, double height, double neck, double waist, double hip) {

        this.sex = sex;
        this.weight = weight;
        this.height = height;
        this.neck = neck;
        this.waist = waist;
        this.hip = hip;

        this.heightString = String.valueOf(height);
        this.weightString = String.valueOf(weight);
        this.waistString = String.valueOf(waist);
        this.hipString = String.valueOf(hip);
        this.neckString = String.valueOf(neck);

    }

    /**
     * Here we have the getters and setters for the class.
     * We use the @Bindable annotation to notify the UI when the value of the variable changes, and we use the notifyPropertyChanged method to notify the UI when the value of the variable changes.
     * Also, we includes string getters and setters to get the value of the variables as a string as we need them in bidirectional data binding in the UI.
     */
    @Bindable
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
        notifyPropertyChanged(BR.age);
    }

    @Bindable
    public double getWeight() {
        return weight;
    }

    @Bindable
    public String getHeightAsString() {
        return heightString;
    }

    public void setHeightAsString(String heightAsString) {
        this.heightString = heightAsString;
        notifyPropertyChanged(BR.heightAsString);
    }

    public void setWeight(double weight) {
        this.weight = weight;
        setHeightAsString(String.valueOf(height));
        notifyPropertyChanged(BR.weight);
    }

    @Bindable
    public double getHeight() {
        return height;
    }

    @Bindable
    public String getWeightAsString() {
        return weightString;
    }

    public void setWeightAsString(String weightAsString) {
        this.weightString = weightAsString;
        notifyPropertyChanged(BR.weightAsString);
    }

    public void setHeight(double height) {
        this.height = height;
        setHeightAsString(String.valueOf(height));
        notifyPropertyChanged(BR.height);
    }

    @Bindable
    public double getWaist() {
        return waist;
    }

    @Bindable
    public String getWaistAsString() {
        return waistString;
    }

    public void setWaistAsString(String waistAsString) {
        this.waistString = waistAsString;
        notifyPropertyChanged(BR.waistAsString);
    }

    public void setWaist(double waist) {
        this.waist = waist;
        setWaistAsString(String.valueOf(waist));
        notifyPropertyChanged(BR.waist);
    }

    @Bindable
    public double getHip() {
        return hip;
    }

    @Bindable
    public String getHipAsString() {
        return hipString;
    }

    public void setHipAsString(String hipAsString) {
        this.hipString = hipAsString;
        notifyPropertyChanged(BR.hipAsString);
    }

    public void setHip(double hip) {
        this.hip = hip;
        setHipAsString(String.valueOf(hip));
        notifyPropertyChanged(BR.hip);
    }

    @Bindable
    public double getNeck() {
        return neck;
    }

    @Bindable
    public String getNeckAsString() {
        return neckString;
    }

    public void setNeckAsString(String neckAsString) {
        this.neckString = neckAsString;
        notifyPropertyChanged(BR.neckAsString);
    }

    public void setNeck(double neck) {
        this.neck = neck;
        setNeckAsString(String.valueOf(neck));
        notifyPropertyChanged(BR.neck);
    }

    @Bindable
    public boolean getSex() {
        return sex;
    }

    public void setSex(boolean sex) {
        this.sex = sex;
        notifyPropertyChanged(BR.sex);
    }

    @Bindable
    public double getFatPercentage() {
        return fatPercentage;
    }

    public void setFatPercentage(double fatPercentage) {
        this.fatPercentage = fatPercentage;
        notifyPropertyChanged(BR.fatPercentage);
    }

    public double calculateFatPercentage(boolean sex) {
        // Calculate fat percentage
        double fatPercentage = 0;

        if (sex) {
            // Formula for masculine sex
            fatPercentage = 86.010 * Math.log10(waist - neck) - 70.041 * Math.log10(height) + 36.76;
        } else {
            // Formula for female sex
            fatPercentage = 163.205 * Math.log10(waist + hip - neck) - 97.684 * Math.log10(height) - 78.387;
        }
        return fatPercentage;
    }
}