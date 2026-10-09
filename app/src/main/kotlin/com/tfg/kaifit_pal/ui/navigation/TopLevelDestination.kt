package com.tfg.kaifit_pal.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tfg.kaifit_pal.R

/** The five tabs of the navigation bar, in display order. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    PROFILE(ProfileRoute, R.string.nav_profile, R.drawable.ic_nav_profile),
    CALCULATOR(CalculatorRoute, R.string.nav_calculator, R.drawable.ic_nav_calculator),
    ASSISTANT(AssistantRoute, R.string.nav_assistant, R.drawable.ic_nav_assistant),
    HELP(HelpRoute, R.string.nav_help, R.drawable.ic_nav_help),
    SETTINGS(SettingsRoute, R.string.nav_settings, R.drawable.ic_nav_settings),
}
