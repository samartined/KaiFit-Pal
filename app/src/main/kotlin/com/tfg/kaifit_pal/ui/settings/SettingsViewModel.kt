package com.tfg.kaifit_pal.ui.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tfg.kaifit_pal.KaiFitApp
import com.tfg.kaifit_pal.data.llm.DeviceCheck
import com.tfg.kaifit_pal.data.llm.ImportSource
import com.tfg.kaifit_pal.data.llm.LlmRepository
import com.tfg.kaifit_pal.data.llm.ModelManager
import com.tfg.kaifit_pal.data.llm.ModelState
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.data.settings.InferenceBackend
import com.tfg.kaifit_pal.data.settings.SettingsRepository
import com.tfg.kaifit_pal.data.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/** What the settings screen shows. */
sealed interface SettingsUiState {

    /** The saved settings have not been read yet: no value is shown, not even the defaults. */
    data object Loading : SettingsUiState

    /**
     * The settings and the on-device model.
     *
     * @property settings saved settings.
     * @property dynamicColorAvailable whether the device supports wallpaper colours (Android 12+).
     * @property model where the model of Kai-Q is.
     * @property device RAM, free space and ABI, once checked.
     */
    data class Ready(
        val settings: AppSettings,
        val dynamicColorAvailable: Boolean,
        val model: ModelState = ModelState.Checking,
        val device: DeviceCheck? = null,
    ) : SettingsUiState
}

/**
 * Reads and changes the settings and manages the model of Kai-Q. Changes are written to
 * [settingsRepository], and the whole app picks them up from there; a change that cannot be
 * written is dropped. Deleting the model runs in [releaseScope], so leaving the screen never
 * stops it halfway.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    dynamicColorAvailable: Boolean,
    private val modelManager: ModelManager,
    private val llmRepository: LlmRepository,
    private val releaseScope: CoroutineScope,
) : ViewModel() {

    private val device = MutableStateFlow<DeviceCheck?>(null)

    val uiState: StateFlow<SettingsUiState> =
        combine(settingsRepository.settings, modelManager.state, device) { settings, model, device ->
            SettingsUiState.Ready(settings, dynamicColorAvailable, model, device)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState.Loading,
        )

    /** The screen is visible: reconciles the model with the disk and measures the device. */
    fun onScreenStarted() {
        viewModelScope.launch {
            modelManager.refresh()
            device.value = modelManager.deviceCheck()
        }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        save { settingsRepository.setThemeMode(mode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        save { settingsRepository.setDynamicColor(enabled) }
    }

    fun onInferenceBackendChange(backend: InferenceBackend) {
        save { settingsRepository.setInferenceBackend(backend) }
    }

    fun onDownloadOnWifiOnlyChange(enabled: Boolean) {
        save { settingsRepository.setDownloadOnWifiOnly(enabled) }
    }

    fun onDownloadModel() {
        viewModelScope.launch {
            modelManager.startDownload(wifiOnly = settingsRepository.settings.first().downloadOnWifiOnly)
        }
    }

    fun onImportModel(source: ImportSource) {
        viewModelScope.launch { modelManager.importModel(source) }
    }

    fun onCancelModelTransfer() {
        viewModelScope.launch { modelManager.cancel() }
    }

    fun onDismissModelError() {
        modelManager.dismissError()
    }

    /** Closes the engine, which has the file open, and only then deletes the model. */
    fun onDeleteModel() {
        releaseScope.launch {
            llmRepository.close()
            modelManager.delete()
            device.value = modelManager.deviceCheck()
        }
    }

    private fun save(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
            } catch (e: IOException) {
                // The file could not be written: the setting stays as it was, and so does the
                // screen, which shows the saved value.
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as KaiFitApp).container
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    modelManager = container.modelManager,
                    llmRepository = container.llmRepository,
                    releaseScope = container.applicationScope,
                )
            }
        }
    }
}
