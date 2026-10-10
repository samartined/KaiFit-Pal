package com.tfg.kaifit_pal.ui.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tfg.kaifit_pal.KaiFitApp
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.ui.components.formatDecimalInput
import com.tfg.kaifit_pal.ui.navigation.ResultsRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The calculator form: validates every change, estimates the body fat percentage live and gives
 * the results destination once the form is complete.
 *
 * The form is kept in [savedStateHandle], so it survives process death.
 */
class CalculatorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreForm().revalidated())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    /** Chooses the sex. Leaving Mujer clears the hip, which only exists for women. */
    fun onSexChange(sex: Sex) = updateForm { form ->
        form.copy(sex = sex, hip = if (sex == Sex.FEMALE) form.hip else "")
    }

    /** Sets the age, kept within [BodyMeasurements.AGE_RANGE]. */
    fun onAgeChange(ageYears: Int) = updateForm { form ->
        form.copy(ageYears = ageYears.coerceIn(BodyMeasurements.AGE_RANGE))
    }

    fun onHeightChange(text: String) = updateForm { it.copy(height = text) }

    fun onWeightChange(text: String) = updateForm { it.copy(weight = text) }

    fun onNeckChange(text: String) = updateForm { it.copy(neck = text) }

    fun onWaistChange(text: String) = updateForm { it.copy(waist = text) }

    fun onHipChange(text: String) = updateForm { it.copy(hip = text) }

    fun onActivityLevelChange(level: ActivityLevel) = updateForm { it.copy(activityLevel = level) }

    /**
     * The results destination of the current form, or `null` while Calcular is disabled. It does
     * not navigate: the caller does.
     */
    fun onCalculate(): ResultsRoute? {
        val state = _uiState.value
        if (!state.canCalculate) return null
        val measurements = state.toMeasurements() ?: return null
        return ResultsRoute.from(measurements, state.activityLevel)
    }

    /**
     * Fills the whole form with the saved profile, if there is one, writing the numbers with the
     * decimal separator of the default locale. Without a profile the form is left as it is.
     */
    fun prefillFromSavedProfile() {
        viewModelScope.launch {
            val profile = profileRepository.profile.first() ?: return@launch
            val locale = Locale.getDefault()
            val measurements = profile.measurements
            fun Double?.toText(): String = this?.let { formatDecimalInput(it, locale) }.orEmpty()
            updateForm {
                CalculatorUiState(
                    sex = measurements.sex,
                    ageYears = measurements.ageYears.coerceIn(BodyMeasurements.AGE_RANGE),
                    height = measurements.heightCm.toText(),
                    weight = measurements.weightKg.toText(),
                    neck = measurements.neckCm.toText(),
                    waist = measurements.waistCm.toText(),
                    hip = if (measurements.sex == Sex.FEMALE) measurements.hipCm.toText() else "",
                    activityLevel = profile.activityLevel,
                )
            }
        }
    }

    private fun restoreForm() = CalculatorUiState(
        sex = savedStateHandle.get<String>(KEY_SEX)?.let { name -> Sex.entries.firstOrNull { it.name == name } },
        ageYears = (savedStateHandle.get<Int>(KEY_AGE_YEARS) ?: CalculatorUiState.DEFAULT_AGE_YEARS)
            .coerceIn(BodyMeasurements.AGE_RANGE),
        height = savedStateHandle.get<String>(KEY_HEIGHT).orEmpty(),
        weight = savedStateHandle.get<String>(KEY_WEIGHT).orEmpty(),
        neck = savedStateHandle.get<String>(KEY_NECK).orEmpty(),
        waist = savedStateHandle.get<String>(KEY_WAIST).orEmpty(),
        hip = savedStateHandle.get<String>(KEY_HIP).orEmpty(),
        activityLevel = savedStateHandle.get<String>(KEY_ACTIVITY_LEVEL)
            ?.let { name -> ActivityLevel.entries.firstOrNull { it.name == name } }
            ?: ActivityLevel.SEDENTARY,
    )

    private fun updateForm(transform: (CalculatorUiState) -> CalculatorUiState) {
        val state = transform(_uiState.value).revalidated()
        _uiState.value = state
        savedStateHandle[KEY_SEX] = state.sex?.name
        savedStateHandle[KEY_AGE_YEARS] = state.ageYears
        savedStateHandle[KEY_HEIGHT] = state.height
        savedStateHandle[KEY_WEIGHT] = state.weight
        savedStateHandle[KEY_NECK] = state.neck
        savedStateHandle[KEY_WAIST] = state.waist
        savedStateHandle[KEY_HIP] = state.hip
        savedStateHandle[KEY_ACTIVITY_LEVEL] = state.activityLevel.name
    }

    companion object {
        /**
         * Key, in the saved state of the calculator's back stack entry, of a one-shot request to
         * fill the form with the saved profile ("Recalcular" in the profile tab).
         */
        const val PREFILL_FROM_PROFILE_KEY = "prefill_from_profile"

        private const val KEY_SEX = "calculator_sex"
        private const val KEY_AGE_YEARS = "calculator_age_years"
        private const val KEY_HEIGHT = "calculator_height"
        private const val KEY_WEIGHT = "calculator_weight"
        private const val KEY_NECK = "calculator_neck"
        private const val KEY_WAIST = "calculator_waist"
        private const val KEY_HIP = "calculator_hip"
        private const val KEY_ACTIVITY_LEVEL = "calculator_activity_level"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CalculatorViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    profileRepository = (this[APPLICATION_KEY] as KaiFitApp).container.profileRepository,
                )
            }
        }
    }
}
