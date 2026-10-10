package com.tfg.kaifit_pal.ui.calculator

import androidx.annotation.StringRes
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.domain.ActivityLevel
import java.text.NumberFormat
import java.util.Locale

/** Short name of the activity level. */
@get:StringRes
internal val ActivityLevel.labelRes: Int
    get() = when (this) {
        ActivityLevel.SEDENTARY -> R.string.activity_sedentary
        ActivityLevel.LIGHT -> R.string.activity_light
        ActivityLevel.MODERATE -> R.string.activity_moderate
        ActivityLevel.ACTIVE -> R.string.activity_active
        ActivityLevel.VERY_ACTIVE -> R.string.activity_very_active
    }

/** How much exercise the activity level stands for. */
@get:StringRes
internal val ActivityLevel.descriptionRes: Int
    get() = when (this) {
        ActivityLevel.SEDENTARY -> R.string.activity_sedentary_description
        ActivityLevel.LIGHT -> R.string.activity_light_description
        ActivityLevel.MODERATE -> R.string.activity_moderate_description
        ActivityLevel.ACTIVE -> R.string.activity_active_description
        ActivityLevel.VERY_ACTIVE -> R.string.activity_very_active_description
    }

/** The activity factor written for [locale], with all its decimals: "1,375" in Spanish. Display only. */
internal fun ActivityLevel.formattedFactor(locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 3 }.format(factor)
