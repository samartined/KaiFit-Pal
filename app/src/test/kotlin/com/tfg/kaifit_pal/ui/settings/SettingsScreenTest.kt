package com.tfg.kaifit_pal.ui.settings

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.data.settings.SettingsRepository
import com.tfg.kaifit_pal.data.settings.ThemeMode
import com.tfg.kaifit_pal.testing.TemporaryDataStores
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es-rES")
class SettingsScreenTest {

    @get:Rule(order = 0)
    val dataStores = TemporaryDataStores()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun choosingDarkThemeAndDynamicColourSavesBoth() {
        val settings = SettingsRepository(dataStores.newDataStore())
        val viewModel = SettingsViewModel(settings, dynamicColorAvailable = true)
        composeRule.setContent {
            KaiFitTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        themeRow(R.string.settings_theme_system).assertIsSelected()

        themeRow(R.string.settings_theme_dark).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(string(R.string.settings_theme_dark)) and isSelected())
                .fetchSemanticsNodes().isNotEmpty()
        }

        themeRow(R.string.settings_theme_dark).assertIsSelected()
        themeRow(R.string.settings_theme_system).assertIsNotSelected()
        assertEquals(ThemeMode.DARK, runBlocking { settings.settings.first() }.themeMode)

        dynamicColorRow().assertIsOff().performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(string(R.string.settings_dynamic_color)) and isOn())
                .fetchSemanticsNodes().isNotEmpty()
        }

        dynamicColorRow().assertIsOn()
        assertTrue(runBlocking { settings.settings.first() }.dynamicColor)
    }

    @Test
    fun dynamicColourIsDisabledAndExplainedBeforeAndroid12() {
        composeRule.setContent {
            KaiFitTheme {
                SettingsContent(
                    state = SettingsUiState(AppSettings.DEFAULT, dynamicColorAvailable = false),
                    onThemeModeChange = {},
                    onDynamicColorChange = {},
                )
            }
        }

        dynamicColorRow().assertIsNotEnabled().assertIsOff()
        composeRule.onNodeWithText(string(R.string.settings_dynamic_color_unavailable), substring = true)
            .assertIsDisplayed()
    }

    private fun string(@StringRes id: Int): String = composeRule.activity.getString(id)

    private fun themeRow(@StringRes labelRes: Int) = composeRule.onNodeWithText(string(labelRes))

    private fun dynamicColorRow() = composeRule.onNodeWithText(string(R.string.settings_dynamic_color))

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
