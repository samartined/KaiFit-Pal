package com.tfg.kaifit_pal.ui.profile

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.TdeeCalculator
import com.tfg.kaifit_pal.testing.ReferenceData
import com.tfg.kaifit_pal.testing.TemporaryDataStores
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es-rES")
class ProfileScreenTest {

    @get:Rule(order = 0)
    val dataStores = TemporaryDataStores()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyProfileOffersToOpenTheCalculator() {
        var calculatorOpened = 0
        composeRule.setContent {
            KaiFitTheme {
                ProfileContent(
                    state = ProfileUiState.Empty,
                    onGoToCalculator = { calculatorOpened++ },
                    onRecalculate = {},
                    onClearProfile = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.profile_go_to_calculator)).assertIsDisplayed().performClick()

        assertEquals(1, calculatorOpened)
    }

    @Test
    fun savedProfileShowsItsTdee() {
        composeRule.setContent {
            KaiFitTheme {
                ProfileContent(
                    state = ProfilePreviewStates.saved,
                    onGoToCalculator = {},
                    onRecalculate = {},
                    onClearProfile = {},
                )
            }
        }

        composeRule.onNodeWithText("2819 kcal/día").performScrollTo().assertIsDisplayed()
        // The percentage is joined to its sign by a no-break space.
        composeRule.onNodeWithText("Déficit moderado-intenso (−15\u00A0%)").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.profile_go_to_calculator)).assertCountEquals(0)
    }

    @Test
    fun clearingAfterConfirmationLeavesTheProfileEmpty() {
        val profiles = ProfileRepository(dataStores.newDataStore())
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
        val viewModel = ProfileViewModel(profiles)
        composeRule.setContent {
            KaiFitTheme {
                ProfileScreen(onGoToCalculator = {}, onRecalculate = {}, viewModel = viewModel)
            }
        }
        waitForText(R.string.profile_clear)

        composeRule.onNodeWithText(string(R.string.profile_clear)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.profile_clear_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.profile_clear_confirm)).performClick()

        waitForText(R.string.profile_go_to_calculator)
        composeRule.onNodeWithText(string(R.string.profile_go_to_calculator)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.profile_clear_title)).assertDoesNotExist()
        assertNull(runBlocking { profiles.profile.first() })
    }

    @Test
    fun cancellingTheConfirmationKeepsTheProfile() {
        var cleared = 0
        composeRule.setContent {
            KaiFitTheme {
                ProfileContent(
                    state = ProfilePreviewStates.saved,
                    onGoToCalculator = {},
                    onRecalculate = {},
                    onClearProfile = { cleared++ },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.profile_clear)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.profile_clear_cancel)).performClick()

        composeRule.onNodeWithText(string(R.string.profile_clear_title)).assertDoesNotExist()
        composeRule.onNodeWithText("2819 kcal/día").performScrollTo().assertIsDisplayed()
        assertEquals(0, cleared)
    }

    private fun string(@StringRes id: Int): String = composeRule.activity.getString(id)

    private fun waitForText(@StringRes id: Int) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(string(id)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
