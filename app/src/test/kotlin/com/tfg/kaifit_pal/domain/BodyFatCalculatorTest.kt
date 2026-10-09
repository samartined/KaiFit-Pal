package com.tfg.kaifit_pal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BodyFatCalculatorTest {

    private val male180 = BodyMeasurements(
        sex = Sex.MALE,
        ageYears = 30,
        heightCm = 180.0,
        weightKg = 80.0,
        neckCm = 38.0,
        waistCm = 85.0,
    )

    private val female165 = BodyMeasurements(
        sex = Sex.FEMALE,
        ageYears = 30,
        heightCm = 165.0,
        weightKg = 60.0,
        neckCm = 33.0,
        waistCm = 70.0,
        hipCm = 95.0,
    )

    private fun usNavyOf(m: BodyMeasurements): Double {
        val percent = BodyFatCalculator.usNavy(m)
        assertNotNull("US Navy body fat expected for $m", percent)
        return percent!!
    }

    @Test
    fun usNavyMale180Waist85Neck38Is16_15() {
        assertEquals(16.15, usNavyOf(male180), 0.005)
    }

    @Test
    fun usNavyFemale165Waist70Neck33Hip95Is24_56() {
        assertEquals(24.56, usNavyOf(female165), 0.005)
    }

    @Test
    fun usNavyMale175Waist80Neck40Is10_98() {
        val m = male180.copy(heightCm = 175.0, waistCm = 80.0, neckCm = 40.0)
        assertEquals(10.98, usNavyOf(m), 0.005)
    }

    @Test
    fun usNavyFemale170Waist75Neck34Hip100Is27_97() {
        val m = female165.copy(heightCm = 170.0, waistCm = 75.0, neckCm = 34.0, hipCm = 100.0)
        assertEquals(27.97, usNavyOf(m), 0.005)
    }

    @Test
    fun usNavyMaleIgnoresHip() {
        assertEquals(16.15, usNavyOf(male180.copy(hipCm = 120.0)), 0.005)
    }

    @Test
    fun usNavyMaleWaistEqualToNeckIsNull() {
        assertNull(BodyFatCalculator.usNavy(male180.copy(waistCm = 38.0)))
    }

    @Test
    fun usNavyMaleWaistBelowNeckIsNull() {
        assertNull(BodyFatCalculator.usNavy(male180.copy(waistCm = 30.0)))
    }

    @Test
    fun usNavyHeightZeroIsNull() {
        assertNull(BodyFatCalculator.usNavy(male180.copy(heightCm = 0.0)))
    }

    @Test
    fun usNavyNeckZeroIsNull() {
        assertNull(BodyFatCalculator.usNavy(male180.copy(neckCm = 0.0)))
    }

    @Test
    fun usNavyWaistZeroIsNull() {
        assertNull(BodyFatCalculator.usNavy(male180.copy(waistCm = 0.0)))
    }

    @Test
    fun usNavyFemaleWithoutHipIsNull() {
        assertNull(BodyFatCalculator.usNavy(female165.copy(hipCm = null)))
    }

    @Test
    fun usNavyFemaleHipZeroIsNull() {
        assertNull(BodyFatCalculator.usNavy(female165.copy(hipCm = 0.0)))
    }

    @Test
    fun usNavyMaleWithoutNeckIsNull() {
        assertNull(BodyFatCalculator.usNavy(male180.copy(neckCm = null)))
    }

    @Test
    fun usNavyMaleWithoutWaistIsNull() {
        assertNull(BodyFatCalculator.usNavy(male180.copy(waistCm = null)))
    }

    @Test
    fun usNavyNaNHeightIsNull() {
        assertNull(BodyFatCalculator.usNavy(male180.copy(heightCm = Double.NaN)))
    }

    @Test
    fun usNavyInfiniteWaistIsNull() {
        assertNull(BodyFatCalculator.usNavy(female165.copy(waistCm = Double.POSITIVE_INFINITY)))
    }

    @Test
    fun usNavyResultNotAboveZeroIsNull() {
        // Waist 1 cm above the neck: the formula gives about -127.7 %.
        assertNull(BodyFatCalculator.usNavy(male180.copy(waistCm = 39.0)))
    }

    @Test
    fun usNavyResultNotBelowHundredIsNull() {
        // Height 100 cm, waist 200, hip 200 and neck 20: the formula gives about 120.8 %.
        val m = female165.copy(heightCm = 100.0, neckCm = 20.0, waistCm = 200.0, hipCm = 200.0)
        assertNull(BodyFatCalculator.usNavy(m))
    }
}
