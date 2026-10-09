package com.tfg.kaifit_pal.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tfg.kaifit_pal.data.FailingDataStore
import com.tfg.kaifit_pal.data.close
import com.tfg.kaifit_pal.data.closeableScope
import com.tfg.kaifit_pal.data.newPreferencesFile
import com.tfg.kaifit_pal.data.preferencesDataStoreOn
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.Goal
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.domain.TdeeCalculator
import com.tfg.kaifit_pal.domain.TdeeMethod
import com.tfg.kaifit_pal.domain.TdeeResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ProfileRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val clock = Clock.fixed(Instant.parse("2026-10-09T10:15:30.123456Z"), ZoneOffset.UTC)

    /** Man of the reference values: Katch-McArdle, 2819 kcal at MODERATE. */
    private val man = BodyMeasurements(
        sex = Sex.MALE,
        ageYears = 30,
        heightCm = 180.0,
        weightKg = 80.0,
        neckCm = 38.0,
        waistCm = 85.0,
    )

    /** Woman of the reference values without hip: Mifflin-St Jeor, 1584 kcal at SEDENTARY. */
    private val womanWithoutHip = BodyMeasurements(
        sex = Sex.FEMALE,
        ageYears = 30,
        heightCm = 165.0,
        weightKg = 60.0,
        neckCm = 33.0,
        waistCm = 70.0,
    )

    private fun TestScope.newDataStore(): DataStore<Preferences> =
        backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile())

    private fun TestScope.newRepository(): ProfileRepository = ProfileRepository(newDataStore(), clock)

    private fun tdeeOf(measurements: BodyMeasurements, activityLevel: ActivityLevel): TdeeResult {
        val tdee = TdeeCalculator.calculate(measurements, activityLevel)
        assertNotNull("TDEE expected for $measurements", tdee)
        return tdee!!
    }

    /** Saves [measurements] with the default macro plan of [goalModifier]. */
    private suspend fun ProfileRepository.saveWithDefaultPlan(
        measurements: BodyMeasurements,
        activityLevel: ActivityLevel,
        goalModifier: GoalModifier,
    ): SavedProfile {
        val tdee = tdeeOf(measurements, activityLevel)
        val macroPlan = MacroPlan.forGoal(goalModifier.applyTo(tdee.tdeeKcal), goalModifier.goal)
        return save(measurements, activityLevel, tdee, goalModifier, macroPlan)
    }

    /** Saves the reference man at MODERATE with a −15 % deficit: 2819 kcal, target 2396 kcal. */
    private suspend fun ProfileRepository.saveMan(): SavedProfile =
        saveWithDefaultPlan(man, ActivityLevel.MODERATE, GoalModifier(-15))

    /** Saves a valid profile, applies [change] to the raw preferences and reads the profile again. */
    private suspend fun TestScope.profileAfterRawChange(change: (MutablePreferences) -> Unit): SavedProfile? {
        val dataStore = newDataStore()
        val repository = ProfileRepository(dataStore, clock)
        repository.saveMan()
        assertNotNull(repository.profile.first())

        dataStore.edit(change)

        return repository.profile.first()
    }

    @Test
    fun emptyStoreHasNoProfile() = runTest {
        assertNull(newRepository().profile.first())
    }

    @Test
    fun maleKatchMcArdleProfileRoundTrips() = runTest {
        val repository = newRepository()
        val tdee = tdeeOf(man, ActivityLevel.MODERATE)
        val macroPlan = MacroPlan.forGoal(GoalModifier(-15).applyTo(2819), Goal.DEFICIT)

        val saved = repository.save(man, ActivityLevel.MODERATE, tdee, GoalModifier(-15), macroPlan)
        val read = repository.profile.first()

        assertEquals(saved, read)
        assertEquals(2819, read!!.tdee.tdeeKcal)
        assertEquals(TdeeMethod.KATCH_MCARDLE, read.tdee.method)
        assertEquals(2396, read.macroPlan.targetKcal)
        assertEquals(Instant.parse("2026-10-09T10:15:30.123Z"), read.savedAt)
        assertEquals(Instant.parse("2026-10-09T10:15:30.123Z"), saved.savedAt)
    }

    @Test
    fun femaleMifflinProfileWithoutHipRoundTrips() = runTest {
        val repository = newRepository()

        val saved = repository.saveWithDefaultPlan(womanWithoutHip, ActivityLevel.SEDENTARY, GoalModifier.MAINTENANCE)
        val read = repository.profile.first()

        assertEquals(saved, read)
        assertEquals(TdeeMethod.MIFFLIN_ST_JEOR, read!!.tdee.method)
        assertEquals(1584, read.tdee.tdeeKcal)
        assertNull(read.tdee.bodyFatPercent)
        assertNull(read.measurements.hipCm)
        assertEquals(GoalModifier.MAINTENANCE, read.goalModifier)
    }

    @Test
    fun profileSurvivesReopeningTheFile() = runTest {
        val file = tmp.newPreferencesFile()
        val firstScope = closeableScope()
        val saved = ProfileRepository(firstScope.preferencesDataStoreOn(file), clock).saveMan()
        firstScope.close()

        val reopened = ProfileRepository(backgroundScope.preferencesDataStoreOn(file), clock).profile.first()

        assertEquals(saved, reopened)
        assertEquals(2819, reopened!!.tdee.tdeeKcal)
    }

    @Test
    fun saveRemovesOptionalValuesOfThePreviousProfile() = runTest {
        val repository = newRepository()
        val womanWithHip = womanWithoutHip.copy(hipCm = 95.0)
        val first = repository.saveWithDefaultPlan(womanWithHip, ActivityLevel.SEDENTARY, GoalModifier.MAINTENANCE)
        assertEquals(1617, first.tdee.tdeeKcal)
        assertEquals(TdeeMethod.KATCH_MCARDLE, first.tdee.method)

        val manWithoutCircumferences = man.copy(neckCm = null, waistCm = null)
        repository.saveWithDefaultPlan(manWithoutCircumferences, ActivityLevel.MODERATE, GoalModifier.MAINTENANCE)
        val read = repository.profile.first()

        assertNotNull(read)
        assertNull(read!!.measurements.neckCm)
        assertNull(read.measurements.waistCm)
        assertNull(read.measurements.hipCm)
        assertNull(read.tdee.bodyFatPercent)
        assertEquals(TdeeMethod.MIFFLIN_ST_JEOR, read.tdee.method)
        assertEquals(2759, read.tdee.tdeeKcal)
    }

    @Test
    fun manuallyAdjustedGramsAreKept() = runTest {
        val repository = newRepository()
        val tdee = TdeeResult(tdeeKcal = 2500, bmrKcal = 1612.9, method = TdeeMethod.MIFFLIN_ST_JEOR, bodyFatPercent = null)
        val macroPlan = MacroPlan.forGoal(2500, Goal.MAINTENANCE).withProteinGrams(250)

        repository.save(man, ActivityLevel.MODERATE, tdee, GoalModifier.MAINTENANCE, macroPlan)
        val read = repository.profile.first()!!.macroPlan

        assertEquals(250, read.proteinGrams)
        assertEquals(69, read.fatGrams)
        assertEquals(220, read.carbGrams)
    }

    @Test
    fun clearRemovesTheProfile() = runTest {
        val repository = newRepository()
        repository.saveMan()

        repository.clear()

        assertNull(repository.profile.first())
    }

    @Test
    fun profileFlowEmitsNullAndThenTheSavedProfile() = runTest {
        val repository = newRepository()
        val emptySeen = CompletableDeferred<Unit>()
        val emissions = async {
            repository.profile.onEach { if (it == null) emptySeen.complete(Unit) }.take(2).toList()
        }
        emptySeen.await()

        val saved = repository.saveMan()

        assertEquals(listOf(null, saved), emissions.await())
    }

    @Test
    fun unknownSexGivesNoProfile() = runTest {
        assertNull(profileAfterRawChange { it[ProfileKeys.SEX] = "OTHER" })
    }

    @Test
    fun invalidGoalModifierGivesNoProfile() = runTest {
        assertNull(profileAfterRawChange { it[ProfileKeys.GOAL_MODIFIER_PERCENT] = 7 })
    }

    @Test
    fun missingTdeeGivesNoProfile() = runTest {
        assertNull(profileAfterRawChange { it.remove(ProfileKeys.TDEE_KCAL) })
    }

    @Test
    fun invalidMeasurementsGiveNoProfile() = runTest {
        assertNull(profileAfterRawChange { it[ProfileKeys.HEIGHT_CM] = 0.0 })
    }

    @Test
    fun targetNotMatchingTheGoalModifierGivesNoProfile() = runTest {
        assertNull(profileAfterRawChange { it[ProfileKeys.TARGET_KCAL] = 2397 })
    }

    @Test
    fun otherSchemaVersionGivesNoProfile() = runTest {
        assertNull(profileAfterRawChange { it[ProfileKeys.SCHEMA_VERSION] = 2 })
    }

    @Test
    fun nonFiniteValueGivesNoProfile() = runTest {
        assertNull(profileAfterRawChange { it[ProfileKeys.BMR_KCAL] = Double.NaN })
    }

    @Test
    fun bodyFatWithMifflinMethodGivesNoProfile() = runTest {
        assertNull(profileAfterRawChange { it[ProfileKeys.TDEE_METHOD] = "MIFFLIN_ST_JEOR" })
    }

    @Test
    fun valueOfAnotherTypeGivesNoProfile() = runTest {
        assertNull(profileAfterRawChange { it[stringPreferencesKey("age_years")] = "30" })
    }

    @Test
    fun saveWithInvalidMeasurementsThrowsAndKeepsThePreviousProfile() = runTest {
        val repository = newRepository()
        val previous = repository.saveMan()
        val tdee = tdeeOf(man, ActivityLevel.MODERATE)
        val macroPlan = MacroPlan.forGoal(GoalModifier(-15).applyTo(2819), Goal.DEFICIT)

        try {
            repository.save(man.copy(heightCm = 0.0), ActivityLevel.MODERATE, tdee, GoalModifier(-15), macroPlan)
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }

        assertEquals(previous, repository.profile.first())
    }

    @Test
    fun saveWithTargetNotMatchingTheGoalModifierThrows() = runTest {
        val repository = newRepository()
        val tdee = tdeeOf(man, ActivityLevel.MODERATE)
        val maintenancePlan = MacroPlan.forGoal(2819, Goal.MAINTENANCE)

        try {
            repository.save(man, ActivityLevel.MODERATE, tdee, GoalModifier(-15), maintenancePlan)
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }

        assertNull(repository.profile.first())
    }

    @Test
    fun saveWithBodyFatButMifflinMethodThrows() = runTest {
        val repository = newRepository()
        val tdee = tdeeOf(man, ActivityLevel.MODERATE).copy(method = TdeeMethod.MIFFLIN_ST_JEOR)
        val macroPlan = MacroPlan.forGoal(2819, Goal.MAINTENANCE)

        try {
            repository.save(man, ActivityLevel.MODERATE, tdee, GoalModifier.MAINTENANCE, macroPlan)
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }

        assertNull(repository.profile.first())
    }

    @Test
    fun saveOfManWithNaNHipThrowsAndKeepsThePreviousProfile() = runTest {
        val repository = newRepository()
        val previous = repository.saveMan()
        val manWithNaNHip = man.copy(hipCm = Double.NaN)

        try {
            repository.saveWithDefaultPlan(manWithNaNHip, ActivityLevel.MODERATE, GoalModifier(-15))
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }

        assertEquals(previous, repository.profile.first())
    }

    @Test
    fun saveOfManWithInfiniteHipThrowsAndWritesNothing() = runTest {
        val dataStore = newDataStore()
        val repository = ProfileRepository(dataStore, clock)
        val manWithInfiniteHip = man.copy(hipCm = Double.POSITIVE_INFINITY)

        try {
            repository.saveWithDefaultPlan(manWithInfiniteHip, ActivityLevel.MODERATE, GoalModifier(-15))
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }

        assertEquals(emptyMap<Preferences.Key<*>, Any>(), dataStore.data.first().asMap())
    }

    @Test
    fun saveWithNaNBmrThrowsAndWritesNothing() = runTest {
        val dataStore = newDataStore()
        val repository = ProfileRepository(dataStore, clock)
        val tdee = tdeeOf(man, ActivityLevel.MODERATE).copy(bmrKcal = Double.NaN)
        val macroPlan = MacroPlan.forGoal(GoalModifier(-15).applyTo(2819), Goal.DEFICIT)

        try {
            repository.save(man, ActivityLevel.MODERATE, tdee, GoalModifier(-15), macroPlan)
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }

        assertEquals(emptyMap<Preferences.Key<*>, Any>(), dataStore.data.first().asMap())
    }

    @Test
    fun saveWithNaNBodyFatThrowsAndWritesNothing() = runTest {
        val dataStore = newDataStore()
        val repository = ProfileRepository(dataStore, clock)
        val tdee = tdeeOf(man, ActivityLevel.MODERATE).copy(bodyFatPercent = Double.NaN)
        val macroPlan = MacroPlan.forGoal(GoalModifier(-15).applyTo(2819), Goal.DEFICIT)

        try {
            repository.save(man, ActivityLevel.MODERATE, tdee, GoalModifier(-15), macroPlan)
            fail("IllegalArgumentException expected")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }

        assertEquals(emptyMap<Preferences.Key<*>, Any>(), dataStore.data.first().asMap())
    }

    @Test
    fun manWithFiniteHipRoundTrips() = runTest {
        val repository = newRepository()

        val saved = repository.saveWithDefaultPlan(man.copy(hipCm = 95.0), ActivityLevel.MODERATE, GoalModifier(-15))
        val read = repository.profile.first()

        assertEquals(saved, read)
        assertEquals(95.0, read!!.measurements.hipCm)
        assertEquals(TdeeMethod.KATCH_MCARDLE, read.tdee.method)
        assertEquals(2819, read.tdee.tdeeKcal)
        assertEquals(2396, read.macroPlan.targetKcal)
    }

    @Test
    fun saveStoresTheDocumentedKeys() = runTest {
        val dataStore = newDataStore()
        ProfileRepository(dataStore, clock).saveMan()

        val stored = dataStore.data.first()

        assertEquals(
            setOf(
                "schema_version", "sex", "age_years", "height_cm", "weight_kg", "neck_cm", "waist_cm",
                "activity_level", "tdee_kcal", "bmr_kcal", "tdee_method", "body_fat_percent",
                "goal_modifier_percent", "target_kcal", "protein_g", "fat_g", "carb_g", "saved_at_epoch_millis",
            ),
            stored.asMap().keys.map { it.name }.toSet(),
        )
        assertEquals(1, stored[ProfileKeys.SCHEMA_VERSION])
        assertEquals("MALE", stored[ProfileKeys.SEX])
        assertEquals("MODERATE", stored[ProfileKeys.ACTIVITY_LEVEL])
        assertEquals("KATCH_MCARDLE", stored[ProfileKeys.TDEE_METHOD])
        assertEquals(-15, stored[ProfileKeys.GOAL_MODIFIER_PERCENT])
        assertEquals(1791540930123L, stored[ProfileKeys.SAVED_AT_EPOCH_MILLIS])
    }

    @Test
    fun readErrorGivesNoProfile() = runTest {
        val repository = ProfileRepository(FailingDataStore(IOException("disk unavailable")), clock)

        assertNull(repository.profile.first())
    }

    @Test
    fun errorOtherThanIoReachesTheCollector() = runTest {
        val repository = ProfileRepository(FailingDataStore(IllegalStateException("bug")), clock)

        try {
            repository.profile.first()
            fail("IllegalStateException expected")
        } catch (e: IllegalStateException) {
            assertEquals("bug", e.message)
        }
    }
}
