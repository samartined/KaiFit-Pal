package com.tfg.kaifit_pal.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.tfg.kaifit_pal.ui.assistant.AssistantPreview
import com.tfg.kaifit_pal.ui.assistant.AssistantPreviewStates
import com.tfg.kaifit_pal.ui.calculator.CalculatorContent
import com.tfg.kaifit_pal.ui.calculator.CalculatorPreviewStates
import com.tfg.kaifit_pal.ui.calculator.CalculatorUiState
import com.tfg.kaifit_pal.ui.help.FirstHelpQuestion
import com.tfg.kaifit_pal.ui.help.HelpContent
import com.tfg.kaifit_pal.ui.navigation.KaiFitNavHost
import com.tfg.kaifit_pal.ui.profile.ProfileContent
import com.tfg.kaifit_pal.ui.profile.ProfilePreviewStates
import com.tfg.kaifit_pal.ui.profile.ProfileUiState
import com.tfg.kaifit_pal.ui.results.ResultsContent
import com.tfg.kaifit_pal.ui.results.ResultsPreviewStates
import com.tfg.kaifit_pal.ui.settings.SettingsPreview
import com.tfg.kaifit_pal.ui.settings.SettingsPreviewStates
import com.tfg.kaifit_pal.ui.settings.SettingsUiState
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

/**
 * Screenshots of every screen in the light and dark themes, from fixed states, on a 411 × 891 dp
 * phone in Spanish. The `_full` variants use a 1600 dp tall screen to show whole screens that
 * scroll, and `shell_w360_light` shows the navigation bar on a narrow 360 dp phone. On that
 * phone, `profile_saved_w360_light` shows the saved profile, and `results_w360_fs13_light` and
 * `assistant_chat_w360_fs13_light` the results and the chat with the font scaled to 130 %.
 * `results_w320_fs20_light` shows the results on a 320 dp phone with the font at 200 %.
 *
 * The time zone is fixed to Europe/Madrid, so the date of the saved profile is the same on any
 * machine.
 *
 * Kai-Q is captured in each of its states: the model missing (also with warnings and on a 32-bit
 * device), downloading, waiting for Wi-Fi, importing or failed, the engine loading or failed,
 * and the chat empty, streaming, full or running on the CPU. Settings show the model section
 * downloaded, missing and downloading.
 *
 * The PNG files are written only by `./gradlew recordRoborazziDebug`, under
 * app/build/outputs/roborazzi/; a normal test run only draws the screens.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PHONE)
class ScreenScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    @Before
    fun fixTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(SCREENSHOT_TIME_ZONE))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun calculatorEmptyLight() = capture("calculator_empty", darkTheme = false) { Calculator(CalculatorPreviewStates.empty) }

    @Test
    fun calculatorEmptyDark() = capture("calculator_empty", darkTheme = true) { Calculator(CalculatorPreviewStates.empty) }

    @Test
    fun calculatorFemaleLight() = capture("calculator_female", darkTheme = false) { Calculator(CalculatorPreviewStates.female) }

    @Test
    fun calculatorFemaleDark() = capture("calculator_female", darkTheme = true) { Calculator(CalculatorPreviewStates.female) }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun calculatorFemaleFullLight() =
        capture("calculator_female_full", darkTheme = false) { Calculator(CalculatorPreviewStates.female) }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun calculatorFemaleFullDark() =
        capture("calculator_female_full", darkTheme = true) { Calculator(CalculatorPreviewStates.female) }

    @Test
    fun calculatorErrorLight() = capture("calculator_error", darkTheme = false) { Calculator(CalculatorPreviewStates.heightError) }

    @Test
    fun calculatorErrorDark() = capture("calculator_error", darkTheme = true) { Calculator(CalculatorPreviewStates.heightError) }

    @Test
    fun resultsLight() = capture("results", darkTheme = false) { Results() }

    @Test
    fun resultsDark() = capture("results", darkTheme = true) { Results() }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun resultsFullLight() = capture("results_full", darkTheme = false) { Results() }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun resultsFullDark() = capture("results_full", darkTheme = true) { Results() }

    /** Results on a narrow phone with large text: the target calories stay on one line. */
    @Test
    @Config(qualifiers = NARROW_TALL_PHONE, fontScale = 1.3f)
    fun resultsNarrowLargeTextLight() = capture("results_w360_fs13", darkTheme = false) { Results() }

    /** Results on a 320 dp phone with text at 200 %: the labels only wrap between words. */
    @Test
    @Config(qualifiers = NARROWEST_TALL_PHONE, fontScale = 2.0f)
    fun resultsNarrowestLargestTextLight() = capture("results_w320_fs20", darkTheme = false) { Results() }

    @Test
    fun profileEmptyLight() = capture("profile_empty", darkTheme = false) { Profile(ProfileUiState.Empty) }

    @Test
    fun profileEmptyDark() = capture("profile_empty", darkTheme = true) { Profile(ProfileUiState.Empty) }

    @Test
    fun profileSavedLight() = capture("profile_saved", darkTheme = false) { Profile(ProfilePreviewStates.saved) }

    @Test
    fun profileSavedDark() = capture("profile_saved", darkTheme = true) { Profile(ProfilePreviewStates.saved) }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun profileSavedFullLight() = capture("profile_saved_full", darkTheme = false) { Profile(ProfilePreviewStates.saved) }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun profileSavedFullDark() = capture("profile_saved_full", darkTheme = true) { Profile(ProfilePreviewStates.saved) }

    /** The saved profile on a narrow phone: labels and values wrap between words. */
    @Test
    @Config(qualifiers = NARROW_TALL_PHONE)
    fun profileSavedNarrowLight() =
        capture("profile_saved_w360", darkTheme = false) { Profile(ProfilePreviewStates.saved) }

    @Test
    fun helpLight() = capture("help", darkTheme = false) { HelpContent(initiallyExpanded = setOf(FirstHelpQuestion)) }

    @Test
    fun helpDark() = capture("help", darkTheme = true) { HelpContent(initiallyExpanded = setOf(FirstHelpQuestion)) }

    @Test
    fun settingsLight() = capture("settings", darkTheme = false) { Settings() }

    @Test
    fun settingsDark() = capture("settings", darkTheme = true) { Settings() }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun settingsFullLight() = capture("settings_full", darkTheme = false) { Settings() }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun settingsFullDark() = capture("settings_full", darkTheme = true) { Settings() }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun settingsModelNotDownloadedLight() =
        capture("settings_model_not_downloaded", darkTheme = false) { Settings(SettingsPreviewStates.modelNotDownloaded) }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun settingsModelDownloadingLight() =
        capture("settings_model_downloading", darkTheme = false) { Settings(SettingsPreviewStates.modelDownloading) }

    @Test
    fun assistantNotDownloadedLight() =
        capture("assistant_not_downloaded", darkTheme = false) { AssistantPreview(AssistantPreviewStates.notDownloaded) }

    @Test
    fun assistantNotDownloadedDark() =
        capture("assistant_not_downloaded", darkTheme = true) { AssistantPreview(AssistantPreviewStates.notDownloaded) }

    /** 4 GB of RAM (only a warning), 2 GB free (download disabled) and mobile data allowed. */
    @Test
    fun assistantNotDownloadedWarningsLight() = capture("assistant_not_downloaded_warnings", darkTheme = false) {
        AssistantPreview(AssistantPreviewStates.notDownloadedWithWarnings)
    }

    @Test
    fun assistantUnsupportedLight() =
        capture("assistant_unsupported", darkTheme = false) { AssistantPreview(AssistantPreviewStates.unsupported) }

    @Test
    fun assistantDownloadingLight() =
        capture("assistant_downloading", darkTheme = false) { AssistantPreview(AssistantPreviewStates.downloading) }

    @Test
    fun assistantDownloadingDark() =
        capture("assistant_downloading", darkTheme = true) { AssistantPreview(AssistantPreviewStates.downloading) }

    @Test
    fun assistantWaitingWifiLight() =
        capture("assistant_waiting_wifi", darkTheme = false) { AssistantPreview(AssistantPreviewStates.waitingForWifi) }

    @Test
    fun assistantImportingLight() =
        capture("assistant_importing", darkTheme = false) { AssistantPreview(AssistantPreviewStates.importing) }

    @Test
    fun assistantDownloadErrorLight() =
        capture("assistant_download_error", darkTheme = false) { AssistantPreview(AssistantPreviewStates.downloadError) }

    @Test
    fun assistantLoadingLight() = capture("assistant_loading", darkTheme = false) { AssistantPreview(AssistantPreviewStates.loading) }

    @Test
    fun assistantLoadingDark() = capture("assistant_loading", darkTheme = true) { AssistantPreview(AssistantPreviewStates.loading) }

    @Test
    fun assistantEngineErrorLight() =
        capture("assistant_engine_error", darkTheme = false) { AssistantPreview(AssistantPreviewStates.engineError) }

    @Test
    fun assistantChatEmptyLight() =
        capture("assistant_chat_empty", darkTheme = false) { AssistantPreview(AssistantPreviewStates.chatEmpty) }

    @Test
    fun assistantChatEmptyDark() =
        capture("assistant_chat_empty", darkTheme = true) { AssistantPreview(AssistantPreviewStates.chatEmpty) }

    @Test
    fun assistantChatStreamingLight() =
        capture("assistant_chat_streaming", darkTheme = false) { AssistantPreview(AssistantPreviewStates.chatStreaming) }

    @Test
    fun assistantChatStreamingDark() =
        capture("assistant_chat_streaming", darkTheme = true) { AssistantPreview(AssistantPreviewStates.chatStreaming) }

    @Test
    fun assistantChatContextFullLight() =
        capture("assistant_chat_context_full", darkTheme = false) { AssistantPreview(AssistantPreviewStates.chatContextFull) }

    @Test
    fun assistantChatCpuFallbackLight() =
        capture("assistant_chat_cpu_fallback", darkTheme = false) { AssistantPreview(AssistantPreviewStates.chatCpuFallback) }

    /** The chat on a narrow phone with large text: bubbles, notice and buttons still fit. */
    @Test
    @Config(qualifiers = NARROW_PHONE, fontScale = 1.3f)
    fun assistantChatNarrowLargeTextLight() =
        capture("assistant_chat_w360_fs13", darkTheme = false) { AssistantPreview(AssistantPreviewStates.chatStreaming) }

    /** The whole shell on a narrow phone, to check how the tab labels fit. */
    @Test
    @Config(qualifiers = NARROW_PHONE)
    fun shellNarrowLight() = capture("shell_w360", darkTheme = false) { KaiFitNavHost() }

    private fun capture(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            KaiFitTheme(darkTheme = darkTheme) {
                content()
            }
        }
        composeRule.waitForIdle()
        val theme = if (darkTheme) "dark" else "light"
        composeRule.onRoot().captureRoboImage("$OUTPUT_DIR/${name}_$theme.png")
    }

    private companion object {
        const val OUTPUT_DIR = "build/outputs/roborazzi"
        const val SCREENSHOT_TIME_ZONE = "Europe/Madrid"
    }
}

private const val PHONE = "es-rES-w411dp-h891dp-xxhdpi"
private const val TALL_PHONE = "es-rES-w411dp-h1600dp-xxhdpi"
private const val NARROW_PHONE = "es-rES-w360dp-h800dp-xxhdpi"
private const val NARROW_TALL_PHONE = "es-rES-w360dp-h1600dp-xxhdpi"
private const val NARROWEST_TALL_PHONE = "es-rES-w320dp-h1600dp-xxhdpi"

@Composable
private fun Calculator(state: CalculatorUiState) {
    CalculatorContent(
        state = state,
        onSexChange = {},
        onAgeChange = {},
        onHeightChange = {},
        onWeightChange = {},
        onNeckChange = {},
        onWaistChange = {},
        onHipChange = {},
        onActivityLevelChange = {},
        onCalculate = {},
    )
}

@Composable
private fun Results() {
    ResultsContent(
        state = ResultsPreviewStates.manAtModerateHighDeficit,
        onBack = {},
        onGoalChange = {},
        onProteinChange = {},
        onFatChange = {},
        onSave = {},
        onAskAssistant = {},
    )
}

@Composable
private fun Profile(state: ProfileUiState) {
    ProfileContent(state = state, onGoToCalculator = {}, onRecalculate = {}, onClearProfile = {})
}

/** Settings with the model downloaded, 8 GB of RAM and 20 GB free, unless [state] says otherwise. */
@Composable
private fun Settings(state: SettingsUiState = SettingsPreviewStates.modelDownloaded) {
    SettingsPreview(state)
}
