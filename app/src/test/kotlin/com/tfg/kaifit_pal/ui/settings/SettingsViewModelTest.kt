package com.tfg.kaifit_pal.ui.settings

import com.tfg.kaifit_pal.data.FailingWritesDataStore
import com.tfg.kaifit_pal.data.newPreferencesFile
import com.tfg.kaifit_pal.data.preferencesDataStoreOn
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.data.settings.SettingsRepository
import com.tfg.kaifit_pal.data.settings.ThemeMode
import com.tfg.kaifit_pal.testing.MainDispatcherRule
import com.tfg.kaifit_pal.ui.theme.isDark
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.newRepository() =
        SettingsRepository(backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile()))

    @Test
    fun stateReflectsTheSavedSettings() = runTest {
        val repository = newRepository()
        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setDynamicColor(true)
        val viewModel = SettingsViewModel(repository, dynamicColorAvailable = false)
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.first { it.settings.themeMode == ThemeMode.LIGHT }

        assertEquals(SettingsUiState(AppSettings(themeMode = ThemeMode.LIGHT, dynamicColor = true), false), state)
    }

    @Test
    fun themeModeChangeIsSaved() = runTest {
        val repository = newRepository()
        val viewModel = SettingsViewModel(repository, dynamicColorAvailable = true)

        viewModel.onThemeModeChange(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, repository.settings.first().themeMode)
    }

    @Test
    fun dynamicColorChangeIsSaved() = runTest {
        val repository = newRepository()
        val viewModel = SettingsViewModel(repository, dynamicColorAvailable = true)

        viewModel.onDynamicColorChange(true)
        advanceUntilIdle()

        assertTrue(repository.settings.first().dynamicColor)
    }

    @Test
    fun failedWritesKeepTheSavedSettingsWithoutCrashing() = runTest {
        val dataStore = backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile())
        SettingsRepository(dataStore).apply {
            setThemeMode(ThemeMode.LIGHT)
            setDynamicColor(true)
        }
        val repository = SettingsRepository(FailingWritesDataStore(dataStore, IOException("disk full")))
        val viewModel = SettingsViewModel(repository, dynamicColorAvailable = true)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.settings.themeMode == ThemeMode.LIGHT }

        viewModel.onThemeModeChange(ThemeMode.DARK)
        viewModel.onDynamicColorChange(false)
        advanceUntilIdle()

        assertEquals(SettingsUiState(AppSettings(themeMode = ThemeMode.LIGHT, dynamicColor = true), true), viewModel.uiState.value)
    }

    @Test
    fun systemModeFollowsTheSystemAndTheOthersIgnoreIt() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = false))
        assertTrue(ThemeMode.DARK.isDark(systemDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
    }
}
