package com.tfg.kaifit_pal.ui.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.tfg.kaifit_pal.KaiFitApp
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.TdeeCalculator
import com.tfg.kaifit_pal.domain.TdeeResult
import com.tfg.kaifit_pal.ui.navigation.ResultsRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.roundToInt

/** Progress of "Guardar en perfil". */
sealed interface SaveStatus {
    /** Nothing to report. */
    data object Idle : SaveStatus

    /** The profile is being written. */
    data object Saving : SaveStatus

    /** The profile was saved; the screen tells the user once. */
    data object Saved : SaveStatus

    /** The profile could not be saved; the screen tells the user once. */
    data object Error : SaveStatus
}

/** What the results screen shows. */
sealed interface ResultsUiState {

    /**
     * The measurements of the route cannot give a result. The calculator only opens Results with
     * valid measurements, so this should never be seen.
     */
    data object InvalidMeasurements : ResultsUiState

    /**
     * @property tdee total daily energy expenditure, with its method and body fat percentage.
     * @property goalModifier calorie adjustment chosen with the goal slider.
     * @property targetKcal calorie target: [goalModifier] applied to the TDEE, in kcal/day.
     * @property macroPlan macros for [targetKcal], default or adjusted by the user.
     * @property saveStatus progress of saving the profile.
     */
    data class Ready(
        val tdee: TdeeResult,
        val goalModifier: GoalModifier,
        val targetKcal: Int,
        val macroPlan: MacroPlan,
        val saveStatus: SaveStatus,
    ) : ResultsUiState
}

/**
 * Results of the measurements in [route]: the TDEE, the goal slider and the macro split, and
 * saving all of it to the profile.
 *
 * The TDEE is computed again from the route. The goal and any manual change to the protein or
 * fat grams are kept in [savedStateHandle], so they survive process death.
 */
class ResultsViewModel(
    route: ResultsRoute,
    private val savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val measurements = route.toMeasurements()
    private val activityLevel = route.activityLevel
    private val tdee: TdeeResult? = TdeeCalculator.calculate(measurements, activityLevel)

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    /**
     * Sets the goal from the slider position [value], rounded to the nearest multiple of 5 and
     * limited to −20..20 %. A different goal starts again from its default macro split.
     */
    fun onGoalSliderChange(value: Float) {
        val current = _uiState.value as? ResultsUiState.Ready ?: return
        val step = GoalModifier.STEP
        val percent = ((value / step).roundToInt() * step).coerceIn(GoalModifier.MIN_PERCENT, GoalModifier.MAX_PERCENT)
        if (percent == current.goalModifier.percent) return
        val modifier = GoalModifier(percent)
        savedStateHandle[KEY_GOAL_PERCENT] = percent
        savedStateHandle.remove<Int>(KEY_PROTEIN_GRAMS)
        savedStateHandle.remove<Int>(KEY_FAT_GRAMS)
        _uiState.value = readyState(current.tdee, modifier, defaultPlan(current.tdee, modifier), current.saveStatus)
    }

    /** Sets the protein grams, within what fits next to the current fat; carbohydrates follow. */
    fun onProteinChange(grams: Int) {
        updatePlan { plan -> plan.withProteinGrams(grams) }
    }

    /** Sets the fat grams, within what fits next to the current protein; carbohydrates follow. */
    fun onFatChange(grams: Int) {
        updatePlan { plan -> plan.withFatGrams(grams) }
    }

    /** Saves the measurements, the TDEE, the goal and the macros as the profile. */
    fun onSave() {
        val current = _uiState.value as? ResultsUiState.Ready ?: return
        if (current.saveStatus == SaveStatus.Saving) return
        setSaveStatus(SaveStatus.Saving)
        viewModelScope.launch {
            val status = try {
                profileRepository.save(measurements, activityLevel, current.tdee, current.goalModifier, current.macroPlan)
                SaveStatus.Saved
            } catch (e: IllegalArgumentException) {
                // Incoherent data: the repository refuses it before writing anything.
                SaveStatus.Error
            } catch (e: IOException) {
                // The file could not be written.
                SaveStatus.Error
            }
            setSaveStatus(status)
        }
    }

    /** The screen has told the user how saving went. */
    fun onSaveStatusShown() {
        val current = _uiState.value as? ResultsUiState.Ready ?: return
        if (current.saveStatus == SaveStatus.Saved || current.saveStatus == SaveStatus.Error) {
            setSaveStatus(SaveStatus.Idle)
        }
    }

    private fun initialState(): ResultsUiState {
        val tdee = tdee ?: return ResultsUiState.InvalidMeasurements
        val savedPercent = savedStateHandle.get<Int>(KEY_GOAL_PERCENT)
        val modifier = GoalModifier.all.firstOrNull { it.percent == savedPercent } ?: GoalModifier.MAINTENANCE
        var plan = defaultPlan(tdee, modifier)
        val protein = savedStateHandle.get<Int>(KEY_PROTEIN_GRAMS)
        val fat = savedStateHandle.get<Int>(KEY_FAT_GRAMS)
        if (protein != null && fat != null) {
            // Fat goes to 0 first, so the saved protein is not limited by the default fat.
            plan = plan.withFatGrams(0).withProteinGrams(protein).withFatGrams(fat)
        }
        return readyState(tdee, modifier, plan, SaveStatus.Idle)
    }

    private fun updatePlan(transform: (MacroPlan) -> MacroPlan) {
        val current = _uiState.value as? ResultsUiState.Ready ?: return
        val plan = transform(current.macroPlan)
        savedStateHandle[KEY_PROTEIN_GRAMS] = plan.proteinGrams
        savedStateHandle[KEY_FAT_GRAMS] = plan.fatGrams
        _uiState.value = readyState(current.tdee, current.goalModifier, plan, current.saveStatus)
    }

    private fun setSaveStatus(status: SaveStatus) {
        _uiState.update { state ->
            if (state is ResultsUiState.Ready) {
                readyState(state.tdee, state.goalModifier, state.macroPlan, status)
            } else {
                state
            }
        }
    }

    companion object {
        // Keys of the screen's own state; the route arguments live under other names.
        private const val KEY_GOAL_PERCENT = "results_goal_percent"
        private const val KEY_PROTEIN_GRAMS = "results_protein_grams"
        private const val KEY_FAT_GRAMS = "results_fat_grams"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                ResultsViewModel(
                    route = savedStateHandle.toRoute<ResultsRoute>(),
                    savedStateHandle = savedStateHandle,
                    profileRepository = (this[APPLICATION_KEY] as KaiFitApp).container.profileRepository,
                )
            }
        }
    }
}

/** Default macro split of the goal of [modifier], for its target. */
private fun defaultPlan(tdee: TdeeResult, modifier: GoalModifier): MacroPlan =
    MacroPlan.forGoal(modifier.applyTo(tdee.tdeeKcal), modifier.goal)

private fun readyState(tdee: TdeeResult, modifier: GoalModifier, plan: MacroPlan, saveStatus: SaveStatus) =
    ResultsUiState.Ready(
        tdee = tdee,
        goalModifier = modifier,
        targetKcal = plan.targetKcal,
        macroPlan = plan,
        saveStatus = saveStatus,
    )
