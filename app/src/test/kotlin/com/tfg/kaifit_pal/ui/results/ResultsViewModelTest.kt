package com.tfg.kaifit_pal.ui.results

import androidx.lifecycle.SavedStateHandle
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.data.FailingDataStore
import com.tfg.kaifit_pal.data.newPreferencesFile
import com.tfg.kaifit_pal.data.preferencesDataStoreOn
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.TdeeMethod
import com.tfg.kaifit_pal.testing.MainDispatcherRule
import com.tfg.kaifit_pal.testing.ReferenceData
import com.tfg.kaifit_pal.ui.navigation.ResultsRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import kotlin.math.abs

class ResultsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    /** A repository for tests that never save. */
    private val unusedProfiles = ProfileRepository(FailingDataStore(IOException("not used")))

    private fun newViewModel(
        route: ResultsRoute = ReferenceData.manRoute,
        handle: SavedStateHandle = SavedStateHandle(),
        profiles: ProfileRepository = unusedProfiles,
    ) = ResultsViewModel(route, handle, profiles)

    private val ResultsViewModel.ready: ResultsUiState.Ready
        get() = uiState.value as ResultsUiState.Ready

    private fun ResultsUiState.Ready.grams() = listOf(macroPlan.proteinGrams, macroPlan.fatGrams, macroPlan.carbGrams)

    @Test
    fun tdeeIsComputedAgainFromTheRoute() {
        val state = newViewModel().ready

        assertEquals(2819, state.tdee.tdeeKcal)
        assertEquals(TdeeMethod.KATCH_MCARDLE, state.tdee.method)
    }

    @Test
    fun defaultGoalIsMaintenanceWithItsDefaultSplit() {
        val state = newViewModel().ready

        assertEquals(GoalModifier.MAINTENANCE, state.goalModifier)
        assertEquals(2819, state.targetKcal)
        assertEquals(listOf(211, 78, 318), state.grams())
    }

    @Test
    fun goalSliderAtMinus15GivesModerateHighDeficit() {
        val viewModel = newViewModel()

        viewModel.onGoalSliderChange(-15f)

        val state = viewModel.ready
        assertEquals(GoalModifier(-15), state.goalModifier)
        assertEquals(R.string.goal_deficit_moderate_high, goalLabelRes(state.goalModifier))
        assertEquals(2396, state.targetKcal)
        assertEquals(listOf(210, 67, 238), state.grams())
    }

    @Test
    fun goalSliderRoundsToNearestStepAndStaysWithinLimits() {
        val viewModel = newViewModel()

        viewModel.onGoalSliderChange(-13f)
        assertEquals(-15, viewModel.ready.goalModifier.percent)

        viewModel.onGoalSliderChange(22f)
        assertEquals(20, viewModel.ready.goalModifier.percent)

        viewModel.onGoalSliderChange(-40f)
        assertEquals(-20, viewModel.ready.goalModifier.percent)
        assertEquals(2255, viewModel.ready.targetKcal)
    }

    @Test
    fun proteinChangeKeepsFatAndRecomputesCarbs() {
        val viewModel = newViewModel()
        viewModel.onGoalSliderChange(-15f)

        viewModel.onProteinChange(250)

        val state = viewModel.ready
        assertEquals(listOf(250, 67, 198), state.grams())
        assertTrue(abs(state.macroPlan.totalKcal - 2396) <= 2)
    }

    @Test
    fun changingTheGoalResetsManualGrams() {
        val viewModel = newViewModel()
        viewModel.onProteinChange(250)
        viewModel.onFatChange(40)

        viewModel.onGoalSliderChange(-15f)

        assertEquals(listOf(210, 67, 238), viewModel.ready.grams())
    }

    @Test
    fun macrosAddUpToTargetForEveryGoalAfterExtremeChanges() {
        val viewModel = newViewModel()
        GoalModifier.all.forEach { modifier ->
            viewModel.onGoalSliderChange(modifier.percent.toFloat())
            viewModel.onProteinChange(viewModel.ready.targetKcal / 4)
            viewModel.onFatChange(0)

            val state = viewModel.ready
            assertEquals(modifier, state.goalModifier)
            assertTrue("$modifier: ${state.macroPlan}", abs(state.macroPlan.totalKcal - state.targetKcal) <= 2)
        }
    }

    @Test
    fun saveStoresTheWholeResultInTheProfile() = runTest {
        val profiles = ProfileRepository(backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile()))
        val viewModel = newViewModel(profiles = profiles)
        viewModel.onGoalSliderChange(-15f)
        viewModel.onProteinChange(250)

        viewModel.onSave()
        val state = viewModel.uiState.first { (it as ResultsUiState.Ready).saveStatus != SaveStatus.Saving }

        assertEquals(SaveStatus.Saved, (state as ResultsUiState.Ready).saveStatus)
        val saved = profiles.profile.first()!!
        assertEquals(ReferenceData.man, saved.measurements)
        assertEquals(ActivityLevel.MODERATE, saved.activityLevel)
        assertEquals(state.tdee, saved.tdee)
        assertEquals(GoalModifier(-15), saved.goalModifier)
        assertEquals(state.macroPlan, saved.macroPlan)
        assertEquals(2396, saved.macroPlan.targetKcal)
        assertEquals(250, saved.macroPlan.proteinGrams)
    }

    @Test
    fun saveFailureBecomesAnErrorStatus() = runTest {
        val viewModel = newViewModel(profiles = ProfileRepository(FailingDataStore(IOException("disk full"))))

        viewModel.onSave()
        val state = viewModel.uiState.first { (it as ResultsUiState.Ready).saveStatus != SaveStatus.Saving }

        assertEquals(SaveStatus.Error, (state as ResultsUiState.Ready).saveStatus)
    }

    @Test
    fun newViewModelWithSameSavedStateKeepsGoalAndManualProtein() {
        val handle = SavedStateHandle()
        val first = newViewModel(handle = handle)
        first.onGoalSliderChange(-15f)
        first.onFatChange(0)
        first.onProteinChange(500)

        val state = newViewModel(handle = handle).ready

        assertEquals(GoalModifier(-15), state.goalModifier)
        assertEquals(listOf(500, 0, 99), state.grams())
    }

    @Test
    fun invalidMeasurementsGiveAnErrorStateInsteadOfACrash() {
        val route = ReferenceData.manRoute.copy(heightCm = 20.0)

        assertEquals(ResultsUiState.InvalidMeasurements, newViewModel(route = route).uiState.value)
    }
}
