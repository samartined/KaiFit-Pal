package com.tfg.kaifit_pal.ui.calculator

import androidx.lifecycle.SavedStateHandle
import com.tfg.kaifit_pal.data.FailingDataStore
import com.tfg.kaifit_pal.data.newPreferencesFile
import com.tfg.kaifit_pal.data.preferencesDataStoreOn
import com.tfg.kaifit_pal.data.profile.ProfileRepository
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.MeasurementField
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.domain.TdeeCalculator
import com.tfg.kaifit_pal.testing.MainDispatcherRule
import com.tfg.kaifit_pal.testing.ReferenceData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class CalculatorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    /** A repository that is never read; only the prefill test needs a real one. */
    private val unusedProfiles = ProfileRepository(FailingDataStore(IOException("not used")))

    private fun newViewModel(handle: SavedStateHandle = SavedStateHandle()) = CalculatorViewModel(handle, unusedProfiles)

    private fun CalculatorViewModel.enterReferenceMan() {
        onSexChange(Sex.MALE)
        onHeightChange("180")
        onWeightChange("80")
        onNeckChange("38")
        onWaistChange("85")
    }

    @Test
    fun initialStateIsAnEmptyFormThatCannotBeCalculated() {
        val state = newViewModel().uiState.value

        assertNull(state.sex)
        assertEquals(30, state.ageYears)
        assertEquals(ActivityLevel.SEDENTARY, state.activityLevel)
        assertEquals(listOf("", "", "", "", ""), listOf(state.height, state.weight, state.neck, state.waist, state.hip))
        assertEquals(emptyMap<MeasurementField, FieldError>(), state.fieldErrors)
        assertFalse(state.canCalculate)
    }

    @Test
    fun manWithHeightAndWeightCanCalculateWithMifflinNote() {
        val viewModel = newViewModel()

        viewModel.onSexChange(Sex.MALE)
        viewModel.onHeightChange("180")
        viewModel.onWeightChange("80")

        val state = viewModel.uiState.value
        assertTrue(state.canCalculate)
        assertNull(state.bodyFatPercent)
        assertEquals(BodyFatNote.NEEDS_NECK_AND_WAIST, state.bodyFatNote)
    }

    @Test
    fun heightOutOfRangeShowsErrorAndDisablesCalculate() {
        val viewModel = newViewModel()
        viewModel.onSexChange(Sex.MALE)
        viewModel.onWeightChange("80")

        viewModel.onHeightChange("20")

        val state = viewModel.uiState.value
        assertEquals(mapOf(MeasurementField.HEIGHT to FieldError.OUT_OF_RANGE), state.fieldErrors)
        assertFalse(state.canCalculate)
    }

    @Test
    fun emptyRequiredFieldShowsNoErrorButDisablesCalculate() {
        val viewModel = newViewModel()
        viewModel.onSexChange(Sex.MALE)

        viewModel.onHeightChange("180")

        val state = viewModel.uiState.value
        assertEquals(emptyMap<MeasurementField, FieldError>(), state.fieldErrors)
        assertFalse(state.canCalculate)
    }

    @Test
    fun waistNotAboveNeckIsAnErrorOnTheWaist() {
        val viewModel = newViewModel()
        viewModel.enterReferenceMan()

        viewModel.onNeckChange("45")
        viewModel.onWaistChange("45")

        val state = viewModel.uiState.value
        assertEquals(mapOf(MeasurementField.WAIST to FieldError.WAIST_NOT_ABOVE_NECK), state.fieldErrors)
        assertFalse(state.canCalculate)
        assertEquals(BodyFatNote.FIX_ERRORS, state.bodyFatNote)
    }

    @Test
    fun bodyFatIsEstimatedLiveFromTheCircumferences() {
        val man = newViewModel()
        man.enterReferenceMan()
        assertEquals(16.1508, man.uiState.value.bodyFatPercent!!, 1e-3)
        assertEquals(BodyFatNote.US_NAVY, man.uiState.value.bodyFatNote)

        val woman = newViewModel()
        woman.onSexChange(Sex.FEMALE)
        woman.onHeightChange("165")
        woman.onWeightChange("60")
        woman.onNeckChange("33")
        woman.onWaistChange("70")
        assertNull(woman.uiState.value.bodyFatPercent)
        assertEquals(BodyFatNote.NEEDS_HIP, woman.uiState.value.bodyFatNote)

        woman.onHipChange("95")
        assertEquals(24.5635, woman.uiState.value.bodyFatPercent!!, 1e-3)
    }

    @Test
    fun switchingFromFemaleToMaleClearsTheHip() {
        val viewModel = newViewModel()
        viewModel.onSexChange(Sex.FEMALE)
        viewModel.onHipChange("95")
        assertEquals("95", viewModel.uiState.value.hip)

        viewModel.onSexChange(Sex.MALE)

        assertEquals("", viewModel.uiState.value.hip)
        assertFalse(viewModel.uiState.value.showsHip)
    }

    @Test
    fun ageStaysBetween18And90() {
        val viewModel = newViewModel()

        viewModel.onAgeChange(17)
        assertEquals(18, viewModel.uiState.value.ageYears)

        viewModel.onAgeChange(91)
        assertEquals(90, viewModel.uiState.value.ageYears)
    }

    @Test
    fun calculateGivesRouteOnlyForValidForm() {
        val viewModel = newViewModel()
        viewModel.onSexChange(Sex.FEMALE)
        viewModel.onHipChange("95")
        viewModel.onSexChange(Sex.MALE)
        viewModel.onHeightChange("180")
        assertNull(viewModel.onCalculate())

        viewModel.onWeightChange("80,5")
        viewModel.onActivityLevelChange(ActivityLevel.ACTIVE)
        val route = viewModel.onCalculate()

        assertNotNull(route)
        assertEquals(Sex.MALE, route!!.sex)
        assertEquals(180.0, route.heightCm, 0.0)
        assertEquals(80.5, route.weightKg, 0.0)
        assertNull(route.hipCm)
        assertEquals(ActivityLevel.ACTIVE, route.activityLevel)

        viewModel.onHeightChange("20")
        assertNull(viewModel.onCalculate())
    }

    @Test
    fun newViewModelWithSameSavedStateKeepsTheForm() {
        val handle = SavedStateHandle()
        val first = newViewModel(handle)
        first.enterReferenceMan()
        first.onAgeChange(45)
        first.onActivityLevelChange(ActivityLevel.MODERATE)

        val state = newViewModel(handle).uiState.value

        assertEquals(Sex.MALE, state.sex)
        assertEquals(45, state.ageYears)
        assertEquals(listOf("180", "80", "38", "85"), listOf(state.height, state.weight, state.neck, state.waist))
        assertEquals(ActivityLevel.MODERATE, state.activityLevel)
        assertTrue(state.canCalculate)
    }

    @Test
    fun prefillFromSavedProfileFillsTheForm() = runTest {
        val profiles = ProfileRepository(backgroundScope.preferencesDataStoreOn(tmp.newPreferencesFile()))
        val tdee = TdeeCalculator.calculate(ReferenceData.man, ReferenceData.manActivity)!!
        val modifier = GoalModifier(-15)
        profiles.save(
            ReferenceData.man,
            ReferenceData.manActivity,
            tdee,
            modifier,
            MacroPlan.forGoal(modifier.applyTo(tdee.tdeeKcal), modifier.goal),
        )
        val viewModel = CalculatorViewModel(SavedStateHandle(), profiles)

        viewModel.prefillFromSavedProfile()
        val state = viewModel.uiState.first { it.height.isNotEmpty() }

        assertEquals(Sex.MALE, state.sex)
        assertEquals(30, state.ageYears)
        assertEquals(listOf("180", "80", "38", "85", ""), listOf(state.height, state.weight, state.neck, state.waist, state.hip))
        assertEquals(ActivityLevel.MODERATE, state.activityLevel)
        assertTrue(state.canCalculate)
    }
}
