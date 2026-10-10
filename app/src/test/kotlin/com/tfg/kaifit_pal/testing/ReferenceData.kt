package com.tfg.kaifit_pal.testing

import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.ui.navigation.ResultsRoute

/**
 * Reference people of the tests, whose expected results are checked with literal values:
 * the man gives 16.1508 % body fat and a TDEE of 2819 kcal (Katch-McArdle, moderate activity),
 * the woman 24.5635 % and 1617 kcal (sedentary).
 */
internal object ReferenceData {

    val man = BodyMeasurements(Sex.MALE, ageYears = 30, heightCm = 180.0, weightKg = 80.0, neckCm = 38.0, waistCm = 85.0)

    val manActivity = ActivityLevel.MODERATE

    val manRoute = ResultsRoute.from(man, manActivity)

    val woman = BodyMeasurements(
        Sex.FEMALE,
        ageYears = 30,
        heightCm = 165.0,
        weightKg = 60.0,
        neckCm = 33.0,
        waistCm = 70.0,
        hipCm = 95.0,
    )
}
