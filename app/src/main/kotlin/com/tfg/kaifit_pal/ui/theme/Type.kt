package com.tfg.kaifit_pal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/** Material 3 default type scale with the system font. */
val KaiFitTypography = Typography()

/**
 * Returns this style with tabular (fixed-width) digits, so numbers that change, such as the
 * TDEE, keep their width and stay aligned.
 */
fun TextStyle.withTabularFigures(): TextStyle = copy(fontFeatureSettings = "tnum")
