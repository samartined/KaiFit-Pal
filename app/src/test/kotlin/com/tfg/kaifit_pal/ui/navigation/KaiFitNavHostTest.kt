package com.tfg.kaifit_pal.ui.navigation

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.KaiFitApp
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.TdeeCalculator
import com.tfg.kaifit_pal.testing.ReferenceData
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es-rES")
class KaiFitNavHostTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Title of the screen of each tab, and a text near its top that only that screen shows. */
    private val screenTexts = mapOf(
        TopLevelDestination.PROFILE to (R.string.profile_title to R.string.profile_go_to_calculator),
        TopLevelDestination.CALCULATOR to (R.string.calculator_title to R.string.calculator_sex),
        TopLevelDestination.ASSISTANT to (R.string.assistant_title to R.string.assistant_placeholder),
        TopLevelDestination.HELP to (R.string.help_title to R.string.help_placeholder),
        TopLevelDestination.SETTINGS to (R.string.settings_title to R.string.settings_section_appearance),
    )

    private fun setShell() {
        composeRule.setContent {
            KaiFitTheme {
                KaiFitNavHost()
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun startsOnCalculatorWithItsTabSelected() {
        setShell()

        assertScreenShown(TopLevelDestination.CALCULATOR)
        assertOnlySelectedTab(TopLevelDestination.CALCULATOR)
    }

    @Test
    fun eachTabShowsItsScreenAndBecomesSelected() {
        setShell()

        TopLevelDestination.entries.forEach { destination ->
            tab(destination).performClick()
            composeRule.waitForIdle()

            assertScreenShown(destination)
            assertOnlySelectedTab(destination)
        }
    }

    @Test
    fun calculatingShowsResultsWithoutNavigationBarAndBackKeepsTheForm() {
        setShell()
        enterReferenceMan()

        composeRule.onNodeWithText(string(R.string.calculator_calculate)).performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasText(string(R.string.results_title))).assertIsDisplayed()
        composeRule.onNodeWithText("2819").assertIsDisplayed()
        composeRule.onAllNodes(isTab).assertCountEquals(0)

        composeRule.onNodeWithContentDescription(string(R.string.action_back)).performClick()
        composeRule.waitForIdle()

        assertScreenShown(TopLevelDestination.CALCULATOR)
        assertOnlySelectedTab(TopLevelDestination.CALCULATOR)
        composeRule.onNodeWithText(string(R.string.results_title)).assertDoesNotExist()
        field(R.string.calculator_height).assertTextContains("180")
        field(R.string.calculator_weight).assertTextContains("80")
        field(R.string.calculator_neck).assertTextContains("38")
        field(R.string.calculator_waist).assertTextContains("85")
        composeRule.onNodeWithText(activityText(R.string.activity_moderate, "1,55")).assertExists()
    }

    @Test
    fun calculatorFormSurvivesSwitchingToAnotherTabAndBack() {
        setShell()
        field(R.string.calculator_height).performTextInput("180")

        tab(TopLevelDestination.HELP).performClick()
        composeRule.waitForIdle()
        assertScreenShown(TopLevelDestination.HELP)
        tab(TopLevelDestination.CALCULATOR).performClick()
        composeRule.waitForIdle()

        assertScreenShown(TopLevelDestination.CALCULATOR)
        field(R.string.calculator_height).assertTextContains("180")
    }

    @Test
    fun calculatorTabAfterAskingTheAssistantReturnsToResults() {
        setShell()
        enterReferenceMan()
        composeRule.onNodeWithText(string(R.string.calculator_calculate)).performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.results_ask_assistant)).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertScreenShown(TopLevelDestination.ASSISTANT)
        assertOnlySelectedTab(TopLevelDestination.ASSISTANT)

        tab(TopLevelDestination.CALCULATOR).performClick()
        composeRule.waitForIdle()

        // The calculator tab gets back its own back stack, which ended on Results: the detail
        // screen is shown again, without the navigation bar, and its back arrow still works.
        composeRule.onNode(hasText(string(R.string.results_title))).assertIsDisplayed()
        composeRule.onNodeWithText("2819").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodes(isTab).assertCountEquals(0)

        composeRule.onNodeWithContentDescription(string(R.string.action_back)).performClick()
        composeRule.waitForIdle()

        assertScreenShown(TopLevelDestination.CALCULATOR)
        assertOnlySelectedTab(TopLevelDestination.CALCULATOR)
        field(R.string.calculator_height).assertTextContains("180")
    }

    @Test
    fun recalculateFromProfileOpensCalculatorFilledWithTheSavedProfile() {
        saveReferenceManInAppProfile()
        setShell()

        tab(TopLevelDestination.PROFILE).performClick()
        waitForText(R.string.profile_recalculate)
        composeRule.onNodeWithText(string(R.string.profile_recalculate)).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertOnlySelectedTab(TopLevelDestination.CALCULATOR)
        waitForHeight("180")
        field(R.string.calculator_height).assertTextContains("180")
        field(R.string.calculator_weight).assertTextContains("80")
    }

    @Test
    fun recalculateAfterAskingTheAssistantFromResultsShowsTheForm() {
        saveReferenceManInAppProfile()
        setShell()
        composeRule.onNodeWithText(string(R.string.sex_male)).performScrollTo().performClick()
        field(R.string.calculator_height).performTextInput("175")
        field(R.string.calculator_weight).performTextInput("70")
        composeRule.onNodeWithText(string(R.string.calculator_calculate)).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.results_ask_assistant)).performScrollTo().performClick()
        composeRule.waitForIdle()

        tab(TopLevelDestination.PROFILE).performClick()
        waitForText(R.string.profile_recalculate)
        composeRule.onNodeWithText(string(R.string.profile_recalculate)).performScrollTo().performClick()
        composeRule.waitForIdle()

        // The calculator tab would come back on Results; "Recalcular" asks for the form instead.
        assertScreenShown(TopLevelDestination.CALCULATOR)
        assertOnlySelectedTab(TopLevelDestination.CALCULATOR)
        composeRule.onNodeWithText(string(R.string.results_title)).assertDoesNotExist()
        waitForHeight("180")
        field(R.string.calculator_weight).assertTextContains("80")
    }

    @Test
    fun systemBackFromHelpReturnsToCalculator() {
        setShell()
        tab(TopLevelDestination.HELP).performClick()
        composeRule.waitForIdle()
        assertScreenShown(TopLevelDestination.HELP)

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        assertScreenShown(TopLevelDestination.CALCULATOR)
        assertOnlySelectedTab(TopLevelDestination.CALCULATOR)
        composeRule.onNodeWithText(string(R.string.help_placeholder)).assertDoesNotExist()
    }

    @Test
    fun switchingTabsKeepsOnlyStartAndCurrentTabOnBackStack() {
        setShell()
        listOf(
            TopLevelDestination.PROFILE,
            TopLevelDestination.HELP,
            TopLevelDestination.SETTINGS,
            // Reselecting the current tab must not add a second copy of it.
            TopLevelDestination.SETTINGS,
        ).forEach { destination ->
            tab(destination).performClick()
            composeRule.waitForIdle()
        }
        assertScreenShown(TopLevelDestination.SETTINGS)

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        // A single back skips the tabs visited before and lands on the start destination.
        assertScreenShown(TopLevelDestination.CALCULATOR)
        assertOnlySelectedTab(TopLevelDestination.CALCULATOR)
        composeRule.onNode(hasText(string(R.string.settings_title)) and !isTab).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.settings_section_appearance)).assertDoesNotExist()
    }

    private fun string(@StringRes id: Int, vararg formatArgs: Any): String =
        composeRule.activity.getString(id, *formatArgs)

    private fun activityText(@StringRes levelRes: Int, factor: String): String =
        string(R.string.activity_level_with_factor, string(levelRes), factor)

    /** The text field whose label is [labelRes]. */
    private fun field(@StringRes labelRes: Int): SemanticsNodeInteraction =
        composeRule.onNode(hasSetTextAction() and hasText(string(labelRes)))

    /** Man of 30 years, 180 cm, 80 kg, neck 38 and waist 85 cm, moderately active: TDEE 2819 kcal. */
    private fun enterReferenceMan() {
        composeRule.onNodeWithText(string(R.string.sex_male)).performScrollTo().performClick()
        field(R.string.calculator_height).performTextInput("180")
        field(R.string.calculator_weight).performTextInput("80")
        field(R.string.calculator_neck).performTextInput("38")
        field(R.string.calculator_waist).performTextInput("85")
        composeRule.onNodeWithText(activityText(R.string.activity_sedentary, "1,2")).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(activityText(R.string.activity_moderate, "1,55")).performClick()
        composeRule.waitForIdle()
    }

    /** Saves the reference man at −15 % as the profile of the app, before the shell is shown. */
    private fun saveReferenceManInAppProfile() {
        val profiles = (composeRule.activity.application as KaiFitApp).container.profileRepository
        val tdee = checkNotNull(TdeeCalculator.calculate(ReferenceData.man, ReferenceData.manActivity))
        val modifier = GoalModifier(-15)
        runBlocking {
            profiles.save(
                ReferenceData.man,
                ReferenceData.manActivity,
                tdee,
                modifier,
                MacroPlan.forGoal(modifier.applyTo(tdee.tdeeKcal), modifier.goal),
            )
        }
    }

    /** Waits until the height field shows [text]; the saved profile is read from disk first. */
    private fun waitForHeight(text: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasSetTextAction() and hasText(string(R.string.calculator_height)) and hasText(text))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(@StringRes id: Int) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(string(id))).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Navigation bar items have the tab role; the label tells them apart. */
    private fun tab(destination: TopLevelDestination): SemanticsNodeInteraction =
        composeRule.onNode(hasText(string(destination.labelRes)) and isTab)

    private fun assertScreenShown(destination: TopLevelDestination) {
        val (titleRes, markerRes) = screenTexts.getValue(destination)
        // The top app bar title is the only node with that text that is not a tab.
        composeRule.onNode(hasText(string(titleRes)) and !isTab).assertIsDisplayed()
        // The profile is read from disk first, so its content may take a moment. The marker may be
        // scrolled out of view when a screen keeps its scroll position.
        waitForText(markerRes)
        composeRule.onNodeWithText(string(markerRes)).assertExists()
    }

    private fun assertOnlySelectedTab(selected: TopLevelDestination) {
        TopLevelDestination.entries.forEach { destination ->
            if (destination == selected) {
                tab(destination).assertIsSelected()
            } else {
                tab(destination).assertIsNotSelected()
            }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L

        val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    }
}
