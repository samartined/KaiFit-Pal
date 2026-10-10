package com.tfg.kaifit_pal.ui.settings

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.data.settings.InferenceBackend
import com.tfg.kaifit_pal.data.settings.SettingsRepository
import com.tfg.kaifit_pal.data.settings.ThemeMode
import com.tfg.kaifit_pal.testing.FakeLlmRepository
import com.tfg.kaifit_pal.testing.FakeModelManager
import com.tfg.kaifit_pal.testing.TemporaryDataStores
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    private val events = mutableListOf<String>()

    private fun setContent(state: SettingsUiState) {
        composeRule.setContent {
            KaiFitTheme {
                SettingsContent(
                    state = state,
                    onThemeModeChange = { events += "theme:$it" },
                    onDynamicColorChange = { events += "dynamicColor:$it" },
                    onInferenceBackendChange = { events += "backend:$it" },
                    onDownloadOnWifiOnlyChange = { events += "wifiOnly:$it" },
                    onDownloadModel = { events += "download" },
                    onImportModel = { events += "import" },
                    onCancelModelTransfer = { events += "cancel" },
                    onDeleteModel = { events += "delete" },
                    onDismissModelError = { events += "dismiss" },
                )
            }
        }
    }

    @Test
    fun choosingDarkThemeAndDynamicColourSavesBoth() {
        val settings = SettingsRepository(dataStores.newDataStore())
        val viewModel = SettingsViewModel(
            settings,
            dynamicColorAvailable = true,
            modelManager = FakeModelManager(),
            llmRepository = FakeLlmRepository(),
            releaseScope = CoroutineScope(Dispatchers.Unconfined),
        )
        composeRule.setContent {
            KaiFitTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(string(R.string.settings_theme_system)) and isSelected())
                .fetchSemanticsNodes().isNotEmpty()
        }

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
        setContent(SettingsPreviewStates.modelDownloaded.copy(settings = AppSettings.DEFAULT, dynamicColorAvailable = false))

        dynamicColorRow().assertIsNotEnabled().assertIsOff()
        composeRule.onNodeWithText(string(R.string.settings_dynamic_color_unavailable), substring = true)
            .assertIsDisplayed()
    }

    // U26
    @Test
    fun downloadedModelShowsItsSizeAndIsDeletedAfterConfirming() {
        setContent(SettingsPreviewStates.modelDownloaded)

        composeRule.onNodeWithText(string(R.string.model_downloaded, "2,6\u00a0GB")).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.model_free_space, "20,0\u00a0GB")).assertExists()
        composeRule.onNodeWithText(string(R.string.settings_model_delete)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.settings_model_delete_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_model_delete_message, "2,6\u00a0GB")).assertIsDisplayed()
        assertTrue(events.isEmpty())
        composeRule.onNodeWithText(string(R.string.settings_model_delete_confirm)).performClick()

        assertEquals(listOf("delete"), events)
        composeRule.onNodeWithText(string(R.string.settings_model_delete_title)).assertDoesNotExist()
    }

    // U26
    @Test
    fun cancellingTheDeleteDialogKeepsTheModel() {
        setContent(SettingsPreviewStates.modelDownloaded)

        composeRule.onNodeWithText(string(R.string.settings_model_delete)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.settings_model_delete_cancel)).performClick()

        assertTrue(events.isEmpty())
        composeRule.onNodeWithText(string(R.string.settings_model_delete_title)).assertDoesNotExist()
    }

    // U26
    @Test
    fun processorAndWifiOnlyReportTheirChanges() {
        setContent(SettingsPreviewStates.modelDownloaded)

        composeRule.onNodeWithText(string(R.string.settings_backend_gpu)).performScrollTo().assertIsSelected()
        composeRule.onNodeWithText(string(R.string.settings_backend_cpu)).performScrollTo().assertIsNotSelected().performClick()
        composeRule.onNodeWithText(string(R.string.settings_wifi_only)).performScrollTo().assertIsOn().performClick()
        composeRule.onNodeWithText(string(R.string.settings_license_model)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_license_engine)).performScrollTo().assertIsDisplayed()

        assertEquals(listOf("backend:${InferenceBackend.CPU}", "wifiOnly:false"), events)
    }

    // U27
    @Test
    fun missingModelCanBeDownloadedOrImported() {
        setContent(SettingsPreviewStates.modelNotDownloaded)

        composeRule.onNodeWithText(string(R.string.settings_model_not_downloaded)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.model_download)).performScrollTo().assertIsEnabled().performClick()
        composeRule.onNodeWithText(string(R.string.model_import)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.settings_model_delete)).assertDoesNotExist()

        assertEquals(listOf("download", "import"), events)
    }

    // U27
    @Test
    fun downloadInProgressShowsItsPercentAndCancels() {
        setContent(SettingsPreviewStates.modelDownloading)

        composeRule.onNodeWithText("Descargando 42\u00a0%").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.model_cancel)).performScrollTo().performClick()

        assertEquals(listOf("cancel"), events)
    }

    // U27
    @Test
    fun loadingShowsNoDefaultValues() {
        setContent(SettingsUiState.Loading)

        composeRule.onNodeWithContentDescription(string(R.string.settings_loading)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_theme_system)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.settings_backend_gpu)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.settings_wifi_only)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.settings_dynamic_color)).assertDoesNotExist()
    }

    private fun string(@StringRes id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    private fun themeRow(@StringRes labelRes: Int) = composeRule.onNodeWithText(string(labelRes))

    private fun dynamicColorRow() = composeRule.onNodeWithText(string(R.string.settings_dynamic_color))

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
