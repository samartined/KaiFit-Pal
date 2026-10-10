package com.tfg.kaifit_pal.ui

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.testing.MinTouchTarget
import com.tfg.kaifit_pal.testing.describeTouchTarget
import com.tfg.kaifit_pal.testing.hasMinimumTouchTarget
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
import com.tfg.kaifit_pal.ui.results.goalLabelRes
import com.tfg.kaifit_pal.ui.settings.SettingsPreview
import com.tfg.kaifit_pal.ui.settings.SettingsPreviewStates
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Accessibility of every screen, in the fixed states of the screenshots, in Spanish and in the
 * light theme:
 * - everything that can be tapped or dragged has a touch target of at least 48 × 48 dp, both in
 *   what accessibility services get and in the room it takes on screen (AX6);
 * - each screen has headings, and its title is one (AX7);
 * - the sliders say what they are set to (AX8);
 * - the questions of Help say whether their answer is shown (AX9);
 * - a wrong field says so, with its message (AX10).
 *
 * The screen is 1600 dp tall, so whole screens fit without scrolling and no target is measured
 * cut by the edge of a scrolling container.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es-rES-w411dp-h1600dp-xxhdpi")
class AccessibilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // AX6

    @Test
    fun calculatorTouchTargets() = assertTouchTargets { Calculator() }

    @Test
    fun resultsTouchTargets() = assertTouchTargets { Results() }

    @Test
    fun savedProfileTouchTargets() = assertTouchTargets { Profile(ProfilePreviewStates.saved) }

    @Test
    fun emptyProfileTouchTargets() = assertTouchTargets { Profile(ProfileUiState.Empty) }

    @Test
    fun helpTouchTargets() = assertTouchTargets { Help() }

    @Test
    fun settingsTouchTargets() = assertTouchTargets { SettingsPreview(SettingsPreviewStates.modelDownloaded) }

    @Test
    fun assistantEmptyChatTouchTargets() = assertTouchTargets { AssistantPreview(AssistantPreviewStates.chatEmpty) }

    @Test
    fun assistantWithoutModelTouchTargets() = assertTouchTargets { AssistantPreview(AssistantPreviewStates.notDownloaded) }

    /** The navigation bar, around the calculator. */
    @Test
    fun navigationBarTouchTargets() = assertTouchTargets { KaiFitNavHost() }

    // AX7

    @Test
    fun calculatorHeadings() = assertHeadings(R.string.calculator_title) { Calculator() }

    @Test
    fun resultsHeadings() = assertHeadings(R.string.results_title) { Results() }

    @Test
    fun savedProfileHeadings() = assertHeadings(R.string.profile_title) { Profile(ProfilePreviewStates.saved) }

    @Test
    fun emptyProfileHeadings() = assertHeadings(R.string.profile_title, R.string.profile_empty_title) {
        Profile(ProfileUiState.Empty)
    }

    @Test
    fun helpHeadings() = assertHeadings(R.string.help_title) { Help() }

    @Test
    fun settingsHeadings() = assertHeadings(
        R.string.settings_title,
        R.string.settings_section_appearance,
        R.string.settings_section_model,
        R.string.settings_model_name,
        R.string.settings_section_about,
    ) {
        SettingsPreview(SettingsPreviewStates.modelDownloaded)
    }

    @Test
    fun assistantEmptyChatHeadings() = assertHeadings(R.string.assistant_title, R.string.assistant_empty_title) {
        AssistantPreview(AssistantPreviewStates.chatEmpty)
    }

    @Test
    fun assistantWithoutModelHeadings() = assertHeadings(R.string.assistant_title, R.string.model_card_title) {
        AssistantPreview(AssistantPreviewStates.notDownloaded)
    }

    // AX8
    @Test
    fun resultsSlidersSayWhatTheyAreSetTo() {
        setContent { Results() }
        val state = ResultsPreviewStates.manAtModerateHighDeficit
        val goalLabel = string(goalLabelRes(state.goalModifier))
        val target = string(R.string.value_kcal_per_day, state.targetKcal)

        // "Déficit moderado-intenso, 2396 kcal/día".
        composeRule.onNode(hasSetProgressAction() and hasContentDescription(string(R.string.results_goal_slider)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "$goalLabel, $target"))
        composeRule.onNode(hasSetProgressAction() and hasContentDescription(string(R.string.macro_protein)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "${state.macroPlan.proteinGrams} g"))
        composeRule.onNode(hasSetProgressAction() and hasContentDescription(string(R.string.macro_fat)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "${state.macroPlan.fatGrams} g"))
        // The goal and its calories are read together, as are the hero card and each macro.
        composeRule.onNode(hasText(goalLabel) and hasText(target)).assertExists()
        composeRule.onNode(hasText(string(R.string.macro_carbs)) and hasText("${state.macroPlan.carbGrams} g")).assertExists()
    }

    // AX9
    @Test
    fun helpQuestionsSayWhetherTheirAnswerIsShown() {
        setContent { Help() }
        val expanded = string(R.string.expandable_expanded)
        val collapsed = string(R.string.expandable_collapsed)

        composeRule.onNode(hasText(string(FirstHelpQuestion)) and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expanded))
        val second = composeRule.onNode(hasText(string(R.string.help_q_neck)) and hasClickAction())
        second.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, collapsed))
        composeRule.onNodeWithText(string(R.string.help_a_neck)).assertDoesNotExist()

        second.performClick()
        composeRule.waitForIdle()

        second.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expanded))
        composeRule.onNodeWithText(string(R.string.help_a_neck)).assertExists()

        second.performClick()
        composeRule.waitForIdle()

        second.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, collapsed))
    }

    // AX10
    @Test
    fun calculatorFieldErrorIsExposedWithItsMessage() {
        setContent { Calculator(CalculatorPreviewStates.heightError) }

        // TalkBack reads the error state and, with the field, the message under it.
        composeRule.onNode(hasSetTextAction() and hasText(string(R.string.calculator_height)))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(hasText(string(R.string.calculator_error_out_of_range, "100", "250", "cm")))
        composeRule.onNode(hasSetTextAction() and hasText(string(R.string.calculator_weight)))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            KaiFitTheme(darkTheme = false) {
                content()
            }
        }
        composeRule.waitForIdle()
    }

    /** Every node that can be tapped or dragged has a touch target of at least 48 × 48 dp. */
    private fun assertTouchTargets(content: @Composable () -> Unit) {
        setContent(content)
        val targets = composeRule.onAllNodes(hasClickAction() or hasSetProgressAction()).fetchSemanticsNodes()
        assertTrue("no node can be tapped", targets.isNotEmpty())
        val tooSmall = targets.filterNot { it.hasMinimumTouchTarget() }.map { it.describeTouchTarget() }
        assertEquals("touch targets under $MinTouchTarget", emptyList<String>(), tooSmall)
    }

    /** The screen has headings, among them its title and every one of [otherHeadings]. */
    private fun assertHeadings(@StringRes titleRes: Int, @StringRes vararg otherHeadings: Int, content: @Composable () -> Unit) {
        setContent(content)
        val headings = composeRule.onAllNodes(isHeading).fetchSemanticsNodes()
        assertTrue("no headings", headings.isNotEmpty())
        (listOf(titleRes) + otherHeadings.toList()).forEach { id ->
            composeRule.onNode(isHeading and hasText(string(id))).assertExists()
        }
    }

    private fun string(@StringRes id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    private companion object {
        val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

        /** Sliders and other nodes whose value can be dragged; ui-test has no such matcher. */
        fun hasSetProgressAction(): SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
    }
}

@Composable
private fun Calculator(state: CalculatorUiState = CalculatorPreviewStates.female) {
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
private fun Help() {
    HelpContent(initiallyExpanded = setOf(FirstHelpQuestion))
}
