package com.tfg.kaifit_pal.ui.profile

import com.tfg.kaifit_pal.data.newPreferencesFile
import com.tfg.kaifit_pal.data.preferencesDataStoreOn
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.domain.Goal
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.TdeeCalculator
import com.tfg.kaifit_pal.testing.MainDispatcherRule
import com.tfg.kaifit_pal.testing.ReferenceData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.newRepository() =
        ProfileRepository(backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile()))

    /** Keeps the state flow collected, as the screen does. */
    private fun TestScope.subscribe(viewModel: ProfileViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    private suspend fun ProfileRepository.saveReferenceMan() {
        val tdee = TdeeCalculator.calculate(ReferenceData.man, ReferenceData.manActivity)!!
        save(ReferenceData.man, ReferenceData.manActivity, tdee, GoalModifier.MAINTENANCE, MacroPlan.forGoal(tdee.tdeeKcal, Goal.MAINTENANCE))
    }

    @Test
    fun noSavedProfileIsEmpty() = runTest {
        val viewModel = ProfileViewModel(newRepository())
        subscribe(viewModel)

        assertEquals(ProfileUiState.Empty, viewModel.uiState.first { it != ProfileUiState.Loading })
    }

    @Test
    fun savedProfileIsShown() = runTest {
        val repository = newRepository()
        repository.saveReferenceMan()
        val viewModel = ProfileViewModel(repository)
        subscribe(viewModel)

        val state = viewModel.uiState.first { it != ProfileUiState.Loading }

        assertEquals(ProfileUiState.Content(repository.profile.first()!!), state)
        assertEquals(2819, (state as ProfileUiState.Content).profile.tdee.tdeeKcal)
    }

    @Test
    fun clearingTheProfileLeavesItEmpty() = runTest {
        val repository = newRepository()
        repository.saveReferenceMan()
        val viewModel = ProfileViewModel(repository)
        subscribe(viewModel)
        viewModel.uiState.first { it is ProfileUiState.Content }

        viewModel.clearProfile()

        assertEquals(ProfileUiState.Empty, viewModel.uiState.first { it !is ProfileUiState.Content })
        assertEquals(null, repository.profile.first())
    }
}
