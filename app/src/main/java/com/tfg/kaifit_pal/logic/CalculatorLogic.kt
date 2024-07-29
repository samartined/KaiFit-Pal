package com.tfg.kaifit_pal.logic

import kotlin.math.log10

class CalculatorLogic private constructor() {
    private var age = 0
    private var weight = 0.0
    private var waist = 0.0
    private var hip = 0.0
    private var neck = 0.0
    var height: Double = 0.0
        private set
    var fatPercentage: Double = 0.0
        private set
    var activityFactor: Double = 0.0
    private var sex = false

    /**
     * Calculates the Total Daily Energy Expenditure (TDEE) for the person.
     * The calculation is based on the Katch-McArdle formula for Basal Metabolic Rate (BMR),
     * which is then multiplied by the activity factor to get the TDEE.
     *
     * @return The calculated TDEE.
     */
    fun calculateTDEE(): Int {
        val BMR = 370 + (21.6 * this.weight * (1 - this.fatPercentage / 100))

        return (BMR * this.activityFactor).toInt()
    }

    fun getAge(): Int {
        return age
    }

    fun setAge(age: Int) {
        this.age = age
    }

    fun getWeight(): Double {
        return weight
    }

    fun setWeight(weight: Double) {
        this.weight = weight
    }

    fun setHeight(height: Int) {
        this.height = height.toDouble()
    }

    fun getWaist(): Double {
        return waist
    }

    fun setWaist(waist: Double) {
        this.waist = waist
    }

    fun getHip(): Double {
        return hip
    }

    fun setHip(hip: Double) {
        this.hip = hip
    }

    fun getNeck(): Double {
        return neck
    }

    fun setNeck(neck: Double) {
        this.neck = neck
    }

    fun isSex(): Boolean {
        return sex
    }

    fun setSex(sex: Boolean) {
        this.sex = sex
    }

    companion object {
        fun createInstance(
            sex: Boolean,
            age: Int,
            height: Double,
            weight: Double,
            neck: Double,
            waist: Double,
            hip: Double
        ): CalculatorLogic {
            val instance = CalculatorLogic()
            instance.sex = sex
            instance.age = age
            instance.weight = weight
            instance.height = height
            instance.neck = neck
            instance.waist = waist
            instance.hip = hip
            instance.fatPercentage = calculateFatPercentage(sex, height, waist, neck, hip)
            return instance
        }

        fun calculateFatPercentage(
            sex: Boolean,
            height: Double,
            waist: Double,
            neck: Double,
            hip: Double
        ): Double {
            // We convert the values from cm to inches

            var height = height
            var waist = waist
            var neck = neck
            var hip = hip
            height /= 2.54
            neck /= 2.54
            waist /= 2.54
            hip /= 2.54

            // Calculate fat percentage
            val fatPercentage = 0.0

            return if (!sex) {
                // For males, the formula is: 86.010 * log10(waist - neck) - 70.041 * log10(height) + 36.76
                86.010 * log10(waist - neck) - 70.041 * log10(height) + 36.76
            } else {
                // For females, the formula is: 163.205 * log10(waist + hip - neck) - 97.684 * log10(height) - 78.387
                163.205 * log10(waist + hip - neck) - 97.684 * log10(height) - 78.387
            }
        }
    }
}