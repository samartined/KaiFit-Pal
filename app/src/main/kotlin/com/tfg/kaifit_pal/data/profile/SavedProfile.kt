package com.tfg.kaifit_pal.data.profile

import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.TdeeResult
import java.time.Instant

/**
 * The last measurement and its result, saved together as one snapshot.
 *
 * It is what "Guardar en perfil" on the results screen stores, what "Recalcular" loads back into
 * the calculator and what gives the assistant its context about the user.
 *
 * The TDEE and the macro grams are kept as they were saved, never recomputed, so the profile
 * shows the same result the user saw, manual protein and fat changes included.
 *
 * @property measurements body measurements.
 * @property activityLevel activity level used for the TDEE.
 * @property tdee total daily energy expenditure, with its method and body fat percentage.
 * @property goalModifier calorie adjustment chosen by the user.
 * @property macroPlan daily macros, whose target is [goalModifier] applied to the TDEE.
 * @property savedAt when the snapshot was saved, to the millisecond.
 */
data class SavedProfile(
    val measurements: BodyMeasurements,
    val activityLevel: ActivityLevel,
    val tdee: TdeeResult,
    val goalModifier: GoalModifier,
    val macroPlan: MacroPlan,
    val savedAt: Instant,
)
