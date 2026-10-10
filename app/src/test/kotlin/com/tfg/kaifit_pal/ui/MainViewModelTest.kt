package com.tfg.kaifit_pal.ui

import com.tfg.kaifit_pal.data.newPreferencesFile
import com.tfg.kaifit_pal.data.preferencesDataStoreOn
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.data.settings.SettingsRepository
import com.tfg.kaifit_pal.data.settings.ThemeMode
import com.tfg.kaifit_pal.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun loadsThenFollowsTheSavedSettings() = runTest {
        val repository = SettingsRepository(backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile()))
        val viewModel = MainViewModel(repository)
        assertEquals(MainUiState.Loading, viewModel.uiState.value)

        backgroundScope.launch { viewModel.uiState.collect {} }
        assertEquals(MainUiState.Ready(AppSettings.DEFAULT), viewModel.uiState.first { it != MainUiState.Loading })

        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(
            MainUiState.Ready(AppSettings(themeMode = ThemeMode.DARK)),
            viewModel.uiState.first { it != MainUiState.Ready(AppSettings.DEFAULT) },
        )
    }
}
