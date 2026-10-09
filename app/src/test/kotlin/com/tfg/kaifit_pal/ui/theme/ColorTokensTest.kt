package com.tfg.kaifit_pal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the color schemes against silent changes: the brand tokens fixed by the modernization
 * plan, and the roles derived from them, must keep their exact values.
 */
class ColorTokensTest {

    @Test
    fun lightSchemeMatchesPlanTokens() {
        assertRoles(
            scheme = LightColors,
            expected = mapOf(
                "primary" to 0xFF3B7D2C,
                "onPrimary" to 0xFFFFFFFF,
                "primaryContainer" to 0xFFB2EDB0,
                "onPrimaryContainer" to 0xFF0A2A05,
                "secondary" to 0xFF54634D,
                "secondaryContainer" to 0xFFD8E7CB,
                "tertiary" to 0xFF1F6A6B,
                "tertiaryContainer" to 0xFFA8EFF0,
                "error" to 0xFFBA1A1A,
                "background" to 0xFFF5F5EE,
                "surface" to 0xFFF5F5EE,
                "onBackground" to 0xFF1B1C18,
                "onSurface" to 0xFF1B1C18,
                "surfaceVariant" to 0xFFE0E4D6,
                "onSurfaceVariant" to 0xFF43483E,
                "outline" to 0xFF73796E,
                "surfaceContainerLowest" to 0xFFFFFFFF,
                "surfaceContainerLow" to 0xFFEFEFE8,
                "surfaceContainer" to 0xFFE9E9E2,
                "surfaceContainerHigh" to 0xFFE3E4DC,
                "surfaceContainerHighest" to 0xFFDDDED6,
            ),
        )
    }

    @Test
    fun darkSchemeMatchesPlanTokens() {
        assertRoles(
            scheme = DarkColors,
            expected = mapOf(
                "primary" to 0xFF9FD88A,
                "onPrimary" to 0xFF0B3900,
                "primaryContainer" to 0xFF245F17,
                "onPrimaryContainer" to 0xFFB2EDB0,
                "secondary" to 0xFFBCCBB1,
                "secondaryContainer" to 0xFF3D4B36,
                "tertiary" to 0xFF86D3D4,
                "tertiaryContainer" to 0xFF004F51,
                "error" to 0xFFFFB4AB,
                "background" to 0xFF121410,
                "surface" to 0xFF121410,
                "onBackground" to 0xFFE3E3DC,
                "onSurface" to 0xFFE3E3DC,
                "surfaceVariant" to 0xFF43483E,
                "onSurfaceVariant" to 0xFFC3C8BC,
                "outline" to 0xFF8D9387,
                "surfaceContainerLowest" to 0xFF0D0F0B,
                "surfaceContainerLow" to 0xFF1B1C18,
                "surfaceContainer" to 0xFF1F201C,
                "surfaceContainerHigh" to 0xFF292B26,
                "surfaceContainerHighest" to 0xFF343631,
            ),
        )
    }

    @Test
    fun derivedRolesMatchDocumentedValues() {
        val fixedRoles = mapOf(
            "primaryFixed" to 0xFFB2EDB0,
            "primaryFixedDim" to 0xFF9FD88A,
            "onPrimaryFixed" to 0xFF0A2A05,
            "onPrimaryFixedVariant" to 0xFF245F17,
            "secondaryFixed" to 0xFFD8E7CB,
            "secondaryFixedDim" to 0xFFBCCBB1,
            "onSecondaryFixed" to 0xFF121F0E,
            "onSecondaryFixedVariant" to 0xFF3D4B36,
            "tertiaryFixed" to 0xFFA8EFF0,
            "tertiaryFixedDim" to 0xFF86D3D4,
            "onTertiaryFixed" to 0xFF002020,
            "onTertiaryFixedVariant" to 0xFF004F51,
        )
        assertRoles(
            scheme = LightColors,
            expected = fixedRoles + mapOf(
                "onSecondary" to 0xFFFFFFFF,
                "onSecondaryContainer" to 0xFF121F0E,
                "onTertiary" to 0xFFFFFFFF,
                "onTertiaryContainer" to 0xFF002020,
                "onError" to 0xFFFFFFFF,
                "errorContainer" to 0xFFFFDAD6,
                "onErrorContainer" to 0xFF410002,
                "outlineVariant" to 0xFFC3C8BC,
                "inverseSurface" to 0xFF30312C,
                "inverseOnSurface" to 0xFFF2F1EA,
                "inversePrimary" to 0xFF9FD88A,
                "surfaceTint" to 0xFF3B7D2C,
                "surfaceBright" to 0xFFF5F5EE,
                "surfaceDim" to 0xFFDBDBD3,
                "scrim" to 0xFF000000,
            ),
        )
        assertRoles(
            scheme = DarkColors,
            expected = fixedRoles + mapOf(
                "onSecondary" to 0xFF273422,
                "onSecondaryContainer" to 0xFFD8E7CB,
                "onTertiary" to 0xFF003738,
                "onTertiaryContainer" to 0xFFA8EFF0,
                "onError" to 0xFF690005,
                "errorContainer" to 0xFF93000A,
                "onErrorContainer" to 0xFFFFDAD6,
                "outlineVariant" to 0xFF43483E,
                "inverseSurface" to 0xFFE3E3DC,
                "inverseOnSurface" to 0xFF30312C,
                "inversePrimary" to 0xFF3B7D2C,
                "surfaceTint" to 0xFF9FD88A,
                "surfaceBright" to 0xFF383A35,
                "surfaceDim" to 0xFF121410,
                "scrim" to 0xFF000000,
            ),
        )
    }

    @Test
    fun brandGreenIsNotUsedByAnyRole() {
        listOf(LightColors, DarkColors).forEach { scheme ->
            val rolesUsingBrandGreen = scheme.roles().filterValues { it == BrandGreen }.keys
            assertTrue("BrandGreen must stay decorative, found in $rolesUsingBrandGreen", rolesUsingBrandGreen.isEmpty())
        }
    }

    private fun assertRoles(scheme: ColorScheme, expected: Map<String, Long>) {
        val actual = scheme.roles()
        val mismatches = expected.mapNotNull { (role, hex) ->
            val color = actual[role] ?: return@mapNotNull "$role: unknown role"
            if (color == Color(hex)) null else "$role: expected ${Color(hex).hex()}, was ${color.hex()}"
        }
        assertTrue(mismatches.joinToString(separator = "\n"), mismatches.isEmpty())
    }

    private fun Color.hex(): String = "#%08X".format(toArgb())

    /** Every color role of the scheme, by name. */
    private fun ColorScheme.roles(): Map<String, Color> = mapOf(
        "primary" to primary,
        "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer,
        "inversePrimary" to inversePrimary,
        "secondary" to secondary,
        "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary,
        "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer,
        "onTertiaryContainer" to onTertiaryContainer,
        "background" to background,
        "onBackground" to onBackground,
        "surface" to surface,
        "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant,
        "surfaceTint" to surfaceTint,
        "inverseSurface" to inverseSurface,
        "inverseOnSurface" to inverseOnSurface,
        "error" to error,
        "onError" to onError,
        "errorContainer" to errorContainer,
        "onErrorContainer" to onErrorContainer,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "scrim" to scrim,
        "surfaceBright" to surfaceBright,
        "surfaceDim" to surfaceDim,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainerLowest" to surfaceContainerLowest,
        "primaryFixed" to primaryFixed,
        "primaryFixedDim" to primaryFixedDim,
        "onPrimaryFixed" to onPrimaryFixed,
        "onPrimaryFixedVariant" to onPrimaryFixedVariant,
        "secondaryFixed" to secondaryFixed,
        "secondaryFixedDim" to secondaryFixedDim,
        "onSecondaryFixed" to onSecondaryFixed,
        "onSecondaryFixedVariant" to onSecondaryFixedVariant,
        "tertiaryFixed" to tertiaryFixed,
        "tertiaryFixedDim" to tertiaryFixedDim,
        "onTertiaryFixed" to onTertiaryFixed,
        "onTertiaryFixedVariant" to onTertiaryFixedVariant,
    )
}
