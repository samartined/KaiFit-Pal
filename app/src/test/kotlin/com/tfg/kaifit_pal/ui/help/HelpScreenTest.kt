package com.tfg.kaifit_pal.ui.help

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es-rES")
class HelpScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        composeRule.setContent {
            KaiFitTheme {
                HelpScreen()
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun waistAnswerIsShownOnTapAndHiddenOnSecondTap() {
        val question = composeRule.onNodeWithText(string(R.string.help_q_waist))
        question.assert(hasStateDescription(string(R.string.expandable_collapsed)))
        composeRule.onNodeWithText(string(R.string.help_a_waist)).assertDoesNotExist()

        question.performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.help_a_waist)).assertIsDisplayed()
        question.assert(hasStateDescription(string(R.string.expandable_expanded)))

        question.performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.help_a_waist)).assertDoesNotExist()
        question.assert(hasStateDescription(string(R.string.expandable_collapsed)))
    }

    @Test
    fun questionHeaderIsAButton() {
        composeRule.onNodeWithText(string(R.string.help_q_waist))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    private fun string(@StringRes id: Int): String = composeRule.activity.getString(id)

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)
}
