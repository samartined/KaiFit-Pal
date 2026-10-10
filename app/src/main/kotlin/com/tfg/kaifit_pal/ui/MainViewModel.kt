package com.tfg.kaifit_pal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tfg.kaifit_pal.KaiFitApp
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What the activity needs before drawing: the saved appearance settings. */
sealed interface MainUiState {

    /** The settings have not been read yet; the splash screen stays on. */
    data object Loading : MainUiState

    /** The settings to apply, read from disk or the defaults if they could not be read. */
    data class Ready(val settings: AppSettings) : MainUiState
}

/**
 * State of [com.tfg.kaifit_pal.MainActivity]: the theme mode and dynamic colour of the whole app.
 *
 * It starts as [MainUiState.Loading] so that the splash screen can wait for the saved theme
 * instead of drawing the app with the wrong one for a frame.
 */
class MainViewModel(settingsRepository: SettingsRepository) : ViewModel() {

    val uiState: StateFlow<MainUiState> = settingsRepository.settings
        .map<AppSettings, MainUiState> { MainUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState.Loading)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MainViewModel((this[APPLICATION_KEY] as KaiFitApp).container.settingsRepository)
            }
        }
    }
}
