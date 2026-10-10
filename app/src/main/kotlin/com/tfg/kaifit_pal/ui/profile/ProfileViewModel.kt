package com.tfg.kaifit_pal.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tfg.kaifit_pal.KaiFitApp
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.data.profile.SavedProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/** What the profile tab shows. */
sealed interface ProfileUiState {

    /** The profile has not been read yet. */
    data object Loading : ProfileUiState

    /** Nothing has been saved, or it was deleted. */
    data object Empty : ProfileUiState

    /** The last saved measurement and result. */
    data class Content(val profile: SavedProfile) : ProfileUiState
}

/** Shows the saved profile and deletes it on request. */
class ProfileViewModel(private val profileRepository: ProfileRepository) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = profileRepository.profile
        .map { profile -> if (profile == null) ProfileUiState.Empty else ProfileUiState.Content(profile) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState.Loading)

    /** Deletes the saved profile; the state becomes [ProfileUiState.Empty] when it is gone. */
    fun clearProfile() {
        viewModelScope.launch {
            try {
                profileRepository.clear()
            } catch (e: IOException) {
                // The file could not be written: the profile is still there and stays on screen,
                // so the user can try again.
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel((this[APPLICATION_KEY] as KaiFitApp).container.profileRepository)
            }
        }
    }
}
