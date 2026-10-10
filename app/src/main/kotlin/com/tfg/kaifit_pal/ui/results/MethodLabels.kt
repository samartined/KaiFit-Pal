package com.tfg.kaifit_pal.ui.results

import androidx.annotation.StringRes
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.domain.TdeeMethod

/** Name of the equation shown to the user. */
@get:StringRes
internal val TdeeMethod.labelRes: Int
    get() = when (this) {
        TdeeMethod.KATCH_MCARDLE -> R.string.method_katch_mcardle
        TdeeMethod.MIFFLIN_ST_JEOR -> R.string.method_mifflin_st_jeor
    }

/**
 * [percent] with its sign, as shown next to a goal: "+5", "0" or "−15" (with a true minus sign).
 */
internal fun formatSignedPercent(percent: Int): String = when {
    percent > 0 -> "+$percent"
    percent < 0 -> "−${-percent}"
    else -> "0"
}
