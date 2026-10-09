package com.tfg.kaifit_pal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TdeeCalculatorTest {

    private val maleWithCircumferences = BodyMeasurements(
        sex = Sex.MALE,
        ageYears = 30,
        heightCm = 180.0,
        weightKg = 80.0,
        neckCm = 38.0,
        waistCm = 85.0,
    )

    private val femaleWithCircumferences = BodyMeasurements(
        sex = Sex.FEMALE,
        ageYears = 30,
        heightCm = 165.0,
        weightKg = 60.0,
        neckCm = 33.0,
        waistCm = 70.0,
        hipCm = 95.0,
    )

    private fun calculateOrFail(m: BodyMeasurements, activityLevel: ActivityLevel): TdeeResult {
        val result = TdeeCalculator.calculate(m, activityLevel)
        assertNotNull("TDEE expected for $m", result)
        return result!!
    }

    @Test
    fun katchMcArdleBmr80Kg20PercentIs1752_4() {
        assertEquals(1752.4, TdeeCalculator.katchMcArdleBmr(80.0, 20.0), 1e-9)
    }

    @Test
    fun katchMcArdleTdeeModerateFromBmr1752_4Is2716() {
        assertEquals(2716, TdeeCalculator.tdeeKcal(1752.4, ActivityLevel.MODERATE))
    }

    @Test
    fun mifflinStJeorBmrMale30Years180Cm80KgIs1780() {
        assertEquals(1780.0, TdeeCalculator.mifflinStJeorBmr(Sex.MALE, 30, 180.0, 80.0), 1e-9)
    }

    @Test
    fun mifflinStJeorTdeeModerateFromBmr1780Is2759() {
        val bmr = TdeeCalculator.mifflinStJeorBmr(Sex.MALE, 30, 180.0, 80.0)
        assertEquals(2759, TdeeCalculator.tdeeKcal(bmr, ActivityLevel.MODERATE))
    }

    @Test
    fun mifflinStJeorBmrFemale30Years165Cm60KgIs1320_25() {
        assertEquals(1320.25, TdeeCalculator.mifflinStJeorBmr(Sex.FEMALE, 30, 165.0, 60.0), 1e-9)
    }

    @Test
    fun mifflinStJeorTdeeSedentaryFromBmr1320_25Is1584() {
        val bmr = TdeeCalculator.mifflinStJeorBmr(Sex.FEMALE, 30, 165.0, 60.0)
        assertEquals(1584, TdeeCalculator.tdeeKcal(bmr, ActivityLevel.SEDENTARY))
    }

    @Test
    fun calculateMaleWithNeckAndWaistUsesKatchMcArdleAndGives2819() {
        val result = calculateOrFail(maleWithCircumferences, ActivityLevel.MODERATE)
        assertEquals(TdeeMethod.KATCH_MCARDLE, result.method)
        assertEquals(2819, result.tdeeKcal)
        assertEquals(16.15, result.bodyFatPercent!!, 0.005)
    }

    @Test
    fun calculateCompleteFemaleUsesKatchMcArdleAndGives1617() {
        val result = calculateOrFail(femaleWithCircumferences, ActivityLevel.SEDENTARY)
        assertEquals(TdeeMethod.KATCH_MCARDLE, result.method)
        assertEquals(1617, result.tdeeKcal)
        assertEquals(24.56, result.bodyFatPercent!!, 0.005)
    }

    @Test
    fun calculateMaleWithoutCircumferencesUsesMifflinStJeorAndGives2759() {
        val m = maleWithCircumferences.copy(neckCm = null, waistCm = null)
        val result = calculateOrFail(m, ActivityLevel.MODERATE)
        assertEquals(TdeeMethod.MIFFLIN_ST_JEOR, result.method)
        assertEquals(2759, result.tdeeKcal)
        assertEquals(1780.0, result.bmrKcal, 1e-9)
        assertNull(result.bodyFatPercent)
    }

    @Test
    fun calculateFemaleWithoutHipUsesMifflinStJeorAndGives1584() {
        val m = femaleWithCircumferences.copy(hipCm = null)
        val result = calculateOrFail(m, ActivityLevel.SEDENTARY)
        assertEquals(TdeeMethod.MIFFLIN_ST_JEOR, result.method)
        assertEquals(1584, result.tdeeKcal)
        assertNull(result.bodyFatPercent)
    }

    @Test
    fun calculateWithZeroHeightIsNull() {
        val m = maleWithCircumferences.copy(heightCm = 0.0)
        assertNull(TdeeCalculator.calculate(m, ActivityLevel.MODERATE))
    }

    @Test
    fun calculateWithNaNWeightIsNull() {
        val m = maleWithCircumferences.copy(weightKg = Double.NaN, neckCm = null, waistCm = null)
        assertNull(TdeeCalculator.calculate(m, ActivityLevel.MODERATE))
    }

    @Test
    fun calculateWithWaistNotAboveNeckIsNull() {
        val m = maleWithCircumferences.copy(waistCm = 38.0)
        assertNull(TdeeCalculator.calculate(m, ActivityLevel.MODERATE))
    }

    @Test
    fun calculateMifflinStJeorAt40YearsGivesLowerTdeeThanAt30() {
        val m = maleWithCircumferences.copy(ageYears = 40, neckCm = null, waistCm = null)
        val result = calculateOrFail(m, ActivityLevel.MODERATE)
        assertEquals(TdeeMethod.MIFFLIN_ST_JEOR, result.method)
        assertEquals(1730.0, result.bmrKcal, 1e-9)
        assertTrue("TDEE at 40 years: ${result.tdeeKcal}", result.tdeeKcal < 2759)
    }

    @Test
    fun calculateUsesMifflinStJeorWhenBodyFatIsOutOfRange() {
        // Waist 1 cm above the neck passes validation, but US Navy gives a negative percentage.
        val m = maleWithCircumferences.copy(neckCm = 39.0, waistCm = 40.0)
        assertEquals(emptyList<ValidationError>(), m.validate())
        val result = calculateOrFail(m, ActivityLevel.MODERATE)
        assertEquals(TdeeMethod.MIFFLIN_ST_JEOR, result.method)
        assertEquals(2759, result.tdeeKcal)
        assertNull(result.bodyFatPercent)
    }
}
