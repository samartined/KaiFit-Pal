package com.tfg.kaifit_pal

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.ui.navigation.TopLevelDestination
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Launches the real activity, splash screen included, and checks the initial shell. */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun string(@StringRes id: Int): String = composeRule.activity.getString(id)

    @Test
    fun launchShowsCalculatorScreen() {
        composeRule.waitForIdle()

        // The top app bar title is the only non-selectable node with that text; the tab is the other.
        composeRule.onNode(hasText(string(R.string.calculator_title)) and !isSelectable()).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.calculator_placeholder)).assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.nav_calculator)) and isSelectable()).assertIsSelected()
    }

    @Test
    fun navigationBarHasFiveTabs() {
        composeRule.waitForIdle()

        // Navigation bar items are the only selectable nodes on screen.
        composeRule.onAllNodes(isSelectable()).assertCountEquals(5)
        TopLevelDestination.entries.forEach { destination ->
            composeRule.onNode(hasText(string(destination.labelRes)) and isSelectable()).assertIsDisplayed()
        }
    }
}
