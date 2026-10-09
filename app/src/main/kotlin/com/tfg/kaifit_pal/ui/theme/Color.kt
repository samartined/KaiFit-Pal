package com.tfg.kaifit_pal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Original KaiFit green (3.27:1 on white, below AA for text). Decorative use only: it is not
 * assigned to any color role.
 */
val BrandGreen = Color(0xFF5A9E47)

/**
 * Light scheme: green brand palette on the beige brand background.
 *
 * Every role is set explicitly so no Material baseline (purple) color leaks in. The fixed roles
 * are identical in both schemes and reuse the tone 90 / 80 / 10 / 30 colors of each palette.
 */
val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF3B7D2C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB2EDB0),
    onPrimaryContainer = Color(0xFF0A2A05),
    inversePrimary = Color(0xFF9FD88A),
    secondary = Color(0xFF54634D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E7CB),
    onSecondaryContainer = Color(0xFF121F0E),
    tertiary = Color(0xFF1F6A6B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA8EFF0),
    onTertiaryContainer = Color(0xFF002020),
    background = Color(0xFFF5F5EE),
    onBackground = Color(0xFF1B1C18),
    surface = Color(0xFFF5F5EE),
    onSurface = Color(0xFF1B1C18),
    surfaceVariant = Color(0xFFE0E4D6),
    onSurfaceVariant = Color(0xFF43483E),
    surfaceTint = Color(0xFF3B7D2C),
    inverseSurface = Color(0xFF30312C),
    inverseOnSurface = Color(0xFFF2F1EA),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF73796E),
    outlineVariant = Color(0xFFC3C8BC),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF5F5EE),
    surfaceContainer = Color(0xFFE9E9E2),
    surfaceContainerHigh = Color(0xFFE3E4DC),
    surfaceContainerHighest = Color(0xFFDDDED6),
    surfaceContainerLow = Color(0xFFEFEFE8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDBDBD3),
    primaryFixed = Color(0xFFB2EDB0),
    primaryFixedDim = Color(0xFF9FD88A),
    onPrimaryFixed = Color(0xFF0A2A05),
    onPrimaryFixedVariant = Color(0xFF245F17),
    secondaryFixed = Color(0xFFD8E7CB),
    secondaryFixedDim = Color(0xFFBCCBB1),
    onSecondaryFixed = Color(0xFF121F0E),
    onSecondaryFixedVariant = Color(0xFF3D4B36),
    tertiaryFixed = Color(0xFFA8EFF0),
    tertiaryFixedDim = Color(0xFF86D3D4),
    onTertiaryFixed = Color(0xFF002020),
    onTertiaryFixedVariant = Color(0xFF004F51),
)

/** Dark scheme: same palettes on the dark brand background. */
val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF9FD88A),
    onPrimary = Color(0xFF0B3900),
    primaryContainer = Color(0xFF245F17),
    onPrimaryContainer = Color(0xFFB2EDB0),
    inversePrimary = Color(0xFF3B7D2C),
    secondary = Color(0xFFBCCBB1),
    onSecondary = Color(0xFF273422),
    secondaryContainer = Color(0xFF3D4B36),
    onSecondaryContainer = Color(0xFFD8E7CB),
    tertiary = Color(0xFF86D3D4),
    onTertiary = Color(0xFF003738),
    tertiaryContainer = Color(0xFF004F51),
    onTertiaryContainer = Color(0xFFA8EFF0),
    background = Color(0xFF121410),
    onBackground = Color(0xFFE3E3DC),
    surface = Color(0xFF121410),
    onSurface = Color(0xFFE3E3DC),
    surfaceVariant = Color(0xFF43483E),
    onSurfaceVariant = Color(0xFFC3C8BC),
    surfaceTint = Color(0xFF9FD88A),
    inverseSurface = Color(0xFFE3E3DC),
    inverseOnSurface = Color(0xFF30312C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8D9387),
    outlineVariant = Color(0xFF43483E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF383A35),
    surfaceContainer = Color(0xFF1F201C),
    surfaceContainerHigh = Color(0xFF292B26),
    surfaceContainerHighest = Color(0xFF343631),
    surfaceContainerLow = Color(0xFF1B1C18),
    surfaceContainerLowest = Color(0xFF0D0F0B),
    surfaceDim = Color(0xFF121410),
    primaryFixed = Color(0xFFB2EDB0),
    primaryFixedDim = Color(0xFF9FD88A),
    onPrimaryFixed = Color(0xFF0A2A05),
    onPrimaryFixedVariant = Color(0xFF245F17),
    secondaryFixed = Color(0xFFD8E7CB),
    secondaryFixedDim = Color(0xFFBCCBB1),
    onSecondaryFixed = Color(0xFF121F0E),
    onSecondaryFixedVariant = Color(0xFF3D4B36),
    tertiaryFixed = Color(0xFFA8EFF0),
    tertiaryFixedDim = Color(0xFF86D3D4),
    onTertiaryFixed = Color(0xFF002020),
    onTertiaryFixedVariant = Color(0xFF004F51),
)
