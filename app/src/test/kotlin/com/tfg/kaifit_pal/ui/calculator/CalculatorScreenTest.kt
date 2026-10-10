package com.tfg.kaifit_pal.ui.calculator

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.data.FailingDataStore
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es-rES")
class CalculatorScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        // The form never reads the profile here: only "Recalcular" does.
        val viewModel = CalculatorViewModel(SavedStateHandle(), ProfileRepository(FailingDataStore(IOException("not used"))))
        composeRule.setContent {
            KaiFitTheme {
                CalculatorScreen(onCalculate = {}, viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun calculateIsEnabledOnlyWithAValidForm() {
        calculateButton().assertIsNotEnabled()

        field(R.string.calculator_height).performTextInput("20")
        composeRule.onNodeWithText(string(R.string.calculator_error_out_of_range, "100", "250", "cm")).assertIsDisplayed()
        calculateButton().assertIsNotEnabled()

        composeRule.onNodeWithText(string(R.string.sex_male)).performScrollTo().performClick()
        field(R.string.calculator_height).performTextReplacement("180")
        field(R.string.calculator_weight).performTextInput("80")

        composeRule.onAllNodesWithText(string(R.string.calculator_error_out_of_range, "100", "250", "cm"))
            .assertCountEquals(0)
        calculateButton().assertIsEnabled()
    }

    @Test
    fun hipFieldOnlyExistsForWomen() {
        hipNodes().assertCountEquals(0)

        composeRule.onNodeWithText(string(R.string.sex_male)).performScrollTo().performClick()
        hipNodes().assertCountEquals(0)

        composeRule.onNodeWithText(string(R.string.sex_female)).performScrollTo().performClick()
        hipNodes().assertCountEquals(1)
        field(R.string.calculator_hip).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.sex_male)).performScrollTo().performClick()
        hipNodes().assertCountEquals(0)
    }

    @Test
    fun weightWithDecimalCommaIsAccepted() {
        composeRule.onNodeWithText(string(R.string.sex_male)).performScrollTo().performClick()
        field(R.string.calculator_height).performTextInput("180")

        field(R.string.calculator_weight).performTextInput("80,5")

        field(R.string.calculator_weight).assertTextContains("80,5")
        composeRule.onAllNodesWithText(string(R.string.calculator_error_invalid_number)).assertCountEquals(0)
        calculateButton().assertIsEnabled()
    }

    @Test
    fun liveBodyFatOfReferenceManIsShownWithDecimalComma() {
        composeRule.onNodeWithText(string(R.string.sex_male)).performScrollTo().performClick()
        field(R.string.calculator_height).performTextInput("180")
        field(R.string.calculator_weight).performTextInput("80")
        field(R.string.calculator_neck).performTextInput("38")
        field(R.string.calculator_waist).performTextInput("85")

        // The percentage is joined to its sign by a no-break space.
        composeRule.onNodeWithText("16,2\u00A0%", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.body_fat_note_us_navy), substring = true).assertExists()
    }

    private fun string(@StringRes id: Int, vararg formatArgs: Any): String =
        composeRule.activity.getString(id, *formatArgs)

    /** The text field whose label is [labelRes]. */
    private fun field(@StringRes labelRes: Int): SemanticsNodeInteraction =
        composeRule.onNode(hasSetTextAction() and hasText(string(labelRes)))

    /** Every node labelled as the hip field. */
    private fun hipNodes() = composeRule.onAllNodesWithText(string(R.string.calculator_hip))

    private fun calculateButton(): SemanticsNodeInteraction =
        composeRule.onNodeWithText(string(R.string.calculator_calculate)).performScrollTo()
}
