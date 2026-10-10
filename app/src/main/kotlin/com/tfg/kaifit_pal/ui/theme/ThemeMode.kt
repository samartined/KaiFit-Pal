package com.tfg.kaifit_pal.ui.theme

import com.tfg.kaifit_pal.data.settings.ThemeMode

/**
 * Whether this mode uses the dark colour scheme when the system is in dark mode or not
 * ([systemDark]): [ThemeMode.SYSTEM] follows the system, the other two ignore it.
 */
fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
