package com.tfg.kaifit_pal.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BodyMeasurementsTest {

    private val male = BodyMeasurements(
        sex = Sex.MALE,
        ageYears = 30,
        heightCm = 180.0,
        weightKg = 80.0,
        neckCm = 38.0,
        waistCm = 85.0,
    )

    private val female = BodyMeasurements(
        sex = Sex.FEMALE,
        ageYears = 30,
        heightCm = 165.0,
        weightKg = 60.0,
        neckCm = 33.0,
        waistCm = 70.0,
        hipCm = 95.0,
    )

    private fun outOfRange(field: MeasurementField) =
        listOf(ValidationError(field, ValidationError.Kind.OUT_OF_RANGE))

    private val waistNotAboveNeck =
        listOf(ValidationError(MeasurementField.WAIST, ValidationError.Kind.WAIST_NOT_ABOVE_NECK))

    @Test
    fun maleReferenceMeasurementsAreValid() {
        assertEquals(emptyList<ValidationError>(), male.validate())
    }

    @Test
    fun femaleReferenceMeasurementsAreValid() {
        assertEquals(emptyList<ValidationError>(), female.validate())
    }

    @Test
    fun otherReferenceMeasurementsAreValid() {
        val otherMale = BodyMeasurements(Sex.MALE, 30, 175.0, 80.0, neckCm = 40.0, waistCm = 80.0)
        val otherFemale = BodyMeasurements(Sex.FEMALE, 30, 170.0, 60.0, neckCm = 34.0, waistCm = 75.0, hipCm = 100.0)
        assertEquals(emptyList<ValidationError>(), otherMale.validate())
        assertEquals(emptyList<ValidationError>(), otherFemale.validate())
    }

    @Test
    fun age18IsValid() {
        assertEquals(emptyList<ValidationError>(), male.copy(ageYears = 18).validate())
    }

    @Test
    fun age90IsValid() {
        assertEquals(emptyList<ValidationError>(), male.copy(ageYears = 90).validate())
    }

    @Test
    fun age17IsOutOfRange() {
        assertEquals(outOfRange(MeasurementField.AGE), male.copy(ageYears = 17).validate())
    }

    @Test
    fun age91IsOutOfRange() {
        assertEquals(outOfRange(MeasurementField.AGE), male.copy(ageYears = 91).validate())
    }

    @Test
    fun heightZeroIsOutOfRange() {
        assertEquals(outOfRange(MeasurementField.HEIGHT), male.copy(heightCm = 0.0).validate())
    }

    @Test
    fun weightNaNIsOutOfRange() {
        assertEquals(outOfRange(MeasurementField.WEIGHT), male.copy(weightKg = Double.NaN).validate())
    }

    @Test
    fun weightInfinityIsOutOfRange() {
        assertEquals(outOfRange(MeasurementField.WEIGHT), male.copy(weightKg = Double.POSITIVE_INFINITY).validate())
    }

    @Test
    fun heightAndWeightLimitsAreInclusive() {
        assertEquals(emptyList<ValidationError>(), male.copy(heightCm = 250.0, weightKg = 30.0).validate())
        assertEquals(emptyList<ValidationError>(), male.copy(heightCm = 100.0, weightKg = 300.0).validate())
    }

    @Test
    fun nullCircumferencesAreNotErrors() {
        val withoutCircumferences = BodyMeasurements(Sex.FEMALE, 30, 165.0, 60.0)
        assertEquals(emptyList<ValidationError>(), withoutCircumferences.validate())
    }

    @Test
    fun neckBelowRangeIsOutOfRange() {
        assertEquals(outOfRange(MeasurementField.NECK), male.copy(neckCm = 19.9).validate())
    }

    @Test
    fun neckNegativeInfinityIsOutOfRange() {
        assertEquals(outOfRange(MeasurementField.NECK), male.copy(neckCm = Double.NEGATIVE_INFINITY).validate())
    }

    @Test
    fun waistAboveRangeIsOutOfRange() {
        assertEquals(outOfRange(MeasurementField.WAIST), male.copy(waistCm = 200.1).validate())
    }

    @Test
    fun hipOutOfRangeIsIgnoredForMale() {
        assertEquals(emptyList<ValidationError>(), male.copy(hipCm = 10.0).validate())
    }

    @Test
    fun hipOutOfRangeIsErrorForFemale() {
        assertEquals(outOfRange(MeasurementField.HIP), female.copy(hipCm = 10.0).validate())
    }

    @Test
    fun waistEqualToNeckIsWaistNotAboveNeck() {
        assertEquals(waistNotAboveNeck, male.copy(neckCm = 40.0, waistCm = 40.0).validate())
    }

    @Test
    fun waistBelowNeckIsWaistNotAboveNeck() {
        assertEquals(waistNotAboveNeck, male.copy(neckCm = 45.0, waistCm = 42.0).validate())
    }

    @Test
    fun waistNotAboveNeckAlsoAppliesToFemale() {
        assertEquals(waistNotAboveNeck, female.copy(neckCm = 45.0, waistCm = 45.0).validate())
    }

    @Test
    fun everyBrokenRuleIsReported() {
        val errors = BodyMeasurements(Sex.FEMALE, 17, 0.0, 301.0, neckCm = 81.0, waistCm = 39.0, hipCm = 201.0)
            .validate()
        assertEquals(
            listOf(
                ValidationError(MeasurementField.AGE, ValidationError.Kind.OUT_OF_RANGE),
                ValidationError(MeasurementField.HEIGHT, ValidationError.Kind.OUT_OF_RANGE),
                ValidationError(MeasurementField.WEIGHT, ValidationError.Kind.OUT_OF_RANGE),
                ValidationError(MeasurementField.NECK, ValidationError.Kind.OUT_OF_RANGE),
                ValidationError(MeasurementField.WAIST, ValidationError.Kind.OUT_OF_RANGE),
                ValidationError(MeasurementField.HIP, ValidationError.Kind.OUT_OF_RANGE),
                ValidationError(MeasurementField.WAIST, ValidationError.Kind.WAIST_NOT_ABOVE_NECK),
            ),
            errors,
        )
    }

    @Test
    fun activityLevelsAreInAscendingOrder() {
        assertEquals(
            listOf(
                ActivityLevel.SEDENTARY,
                ActivityLevel.LIGHT,
                ActivityLevel.MODERATE,
                ActivityLevel.ACTIVE,
                ActivityLevel.VERY_ACTIVE,
            ),
            ActivityLevel.entries,
        )
    }

    @Test
    fun sedentaryFactorIs1_2() {
        assertEquals(1.2, ActivityLevel.SEDENTARY.factor, 0.0)
    }

    @Test
    fun lightFactorIs1_375() {
        assertEquals(1.375, ActivityLevel.LIGHT.factor, 0.0)
    }

    @Test
    fun moderateFactorIs1_55() {
        assertEquals(1.55, ActivityLevel.MODERATE.factor, 0.0)
    }

    @Test
    fun activeFactorIs1_725() {
        assertEquals(1.725, ActivityLevel.ACTIVE.factor, 0.0)
    }

    @Test
    fun veryActiveFactorIs1_9() {
        assertEquals(1.9, ActivityLevel.VERY_ACTIVE.factor, 0.0)
    }
}
