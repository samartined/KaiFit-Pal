package com.tfg.kaifit_pal.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tfg.kaifit_pal.data.getOrNull
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.domain.TdeeMethod
import com.tfg.kaifit_pal.domain.TdeeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Stores the last [SavedProfile] in its own Preferences [DataStore], which holds nothing else.
 *
 * @param clock source of [SavedProfile.savedAt]; replaceable in tests.
 */
class ProfileRepository(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock = Clock.systemUTC(),
) {

    /**
     * The saved profile, emitted again whenever it changes, or `null` when there is none.
     *
     * Stored data never makes it fail: a file that cannot be read, a missing key, an unknown
     * format version or data that do not form a coherent profile all give `null`, the same as
     * having no profile.
     */
    val profile: Flow<SavedProfile?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { decode(it) }
        .distinctUntilChanged()

    /**
     * Replaces the saved profile with a new snapshot, written in a single transaction, and returns
     * it with [SavedProfile.savedAt] set to now, truncated to milliseconds as it is stored.
     *
     * Every previous value is removed first, so an optional circumference or body fat percentage
     * of an older profile never survives a save without it.
     *
     * @throws IllegalArgumentException before writing anything, if [measurements] are not valid,
     *   if any number to store is `NaN` or infinite (also the hip of a man, the BMR and the body
     *   fat percentage, which validation does not check), if the target of [macroPlan] is not
     *   [goalModifier] applied to the TDEE, or if [tdee] has a body fat percentage without using
     *   Katch-McArdle or the other way round.
     */
    suspend fun save(
        measurements: BodyMeasurements,
        activityLevel: ActivityLevel,
        tdee: TdeeResult,
        goalModifier: GoalModifier,
        macroPlan: MacroPlan,
    ): SavedProfile {
        require(measurements.validate().isEmpty()) {
            "Cannot save invalid measurements: ${measurements.validate()}"
        }
        require(allStoredDoublesFinite(measurements, tdee)) {
            "Cannot save a value that is NaN or infinite: $measurements, $tdee"
        }
        require(targetMatches(tdee, goalModifier, macroPlan)) {
            "Target ${macroPlan.targetKcal} kcal is not $goalModifier applied to ${tdee.tdeeKcal} kcal"
        }
        require(tdee.hasBodyFatOnlyWithKatchMcArdle()) {
            "Method ${tdee.method} does not match body fat ${tdee.bodyFatPercent}"
        }
        val profile = SavedProfile(
            measurements = measurements,
            activityLevel = activityLevel,
            tdee = tdee,
            goalModifier = goalModifier,
            macroPlan = macroPlan,
            savedAt = clock.instant().truncatedTo(ChronoUnit.MILLIS),
        )
        dataStore.edit { prefs ->
            prefs.clear()
            prefs[ProfileKeys.SCHEMA_VERSION] = ProfileKeys.CURRENT_SCHEMA_VERSION
            prefs[ProfileKeys.SEX] = measurements.sex.name
            prefs[ProfileKeys.AGE_YEARS] = measurements.ageYears
            prefs[ProfileKeys.HEIGHT_CM] = measurements.heightCm
            prefs[ProfileKeys.WEIGHT_KG] = measurements.weightKg
            measurements.neckCm?.let { prefs[ProfileKeys.NECK_CM] = it }
            measurements.waistCm?.let { prefs[ProfileKeys.WAIST_CM] = it }
            measurements.hipCm?.let { prefs[ProfileKeys.HIP_CM] = it }
            prefs[ProfileKeys.ACTIVITY_LEVEL] = activityLevel.name
            prefs[ProfileKeys.TDEE_KCAL] = tdee.tdeeKcal
            prefs[ProfileKeys.BMR_KCAL] = tdee.bmrKcal
            prefs[ProfileKeys.TDEE_METHOD] = tdee.method.name
            tdee.bodyFatPercent?.let { prefs[ProfileKeys.BODY_FAT_PERCENT] = it }
            prefs[ProfileKeys.GOAL_MODIFIER_PERCENT] = goalModifier.percent
            prefs[ProfileKeys.TARGET_KCAL] = macroPlan.targetKcal
            prefs[ProfileKeys.PROTEIN_GRAMS] = macroPlan.proteinGrams
            prefs[ProfileKeys.FAT_GRAMS] = macroPlan.fatGrams
            prefs[ProfileKeys.CARB_GRAMS] = macroPlan.carbGrams
            prefs[ProfileKeys.SAVED_AT_EPOCH_MILLIS] = profile.savedAt.toEpochMilli()
        }
        return profile
    }

    /** Deletes the saved profile. The settings live in another file and are not affected. */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    /** The profile in [prefs], or `null` if it is incomplete, of another format or incoherent. */
    private fun decode(prefs: Preferences): SavedProfile? {
        if (prefs.getOrNull(ProfileKeys.SCHEMA_VERSION) != ProfileKeys.CURRENT_SCHEMA_VERSION) return null
        val sexName = prefs.getOrNull(ProfileKeys.SEX)
        val activityName = prefs.getOrNull(ProfileKeys.ACTIVITY_LEVEL)
        val methodName = prefs.getOrNull(ProfileKeys.TDEE_METHOD)
        val sex = Sex.entries.firstOrNull { it.name == sexName } ?: return null
        val activityLevel = ActivityLevel.entries.firstOrNull { it.name == activityName } ?: return null
        val method = TdeeMethod.entries.firstOrNull { it.name == methodName } ?: return null

        val ageYears = prefs.getOrNull(ProfileKeys.AGE_YEARS) ?: return null
        val heightCm = prefs.getOrNull(ProfileKeys.HEIGHT_CM) ?: return null
        val weightKg = prefs.getOrNull(ProfileKeys.WEIGHT_KG) ?: return null
        val neckCm = prefs.getOrNull(ProfileKeys.NECK_CM)
        val waistCm = prefs.getOrNull(ProfileKeys.WAIST_CM)
        val hipCm = prefs.getOrNull(ProfileKeys.HIP_CM)
        val tdeeKcal = prefs.getOrNull(ProfileKeys.TDEE_KCAL) ?: return null
        val bmrKcal = prefs.getOrNull(ProfileKeys.BMR_KCAL) ?: return null
        val bodyFatPercent = prefs.getOrNull(ProfileKeys.BODY_FAT_PERCENT)
        val goalModifierPercent = prefs.getOrNull(ProfileKeys.GOAL_MODIFIER_PERCENT) ?: return null
        val targetKcal = prefs.getOrNull(ProfileKeys.TARGET_KCAL) ?: return null
        val proteinGrams = prefs.getOrNull(ProfileKeys.PROTEIN_GRAMS) ?: return null
        val fatGrams = prefs.getOrNull(ProfileKeys.FAT_GRAMS) ?: return null
        val carbGrams = prefs.getOrNull(ProfileKeys.CARB_GRAMS) ?: return null
        val savedAtMillis = prefs.getOrNull(ProfileKeys.SAVED_AT_EPOCH_MILLIS) ?: return null

        val measurements = BodyMeasurements(sex, ageYears, heightCm, weightKg, neckCm, waistCm, hipCm)
        val tdee = TdeeResult(tdeeKcal, bmrKcal, method, bodyFatPercent)
        if (!allStoredDoublesFinite(measurements, tdee)) return null

        val (goalModifier, macroPlan) = try {
            GoalModifier(goalModifierPercent) to MacroPlan(targetKcal, proteinGrams, fatGrams, carbGrams)
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (measurements.validate().isNotEmpty()) return null
        if (!targetMatches(tdee, goalModifier, macroPlan)) return null
        if (!tdee.hasBodyFatOnlyWithKatchMcArdle()) return null

        return SavedProfile(
            measurements = measurements,
            activityLevel = activityLevel,
            tdee = tdee,
            goalModifier = goalModifier,
            macroPlan = macroPlan,
            savedAt = Instant.ofEpochMilli(savedAtMillis),
        )
    }
}

/** Keys of the profile file. Enums are stored by name, so reordering their values is safe. */
internal object ProfileKeys {
    /** Format of the stored profile; a profile of any other version is ignored. */
    const val CURRENT_SCHEMA_VERSION = 1

    val SCHEMA_VERSION = intPreferencesKey("schema_version")
    val SEX = stringPreferencesKey("sex")
    val AGE_YEARS = intPreferencesKey("age_years")
    val HEIGHT_CM = doublePreferencesKey("height_cm")
    val WEIGHT_KG = doublePreferencesKey("weight_kg")
    val NECK_CM = doublePreferencesKey("neck_cm")
    val WAIST_CM = doublePreferencesKey("waist_cm")
    val HIP_CM = doublePreferencesKey("hip_cm")
    val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
    val TDEE_KCAL = intPreferencesKey("tdee_kcal")
    val BMR_KCAL = doublePreferencesKey("bmr_kcal")
    val TDEE_METHOD = stringPreferencesKey("tdee_method")
    val BODY_FAT_PERCENT = doublePreferencesKey("body_fat_percent")
    val GOAL_MODIFIER_PERCENT = intPreferencesKey("goal_modifier_percent")
    val TARGET_KCAL = intPreferencesKey("target_kcal")
    val PROTEIN_GRAMS = intPreferencesKey("protein_g")
    val FAT_GRAMS = intPreferencesKey("fat_g")
    val CARB_GRAMS = intPreferencesKey("carb_g")
    val SAVED_AT_EPOCH_MILLIS = longPreferencesKey("saved_at_epoch_millis")
}

/**
 * Whether every `Double` that a profile stores is finite: height, weight, the circumferences that
 * are present (the hip too, whatever the sex) and the BMR and body fat percentage of [tdee].
 *
 * Saving and reading share this check, so they never disagree about which numbers can be stored.
 */
private fun allStoredDoublesFinite(measurements: BodyMeasurements, tdee: TdeeResult): Boolean =
    listOfNotNull(
        measurements.heightCm,
        measurements.weightKg,
        measurements.neckCm,
        measurements.waistCm,
        measurements.hipCm,
        tdee.bmrKcal,
        tdee.bodyFatPercent,
    ).all { it.isFinite() }

/** Whether the macro target is the goal modifier applied to the TDEE. */
private fun targetMatches(tdee: TdeeResult, goalModifier: GoalModifier, macroPlan: MacroPlan): Boolean =
    macroPlan.targetKcal == goalModifier.applyTo(tdee.tdeeKcal)

/** Whether there is a body fat percentage exactly when the method is Katch-McArdle, which needs it. */
private fun TdeeResult.hasBodyFatOnlyWithKatchMcArdle(): Boolean =
    (method == TdeeMethod.KATCH_MCARDLE) == (bodyFatPercent != null)
