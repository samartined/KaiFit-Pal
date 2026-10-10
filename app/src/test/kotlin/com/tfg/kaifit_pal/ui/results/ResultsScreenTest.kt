package com.tfg.kaifit_pal.ui.results

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.testing.ReferenceData
import com.tfg.kaifit_pal.testing.TemporaryDataStores
import com.tfg.kaifit_pal.ui.navigation.ResultsRoute
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es-rES")
class ResultsScreenTest {

    @get:Rule(order = 0)
    val dataStores = TemporaryDataStores()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var profiles: ProfileRepository

    @Before
    fun setUp() {
        profiles = ProfileRepository(dataStores.newDataStore())
    }

    @Test
    fun goalSliderAtMinus15ShowsModerateHighDeficitAndItsTarget() {
        showResults(ReferenceData.manRoute)

        goalSlider().performSemanticsAction(SemanticsActions.SetProgress) { it(-15f) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Déficit moderado-intenso").assertIsDisplayed()
        composeRule.onNodeWithText("2396 kcal/día").assertIsDisplayed()
    }

    @Test
    fun savingShowsASnackbarAndStoresTheProfile() {
        showResults(ReferenceData.manRoute)

        goalSlider().performSemanticsAction(SemanticsActions.SetProgress) { it(-15f) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.results_save)).performScrollTo().performClick()

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(string(R.string.results_saved)).fetchSemanticsNodes().isNotEmpty()
        }
        val saved = runBlocking { profiles.profile.first() }
        assertNotNull(saved)
        assertEquals(GoalModifier(-15), saved!!.goalModifier)
        assertEquals(2396, saved.macroPlan.targetKcal)
        assertEquals(2819, saved.tdee.tdeeKcal)
    }

    @Test
    fun withoutBodyFatTheHeroCardSaysItIsNotEstimated() {
        val manWithoutCircumferences = BodyMeasurements(Sex.MALE, ageYears = 30, heightCm = 180.0, weightKg = 80.0)
        showResults(ResultsRoute.from(manWithoutCircumferences, ActivityLevel.MODERATE))

        composeRule.onNodeWithText("2759").assertIsDisplayed()
        composeRule.onNodeWithText("Método: Mifflin-St Jeor").assertIsDisplayed()
        composeRule.onNodeWithText("Grasa corporal: sin estimar").assertIsDisplayed()
    }

    private fun showResults(route: ResultsRoute) {
        val viewModel = ResultsViewModel(route, SavedStateHandle(), profiles)
        composeRule.setContent {
            KaiFitTheme {
                ResultsScreen(onBack = {}, onAskAssistant = {}, viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
    }

    private fun string(@StringRes id: Int): String = composeRule.activity.getString(id)

    private fun goalSlider(): SemanticsNodeInteraction =
        composeRule.onNodeWithContentDescription(string(R.string.results_goal_slider))

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
