package com.tfg.kaifit_pal.ui.navigation

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KaiFitNavHostTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Title and placeholder text that identify the screen of each tab. */
    private val screenTexts = mapOf(
        TopLevelDestination.PROFILE to (R.string.profile_title to R.string.profile_placeholder),
        TopLevelDestination.CALCULATOR to (R.string.calculator_title to R.string.calculator_placeholder),
        TopLevelDestination.ASSISTANT to (R.string.assistant_title to R.string.assistant_placeholder),
        TopLevelDestination.HELP to (R.string.help_title to R.string.help_placeholder),
        TopLevelDestination.SETTINGS to (R.string.settings_title to R.string.settings_placeholder),
    )

    @Before
    fun setUp() {
        composeRule.setContent {
            KaiFitTheme {
                KaiFitNavHost()
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun startsOnCalculatorWithItsTabSelected() {
        assertScreenShown(TopLevelDestination.CALCULATOR)
        assertOnlySelectedTab(TopLevelDestination.CALCULATOR)
    }

    @Test
    fun eachTabShowsItsScreenAndBecomesSelected() {
        TopLevelDestination.entries.forEach { destination ->
            tab(destination).performClick()
            composeRule.waitForIdle()

            assertScreenShown(destination)
            assertOnlySelectedTab(destination)
        }
    }

    @Test
    fun sampleResultsHideNavigationBarAndBackArrowReturnsToCalculator() {
        composeRule.onNodeWithText(string(R.string.calculator_show_sample_results)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.results_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.results_tdee_value, 2716)).assertIsDisplayed()
        composeRule.onAllNodes(isSelectable()).assertCountEquals(0)

        composeRule.onNodeWithContentDescription(string(R.string.action_back)).performClick()
        composeRule.waitForIdle()

        assertScreenShown(TopLevelDestination.CALCULATOR)
        assertOnlySelectedTab(TopLevelDestination.CALCULATOR)
        composeRule.onNodeWithText(string(R.string.results_title)).assertDoesNotExist()
    }

    @Test
    fun systemBackFromHelpReturnsToCalculator() {
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
        composeRule.onNode(hasText(string(R.string.settings_title)) and !isSelectable()).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.settings_placeholder)).assertDoesNotExist()
    }

    private fun string(@StringRes id: Int, vararg formatArgs: Any): String =
        composeRule.activity.getString(id, *formatArgs)

    /** Navigation bar items are the only selectable nodes; the label tells them apart. */
    private fun tab(destination: TopLevelDestination): SemanticsNodeInteraction =
        composeRule.onNode(hasText(string(destination.labelRes)) and isSelectable())

    private fun assertScreenShown(destination: TopLevelDestination) {
        val (titleRes, placeholderRes) = screenTexts.getValue(destination)
        // The top app bar title is the only non-selectable node with that text.
        composeRule.onNode(hasText(string(titleRes)) and !isSelectable()).assertIsDisplayed()
        composeRule.onNodeWithText(string(placeholderRes)).assertIsDisplayed()
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
}
