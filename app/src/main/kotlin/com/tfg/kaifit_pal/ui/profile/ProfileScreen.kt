package com.tfg.kaifit_pal.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.data.profile.SavedProfile
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.domain.TdeeCalculator
import com.tfg.kaifit_pal.ui.calculator.formattedFactor
import com.tfg.kaifit_pal.ui.calculator.labelRes
import com.tfg.kaifit_pal.ui.components.formatDecimalInput
import com.tfg.kaifit_pal.ui.components.labelRes
import com.tfg.kaifit_pal.ui.results.formatSignedPercent
import com.tfg.kaifit_pal.ui.results.goalLabelRes
import com.tfg.kaifit_pal.ui.results.labelRes
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import com.tfg.kaifit_pal.ui.theme.Spacing
import com.tfg.kaifit_pal.ui.theme.withTabularFigures
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Profile tab: the last saved measurement and result.
 *
 * @param onGoToCalculator opens the calculator tab when nothing is saved yet.
 * @param onRecalculate opens the calculator filled with the saved measurements.
 */
@Composable
fun ProfileScreen(
    onGoToCalculator: () -> Unit,
    onRecalculate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(
        state = uiState,
        onGoToCalculator = onGoToCalculator,
        onRecalculate = onRecalculate,
        onClearProfile = viewModel::clearProfile,
        modifier = modifier,
    )
}

/**
 * Stateless profile screen. "Borrar datos" asks for confirmation before calling [onClearProfile].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    state: ProfileUiState,
    onGoToCalculator: () -> Unit,
    onRecalculate: () -> Unit,
    onClearProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), modifier = Modifier.semantics { heading() }) },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        when (state) {
            ProfileUiState.Loading -> {
                val loadingDescription = stringResource(R.string.profile_loading)
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
                }
            }

            ProfileUiState.Empty -> EmptyProfile(onGoToCalculator = onGoToCalculator, modifier = contentModifier)

            is ProfileUiState.Content -> SavedProfileDetails(
                profile = state.profile,
                onRecalculate = onRecalculate,
                onClear = { showClearDialog = true },
                modifier = contentModifier,
            )
        }
    }

    if (showClearDialog && state is ProfileUiState.Content) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearProfile()
                    },
                ) {
                    Text(stringResource(R.string.profile_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.profile_clear_cancel))
                }
            },
            title = { Text(stringResource(R.string.profile_clear_title)) },
            text = { Text(stringResource(R.string.profile_clear_message)) },
        )
    }
}

@Composable
private fun EmptyProfile(onGoToCalculator: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.profile_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.profile_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onGoToCalculator, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.profile_go_to_calculator))
        }
    }
}

@Composable
private fun SavedProfileDetails(
    profile: SavedProfile,
    onRecalculate: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale).withZone(ZoneId.systemDefault())
    }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        MeasurementCard(profile, locale)
        ResultCard(profile)
        Text(
            text = stringResource(R.string.profile_saved_at, dateFormatter.format(profile.savedAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onRecalculate,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.profile_recalculate))
        }
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(R.string.profile_clear))
        }
    }
}

/** Sex, age, height, weight, the circumferences that were entered and the activity level. */
@Composable
private fun MeasurementCard(profile: SavedProfile, locale: Locale) {
    val m = profile.measurements
    fun cm(value: Double) = formatDecimalInput(value, locale)
    InfoCard(title = stringResource(R.string.profile_measurement_title)) {
        InfoRow(stringResource(R.string.calculator_sex), stringResource(m.sex.labelRes))
        InfoRow(stringResource(R.string.calculator_age), pluralStringResource(R.plurals.value_age_years, m.ageYears, m.ageYears))
        InfoRow(stringResource(R.string.calculator_height), stringResource(R.string.value_cm, cm(m.heightCm)))
        InfoRow(stringResource(R.string.calculator_weight), stringResource(R.string.value_kg, cm(m.weightKg)))
        m.neckCm?.let { InfoRow(stringResource(R.string.calculator_neck), stringResource(R.string.value_cm, cm(it))) }
        m.waistCm?.let { InfoRow(stringResource(R.string.calculator_waist), stringResource(R.string.value_cm, cm(it))) }
        if (m.sex == Sex.FEMALE) {
            m.hipCm?.let { InfoRow(stringResource(R.string.calculator_hip), stringResource(R.string.value_cm, cm(it))) }
        }
        InfoRow(
            stringResource(R.string.calculator_activity_level),
            stringResource(
                R.string.activity_level_with_factor,
                stringResource(profile.activityLevel.labelRes),
                profile.activityLevel.formattedFactor(locale),
            ),
        )
    }
}

/** TDEE, method, body fat percentage, goal and macros, as they were saved. */
@Composable
private fun ResultCard(profile: SavedProfile) {
    val tdee = profile.tdee
    val plan = profile.macroPlan
    InfoCard(title = stringResource(R.string.profile_result_title)) {
        InfoRow(stringResource(R.string.profile_tdee), stringResource(R.string.value_kcal_per_day, tdee.tdeeKcal))
        InfoRow(stringResource(R.string.profile_method), stringResource(tdee.method.labelRes))
        InfoRow(
            stringResource(R.string.profile_body_fat),
            tdee.bodyFatPercent?.let { stringResource(R.string.value_body_fat_percent, it) }
                ?: stringResource(R.string.body_fat_not_estimated),
        )
        InfoRow(
            stringResource(R.string.profile_goal),
            stringResource(
                R.string.profile_goal_value,
                stringResource(goalLabelRes(profile.goalModifier)),
                formatSignedPercent(profile.goalModifier.percent),
            ),
        )
        InfoRow(stringResource(R.string.profile_target), stringResource(R.string.value_kcal_per_day, plan.targetKcal))
        InfoRow(stringResource(R.string.macro_protein), stringResource(R.string.value_grams, plan.proteinGrams))
        InfoRow(stringResource(R.string.macro_fat), stringResource(R.string.value_grams, plan.fatGrams))
        InfoRow(stringResource(R.string.macro_carbs), stringResource(R.string.value_grams, plan.carbGrams))
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}

/**
 * A label and its value on one line, read together by TalkBack.
 *
 * The label takes at most half of the row and the value takes the rest, aligned to the end. On a
 * narrow screen or with large text, a long label wraps between words in its half, and a short
 * label leaves more room to the value, so a long value such as a goal name also wraps between
 * words.
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Text(
            text = label,
            modifier = Modifier
                .atMostHalfOfRow(gap = Spacing.l)
                .alignByBaseline(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .alignByBaseline(),
            style = MaterialTheme.typography.bodyLarge.withTabularFigures(),
            textAlign = TextAlign.End,
        )
    }
}

/**
 * Measures the content with at most half of the width that its row offers, after the [gap]
 * between the two halves.
 */
private fun Modifier.atMostHalfOfRow(gap: Dp): Modifier = layout { measurable, constraints ->
    val maxWidth = if (constraints.hasBoundedWidth) {
        ((constraints.maxWidth - gap.roundToPx()) / 2).coerceAtLeast(0)
    } else {
        constraints.maxWidth
    }
    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = maxWidth))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** Fixed example states for previews and screenshots. */
internal object ProfilePreviewStates {

    /** Man of 30 years, 180 cm, 80 kg, neck 38 and waist 85 cm, moderately active, saved at −15 %. */
    val saved: ProfileUiState.Content by lazy {
        val measurements = BodyMeasurements(
            sex = Sex.MALE,
            ageYears = 30,
            heightCm = 180.0,
            weightKg = 80.0,
            neckCm = 38.0,
            waistCm = 85.0,
        )
        val tdee = checkNotNull(TdeeCalculator.calculate(measurements, ActivityLevel.MODERATE))
        val modifier = GoalModifier(-15)
        val plan = MacroPlan.forGoal(modifier.applyTo(tdee.tdeeKcal), modifier.goal)
        ProfileUiState.Content(
            SavedProfile(measurements, ActivityLevel.MODERATE, tdee, modifier, plan, Instant.parse("2026-10-09T19:30:00Z")),
        )
    }
}

@PreviewLightDark
@Composable
private fun ProfileContentEmptyPreview() {
    KaiFitTheme {
        ProfileContent(state = ProfileUiState.Empty, onGoToCalculator = {}, onRecalculate = {}, onClearProfile = {})
    }
}

@PreviewLightDark
@Composable
private fun ProfileContentSavedPreview() {
    KaiFitTheme {
        ProfileContent(state = ProfilePreviewStates.saved, onGoToCalculator = {}, onRecalculate = {}, onClearProfile = {})
    }
}
