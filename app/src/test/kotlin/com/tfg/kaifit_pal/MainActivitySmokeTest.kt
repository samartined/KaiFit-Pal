package com.tfg.kaifit_pal

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.ui.navigation.TopLevelDestination
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Launches the real activity, splash screen and saved theme included, and checks the initial shell. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es-rES")
class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Navigation bar items have the tab role. */
    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun string(@StringRes id: Int): String = composeRule.activity.getString(id)

    @Test
    fun launchShowsCalculatorScreen() {
        composeRule.waitForIdle()

        // The top app bar title is the only node with that text that is not a tab.
        composeRule.onNode(hasText(string(R.string.calculator_title)) and !isTab).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.calculator_calculate))
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeRule.onNode(hasText(string(R.string.nav_calculator)) and isTab).assertIsSelected()
    }

    @Test
    fun navigationBarHasFiveTabs() {
        composeRule.waitForIdle()

        composeRule.onAllNodes(isTab).assertCountEquals(5)
        TopLevelDestination.entries.forEach { destination ->
            composeRule.onNode(hasText(string(destination.labelRes)) and isTab).assertIsDisplayed()
        }
    }
}
