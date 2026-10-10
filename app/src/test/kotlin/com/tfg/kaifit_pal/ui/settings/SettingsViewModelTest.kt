package com.tfg.kaifit_pal.ui.settings

import com.tfg.kaifit_pal.data.FailingWritesDataStore
import com.tfg.kaifit_pal.data.newPreferencesFile
import com.tfg.kaifit_pal.data.preferencesDataStoreOn
import com.tfg.kaifit_pal.data.llm.ModelState
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.data.settings.InferenceBackend
import com.tfg.kaifit_pal.data.settings.SettingsRepository
import com.tfg.kaifit_pal.data.settings.ThemeMode
import com.tfg.kaifit_pal.testing.FakeImportSource
import com.tfg.kaifit_pal.testing.FakeLlmRepository
import com.tfg.kaifit_pal.testing.FakeModelManager
import com.tfg.kaifit_pal.testing.MainDispatcherRule
import com.tfg.kaifit_pal.ui.theme.isDark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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

    private val calls = mutableListOf<String>()
    private val models = FakeModelManager(calls = calls)
    private val llm = FakeLlmRepository(calls)

    private fun TestScope.newRepository() =
        SettingsRepository(backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile()))

    /** Runs what the ViewModel launches in the release scope at once. */
    private fun TestScope.releaseScope() =
        CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))

    private fun TestScope.newViewModel(repository: SettingsRepository, dynamicColorAvailable: Boolean = true) =
        SettingsViewModel(repository, dynamicColorAvailable, models, llm, releaseScope())

    /** Keeps the state flow collected, as the screen does. */
    private fun TestScope.subscribe(viewModel: SettingsViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    private suspend fun SettingsViewModel.awaitReady(predicate: (SettingsUiState.Ready) -> Boolean = { true }): SettingsUiState.Ready =
        uiState.first { it is SettingsUiState.Ready && predicate(it) } as SettingsUiState.Ready

    @Test
    fun stateReflectsTheSavedSettings() = runTest {
        val repository = newRepository()
        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setDynamicColor(true)
        val viewModel = newViewModel(repository, dynamicColorAvailable = false)
        subscribe(viewModel)

        val state = viewModel.awaitReady { it.settings.themeMode == ThemeMode.LIGHT }

        assertEquals(
            SettingsUiState.Ready(
                AppSettings(themeMode = ThemeMode.LIGHT, dynamicColor = true),
                dynamicColorAvailable = false,
                model = ModelState.NotDownloaded,
                device = null,
            ),
            state,
        )
    }

    @Test
    fun themeModeChangeIsSaved() = runTest {
        val repository = newRepository()
        val viewModel = newViewModel(repository)

        viewModel.onThemeModeChange(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, repository.settings.first().themeMode)
    }

    @Test
    fun dynamicColorChangeIsSaved() = runTest {
        val repository = newRepository()
        val viewModel = newViewModel(repository)

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
        val viewModel = newViewModel(repository)
        subscribe(viewModel)
        viewModel.awaitReady { it.settings.themeMode == ThemeMode.LIGHT }

        viewModel.onThemeModeChange(ThemeMode.DARK)
        viewModel.onDynamicColorChange(false)
        advanceUntilIdle()

        assertEquals(AppSettings(themeMode = ThemeMode.LIGHT, dynamicColor = true), viewModel.awaitReady().settings)
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

    // S6
    @Test
    fun stateIsLoadingUntilTheSavedSettingsAreRead() = runTest {
        val repository = newRepository()
        repository.setInferenceBackend(InferenceBackend.CPU)
        val viewModel = newViewModel(repository)

        assertEquals(SettingsUiState.Loading, viewModel.uiState.value)
        subscribe(viewModel)
        val state = viewModel.awaitReady()

        assertEquals(InferenceBackend.CPU, state.settings.inferenceBackend)
    }

    // S7
    @Test
    fun inferenceBackendChangeIsSaved() = runTest {
        val repository = newRepository()
        val viewModel = newViewModel(repository)

        viewModel.onInferenceBackendChange(InferenceBackend.CPU)
        advanceUntilIdle()

        assertEquals(InferenceBackend.CPU, repository.settings.first().inferenceBackend)
    }

    // S8
    @Test
    fun wifiOnlyChangeIsSaved() = runTest {
        val repository = newRepository()
        val viewModel = newViewModel(repository)

        viewModel.onDownloadOnWifiOnlyChange(false)
        advanceUntilIdle()

        assertFalse(repository.settings.first().downloadOnWifiOnly)
    }

    // S9
    @Test
    fun deletingTheModelClosesTheEngineFirst() = runTest {
        models.state.value = FakeModelManager.downloaded
        val viewModel = newViewModel(newRepository())

        viewModel.onDeleteModel()

        assertEquals(listOf("llm.close", "model.delete"), calls)
        assertEquals(ModelState.NotDownloaded, models.state.value)
    }

    // S10
    @Test
    fun stateShowsTheModelAndTheDeviceOfTheManager() = runTest {
        models.state.value = FakeModelManager.downloaded
        val viewModel = newViewModel(newRepository())
        subscribe(viewModel)

        viewModel.onScreenStarted()
        val state = viewModel.awaitReady { it.device != null }

        assertEquals(FakeModelManager.downloaded, state.model)
        assertEquals(models.device, state.device)
        assertEquals(listOf("model.refresh"), calls)

        models.state.value = ModelState.NotDownloaded
        assertEquals(ModelState.NotDownloaded, viewModel.awaitReady { it.model == ModelState.NotDownloaded }.model)
    }

    // S10
    @Test
    fun modelActionsAreDelegatedWithTheWifiSetting() = runTest {
        val repository = newRepository()
        repository.setDownloadOnWifiOnly(false)
        val viewModel = newViewModel(repository)
        val source = FakeImportSource(FakeImportSource.modelBytes(10))

        viewModel.onDownloadModel()
        assertEquals(listOf(false), models.downloadRequests.first { it.isNotEmpty() })
        viewModel.onImportModel(source)
        viewModel.onCancelModelTransfer()
        viewModel.onDismissModelError()
        advanceUntilIdle()

        assertSame(source, models.imports.single())
        assertEquals(listOf("model.startDownload", "model.importModel", "model.cancel", "model.dismissError"), calls)
    }

    // S11
    @Test
    fun failedWritesOfTheModelSettingsDoNotCrash() = runTest {
        val dataStore = backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile())
        val repository = SettingsRepository(FailingWritesDataStore(dataStore, IOException("disk full")))
        val viewModel = newViewModel(repository)
        subscribe(viewModel)
        viewModel.awaitReady()

        viewModel.onInferenceBackendChange(InferenceBackend.CPU)
        viewModel.onDownloadOnWifiOnlyChange(false)
        advanceUntilIdle()

        assertEquals(AppSettings.DEFAULT, viewModel.awaitReady().settings)
    }
}
