package com.tfg.kaifit_pal.ui.calculator

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.MeasurementField
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.ui.components.NumberField
import com.tfg.kaifit_pal.ui.components.SexSelector
import com.tfg.kaifit_pal.ui.components.Stepper
import com.tfg.kaifit_pal.ui.components.formatDecimalInput
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import com.tfg.kaifit_pal.ui.theme.Spacing
import com.tfg.kaifit_pal.ui.theme.withTabularFigures
import java.util.Locale

/**
 * Calculator tab: the measurement form with the live body fat percentage.
 *
 * @param onCalculate called when Calcular is pressed, which is only possible with a valid form;
 *   the caller gets the destination from [CalculatorViewModel.onCalculate].
 */
@Composable
fun CalculatorScreen(
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalculatorContent(
        state = uiState,
        onSexChange = viewModel::onSexChange,
        onAgeChange = viewModel::onAgeChange,
        onHeightChange = viewModel::onHeightChange,
        onWeightChange = viewModel::onWeightChange,
        onNeckChange = viewModel::onNeckChange,
        onWaistChange = viewModel::onWaistChange,
        onHipChange = viewModel::onHipChange,
        onActivityLevelChange = viewModel::onActivityLevelChange,
        onCalculate = onCalculate,
        modifier = modifier,
    )
}

/** Stateless calculator screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorContent(
    state: CalculatorUiState,
    onSexChange: (Sex) -> Unit,
    onAgeChange: (Int) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onNeckChange: (String) -> Unit,
    onWaistChange: (String) -> Unit,
    onHipChange: (String) -> Unit,
    onActivityLevelChange: (ActivityLevel) -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val focusManager = LocalFocusManager.current
    val cm = stringResource(R.string.unit_cm)
    val lastFieldActions = KeyboardActions(onDone = { focusManager.clearFocus() })

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calculator_title), modifier = Modifier.semantics { heading() }) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            FormSection(R.string.calculator_sex) {
                SexSelector(selected = state.sex, onSelect = onSexChange)
            }
            FormSection(R.string.calculator_age) {
                Stepper(
                    value = state.ageYears,
                    onValueChange = onAgeChange,
                    range = BodyMeasurements.AGE_RANGE,
                    valueText = pluralStringResource(R.plurals.value_age_years, state.ageYears, state.ageYears),
                    decreaseDescription = stringResource(R.string.calculator_age_decrease),
                    increaseDescription = stringResource(R.string.calculator_age_increase),
                )
            }

            NumberField(
                value = state.height,
                onValueChange = onHeightChange,
                label = stringResource(R.string.calculator_height),
                unit = cm,
                hint = rangeHint(MeasurementField.HEIGHT, locale),
                errorMessage = fieldErrorMessage(MeasurementField.HEIGHT, state.fieldErrors, locale),
            )
            NumberField(
                value = state.weight,
                onValueChange = onWeightChange,
                label = stringResource(R.string.calculator_weight),
                unit = stringResource(R.string.unit_kg),
                hint = rangeHint(MeasurementField.WEIGHT, locale),
                errorMessage = fieldErrorMessage(MeasurementField.WEIGHT, state.fieldErrors, locale),
            )

            FormSection(R.string.calculator_circumferences, R.string.calculator_circumferences_hint) {
                NumberField(
                    value = state.neck,
                    onValueChange = onNeckChange,
                    label = stringResource(R.string.calculator_neck),
                    unit = cm,
                    hint = rangeHint(MeasurementField.NECK, locale),
                    errorMessage = fieldErrorMessage(MeasurementField.NECK, state.fieldErrors, locale),
                )
                NumberField(
                    value = state.waist,
                    onValueChange = onWaistChange,
                    label = stringResource(R.string.calculator_waist),
                    unit = cm,
                    hint = rangeHint(MeasurementField.WAIST, locale),
                    errorMessage = fieldErrorMessage(MeasurementField.WAIST, state.fieldErrors, locale),
                    imeAction = if (state.showsHip) ImeAction.Next else ImeAction.Done,
                    keyboardActions = if (state.showsHip) KeyboardActions.Default else lastFieldActions,
                )
                // The hip only exists for women: for men or without a sex it is not even composed.
                if (state.showsHip) {
                    NumberField(
                        value = state.hip,
                        onValueChange = onHipChange,
                        label = stringResource(R.string.calculator_hip),
                        unit = cm,
                        hint = rangeHint(MeasurementField.HIP, locale),
                        errorMessage = fieldErrorMessage(MeasurementField.HIP, state.fieldErrors, locale),
                        imeAction = ImeAction.Done,
                        keyboardActions = lastFieldActions,
                    )
                }
            }

            BodyFatCard(percent = state.bodyFatPercent, note = state.bodyFatNote)
            ActivityLevelMenu(selected = state.activityLevel, onSelect = onActivityLevelChange, locale = locale)

            Button(
                onClick = onCalculate,
                enabled = state.canCalculate,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.calculator_calculate))
            }
        }
    }
}

/** A titled group of form controls, with an optional explanation under the title. */
@Composable
private fun FormSection(
    @StringRes titleRes: Int,
    @StringRes hintRes: Int? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = stringResource(titleRes),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
        )
        if (hintRes != null) {
            Text(
                text = stringResource(hintRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

/** Live body fat percentage; changes are announced politely by TalkBack. */
@Composable
private fun BodyFatCard(percent: Double?, note: BodyFatNote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                .padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = stringResource(R.string.calculator_body_fat_title), style = MaterialTheme.typography.titleSmall)
            if (percent != null) {
                Text(
                    text = stringResource(R.string.value_body_fat_percent, percent),
                    style = MaterialTheme.typography.headlineMedium.withTabularFigures(),
                )
            } else {
                Text(
                    text = stringResource(R.string.body_fat_not_estimated),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(text = stringResource(note.textRes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Drop-down menu with the five activity levels and their factors. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityLevelMenu(selected: ActivityLevel, onSelect: (ActivityLevel) -> Unit, locale: Locale) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = activityLevelText(selected, locale),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(stringResource(R.string.calculator_activity_level)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = { Text(stringResource(selected.descriptionRes)) },
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ActivityLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text = activityLevelText(level, locale), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(level.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onSelect(level)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/** "Moderado · factor 1,55": the level name with its factor written for [locale]. */
@Composable
private fun activityLevelText(level: ActivityLevel, locale: Locale): String =
    stringResource(R.string.activity_level_with_factor, stringResource(level.labelRes), level.formattedFactor(locale))

/** "Entre 100 y 250 cm": the accepted values of [field], with its unit. */
@Composable
private fun rangeHint(field: MeasurementField, locale: Locale): String {
    val range = field.validRange()
    return stringResource(
        R.string.calculator_range_hint,
        formatDecimalInput(range.start, locale),
        formatDecimalInput(range.endInclusive, locale),
        stringResource(field.unitRes),
    )
}

/** Message under the field for its error in [errors], or `null` if it has none. */
@Composable
private fun fieldErrorMessage(field: MeasurementField, errors: Map<MeasurementField, FieldError>, locale: Locale): String? =
    when (errors[field]) {
        null -> null
        FieldError.INVALID_NUMBER -> stringResource(R.string.calculator_error_invalid_number)
        FieldError.WAIST_NOT_ABOVE_NECK -> stringResource(R.string.calculator_error_waist_not_above_neck)
        FieldError.OUT_OF_RANGE -> {
            val range = field.validRange()
            stringResource(
                R.string.calculator_error_out_of_range,
                formatDecimalInput(range.start, locale),
                formatDecimalInput(range.endInclusive, locale),
                stringResource(field.unitRes),
            )
        }
    }

/** Unit of a length or weight field. */
@get:StringRes
private val MeasurementField.unitRes: Int
    get() = if (this == MeasurementField.WEIGHT) R.string.unit_kg else R.string.unit_cm

/** Accepted values of a measurement field, from [BodyMeasurements]. */
private fun MeasurementField.validRange(): ClosedFloatingPointRange<Double> = when (this) {
    MeasurementField.AGE -> BodyMeasurements.AGE_RANGE.first.toDouble()..BodyMeasurements.AGE_RANGE.last.toDouble()
    MeasurementField.HEIGHT -> BodyMeasurements.HEIGHT_RANGE_CM
    MeasurementField.WEIGHT -> BodyMeasurements.WEIGHT_RANGE_KG
    MeasurementField.NECK -> BodyMeasurements.NECK_RANGE_CM
    MeasurementField.WAIST -> BodyMeasurements.WAIST_RANGE_CM
    MeasurementField.HIP -> BodyMeasurements.HIP_RANGE_CM
}

@get:StringRes
private val BodyFatNote.textRes: Int
    get() = when (this) {
        BodyFatNote.SELECT_SEX -> R.string.body_fat_note_select_sex
        BodyFatNote.FIX_ERRORS -> R.string.body_fat_note_fix_errors
        BodyFatNote.NEEDS_NECK_AND_WAIST -> R.string.body_fat_note_needs_neck_and_waist
        BodyFatNote.NEEDS_NECK_WAIST_AND_HIP -> R.string.body_fat_note_needs_neck_waist_and_hip
        BodyFatNote.NEEDS_HIP -> R.string.body_fat_note_needs_hip
        BodyFatNote.NEEDS_HEIGHT_AND_WEIGHT -> R.string.body_fat_note_needs_height_and_weight
        BodyFatNote.US_NAVY -> R.string.body_fat_note_us_navy
        BodyFatNote.NOT_ESTIMABLE -> R.string.body_fat_note_not_estimable
    }

/** Fixed example states for previews and screenshots. */
internal object CalculatorPreviewStates {

    /** The form as first opened. */
    val empty: CalculatorUiState by lazy { CalculatorUiState().revalidated() }

    /** Woman of 30 years, 165 cm, 60 kg, neck 33, waist 70 and hip 95 cm, sedentary: 24,6 % body fat. */
    val female: CalculatorUiState by lazy {
        CalculatorUiState(
            sex = Sex.FEMALE,
            height = "165",
            weight = "60",
            neck = "33",
            waist = "70",
            hip = "95",
        ).revalidated()
    }

    /** A man with a height of 20 cm, out of range. */
    val heightError: CalculatorUiState by lazy {
        CalculatorUiState(sex = Sex.MALE, height = "20", weight = "80").revalidated()
    }
}

@PreviewLightDark
@Composable
private fun CalculatorContentPreview() {
    KaiFitTheme {
        CalculatorContent(
            state = CalculatorPreviewStates.female,
            onSexChange = {},
            onAgeChange = {},
            onHeightChange = {},
            onWeightChange = {},
            onNeckChange = {},
            onWaistChange = {},
            onHipChange = {},
            onActivityLevelChange = {},
            onCalculate = {},
        )
    }
}
