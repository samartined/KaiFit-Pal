package com.tfg.kaifit_pal.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.ui.assistant.AssistantScreen
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
import com.tfg.kaifit_pal.ui.settings.SettingsContent
import com.tfg.kaifit_pal.ui.settings.SettingsUiState
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of every screen in the light and dark themes, from fixed states, on a 411 × 891 dp
 * phone in Spanish. The `_full` variants use a 1600 dp tall screen to show whole screens that
 * scroll, and `shell_w360_light` shows the navigation bar on a narrow 360 dp phone.
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

    @Test
    fun helpLight() = capture("help", darkTheme = false) { HelpContent(initiallyExpanded = setOf(FirstHelpQuestion)) }

    @Test
    fun helpDark() = capture("help", darkTheme = true) { HelpContent(initiallyExpanded = setOf(FirstHelpQuestion)) }

    @Test
    fun settingsLight() = capture("settings", darkTheme = false) { Settings() }

    @Test
    fun settingsDark() = capture("settings", darkTheme = true) { Settings() }

    @Test
    fun assistantLight() = capture("assistant", darkTheme = false) { AssistantScreen() }

    @Test
    fun assistantDark() = capture("assistant", darkTheme = true) { AssistantScreen() }

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
    }
}

private const val PHONE = "es-rES-w411dp-h891dp-xxhdpi"
private const val TALL_PHONE = "es-rES-w411dp-h1600dp-xxhdpi"
private const val NARROW_PHONE = "es-rES-w360dp-h800dp-xxhdpi"

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

@Composable
private fun Settings() {
    SettingsContent(
        state = SettingsUiState(AppSettings.DEFAULT, dynamicColorAvailable = true),
        onThemeModeChange = {},
        onDynamicColorChange = {},
    )
}
