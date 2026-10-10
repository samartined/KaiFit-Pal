package com.tfg.kaifit_pal.ui.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tfg.kaifit_pal.KaiFitApp
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.data.settings.SettingsRepository
import com.tfg.kaifit_pal.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the settings screen shows.
 *
 * @property settings saved settings.
 * @property dynamicColorAvailable whether the device supports wallpaper colours (Android 12+).
 */
data class SettingsUiState(
    val settings: AppSettings = AppSettings.DEFAULT,
    val dynamicColorAvailable: Boolean = true,
)

/**
 * Reads and changes the appearance settings. Changes are written to [settingsRepository], and
 * the whole app picks them up from there.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    dynamicColorAvailable: Boolean,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.settings
        .map { settings -> SettingsUiState(settings, dynamicColorAvailable) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(AppSettings.DEFAULT, dynamicColorAvailable),
        )

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = (this[APPLICATION_KEY] as KaiFitApp).container.settingsRepository,
                    dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                )
            }
        }
    }
}
