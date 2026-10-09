package com.tfg.kaifit_pal.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Calorie adjustment applied to the TDEE, as a whole percentage from −20 % to +20 % in steps
 * of 5 %. Whole numbers keep repeated steps exact: −5 % three times is always −15 %.
 *
 * The constructor rejects any other value; [increment] and [decrement] saturate at the limits.
 */
@JvmInline
value class GoalModifier(val percent: Int) {

    init {
        require(percent in MIN_PERCENT..MAX_PERCENT && percent % STEP == 0) {
            "Goal modifier must be a multiple of $STEP in $MIN_PERCENT..$MAX_PERCENT, was $percent"
        }
    }

    /** [Goal.DEFICIT] below 0 %, [Goal.MAINTENANCE] at 0 % and [Goal.SURPLUS] above. */
    val goal: Goal
        get() = when {
            percent < 0 -> Goal.DEFICIT
            percent > 0 -> Goal.SURPLUS
            else -> Goal.MAINTENANCE
        }

    /** Size of the adjustment, from |[percent]|. */
    val intensity: GoalIntensity
        get() = when (abs(percent)) {
            0 -> GoalIntensity.NONE
            5 -> GoalIntensity.LIGHT
            10 -> GoalIntensity.MODERATE
            15 -> GoalIntensity.MODERATE_HIGH
            else -> GoalIntensity.HIGH
        }

    /** The next step up, or this same value at [MAX_PERCENT]. */
    fun increment(): GoalModifier = GoalModifier((percent + STEP).coerceAtMost(MAX_PERCENT))

    /** The next step down, or this same value at [MIN_PERCENT]. */
    fun decrement(): GoalModifier = GoalModifier((percent - STEP).coerceAtLeast(MIN_PERCENT))

    /**
     * Target calories, in kcal/day, for a TDEE in kcal/day: `tdee × (100 + percent) / 100`,
     * multiplied in integers and divided once, then rounded to the nearest kcal.
     */
    fun applyTo(tdeeKcal: Int): Int = (tdeeKcal * (100 + percent) / 100.0).roundToInt()

    companion object {
        const val MIN_PERCENT = -20
        const val MAX_PERCENT = 20
        const val STEP = 5

        /** No adjustment: the target is the TDEE. */
        val MAINTENANCE = GoalModifier(0)

        /** The nine valid modifiers, from [MIN_PERCENT] to [MAX_PERCENT]. */
        val all: List<GoalModifier> = (MIN_PERCENT..MAX_PERCENT step STEP).map(::GoalModifier)
    }
}
