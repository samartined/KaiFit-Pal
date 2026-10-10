package com.tfg.kaifit_pal.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.navigation.KaiFitNavHost
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Smoke screenshots of the app shell on a 411 x 891 dp phone. The PNG files are written only by
 * `./gradlew recordRoborazziDebug`, under app/build/outputs/roborazzi/.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun calculatorLight() {
        setShell(darkTheme = false)
        capture("smoke_calculator_light.png")
    }

    @Test
    fun calculatorDark() {
        setShell(darkTheme = true)
        capture("smoke_calculator_dark.png")
    }

    @Test
    fun resultsLight() {
        setShell(darkTheme = false)
        val activity = composeRule.activity
        composeRule.onNodeWithText(activity.getString(R.string.sex_male)).performClick()
        composeRule.onNode(hasSetTextAction() and hasText(activity.getString(R.string.calculator_height)))
            .performTextInput("180")
        composeRule.onNode(hasSetTextAction() and hasText(activity.getString(R.string.calculator_weight)))
            .performTextInput("80")
        composeRule.onNodeWithText(activity.getString(R.string.calculator_calculate)).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(activity.getString(R.string.results_title)).assertIsDisplayed()
        capture("smoke_results_light.png")
    }

    private fun setShell(darkTheme: Boolean) {
        composeRule.setContent {
            KaiFitTheme(darkTheme = darkTheme) {
                KaiFitNavHost()
            }
        }
        composeRule.waitForIdle()
    }

    private fun capture(fileName: String) {
        composeRule.onRoot().captureRoboImage("$OUTPUT_DIR/$fileName")
    }

    private companion object {
        const val OUTPUT_DIR = "build/outputs/roborazzi"
    }
}
