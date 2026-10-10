package com.tfg.kaifit_pal.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.domain.TdeeCalculator
import com.tfg.kaifit_pal.ui.components.MacroCard
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import com.tfg.kaifit_pal.ui.theme.Spacing
import com.tfg.kaifit_pal.ui.theme.withTabularFigures
import kotlinx.coroutines.launch

/**
 * Results detail destination: TDEE, goal and macros of the measurements of its route.
 *
 * @param onBack returns to the calculator.
 * @param onAskAssistant opens the Kai-Q tab.
 */
@Composable
fun ResultsScreen(
    onBack: () -> Unit,
    onAskAssistant: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultsViewModel = viewModel(factory = ResultsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.results_saved)
    val saveErrorMessage = stringResource(R.string.results_save_error)
    val saveStatus = (uiState as? ResultsUiState.Ready)?.saveStatus

    // The outcome of saving is shown once: the snackbar runs in its own scope, so marking the
    // status as shown, which restarts this effect, does not dismiss it.
    LaunchedEffect(saveStatus) {
        val message = when (saveStatus) {
            SaveStatus.Saved -> savedMessage
            SaveStatus.Error -> saveErrorMessage
            else -> null
        }
        if (message != null) {
            snackbarScope.launch { snackbarHostState.showSnackbar(message) }
            viewModel.onSaveStatusShown()
        }
    }

    ResultsContent(
        state = uiState,
        onBack = onBack,
        onGoalChange = viewModel::onGoalSliderChange,
        onProteinChange = viewModel::onProteinChange,
        onFatChange = viewModel::onFatChange,
        onSave = viewModel::onSave,
        onAskAssistant = onAskAssistant,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/** Stateless results screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsContent(
    state: ResultsUiState,
    onBack: () -> Unit,
    onGoalChange: (Float) -> Unit,
    onProteinChange: (Int) -> Unit,
    onFatChange: (Int) -> Unit,
    onSave: () -> Unit,
    onAskAssistant: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.results_title), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            when (state) {
                ResultsUiState.InvalidMeasurements -> Text(
                    text = stringResource(R.string.results_invalid_measurements),
                    style = MaterialTheme.typography.bodyLarge,
                )

                is ResultsUiState.Ready -> {
                    TdeeHeroCard(state)
                    GoalSection(state, onGoalChange)
                    MacroSection(state, onProteinChange, onFatChange)
                    Button(
                        onClick = onSave,
                        enabled = state.saveStatus != SaveStatus.Saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.results_save))
                    }
                    OutlinedButton(
                        onClick = onAskAssistant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.results_ask_assistant))
                    }
                }
            }
        }
    }
}

/** TDEE in large tabular figures, with the method and the body fat percentage. */
@Composable
private fun TdeeHeroCard(state: ResultsUiState.Ready) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = stringResource(R.string.results_tdee_label), style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.tdee.tdeeKcal.toString(),
                    style = MaterialTheme.typography.displayMedium.withTabularFigures(),
                )
                Spacer(modifier = Modifier.width(Spacing.s))
                Text(
                    text = stringResource(R.string.results_kcal_per_day),
                    modifier = Modifier.padding(bottom = Spacing.s),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(R.string.results_method, stringResource(state.tdee.method.labelRes)),
                style = MaterialTheme.typography.bodyMedium,
            )
            val bodyFat = state.tdee.bodyFatPercent
            Text(
                text = if (bodyFat != null) {
                    stringResource(R.string.results_body_fat, stringResource(R.string.value_body_fat_percent, bodyFat))
                } else {
                    stringResource(R.string.results_body_fat_not_estimated)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Goal slider from −20 % to +20 % in steps of 5, with the goal name and the target calories. */
@Composable
private fun GoalSection(state: ResultsUiState.Ready, onGoalChange: (Float) -> Unit) {
    val goalLabel = stringResource(goalLabelRes(state.goalModifier))
    val targetText = stringResource(R.string.value_kcal_per_day, state.targetKcal)
    val sliderDescription = stringResource(R.string.results_goal_slider)
    val sliderState = stringResource(R.string.results_goal_state, goalLabel, targetText)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = stringResource(R.string.results_goal_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        // The target calories keep their width on one line, at the end of the row; the chip takes
        // what is left and wraps its label if it has to.
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                GoalChip(label = goalLabel)
            }
            Text(
                text = targetText,
                style = MaterialTheme.typography.titleMedium.withTabularFigures(),
                softWrap = false,
            )
        }
        Slider(
            value = state.goalModifier.percent.toFloat(),
            onValueChange = onGoalChange,
            valueRange = GoalModifier.MIN_PERCENT.toFloat()..GoalModifier.MAX_PERCENT.toFloat(),
            // 9 positions: −20, −15, …, +20.
            steps = 7,
            modifier = Modifier.semantics {
                contentDescription = sliderDescription
                stateDescription = sliderState
            },
        )
        Text(
            text = stringResource(R.string.results_goal_adjustment, formatSignedPercent(state.goalModifier.percent)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The goal name drawn as a chip. It is only a label: Material chips are always clickable, and a
 * click that does nothing would mislead TalkBack users.
 */
@Composable
private fun GoalChip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Protein and fat with sliders, carbohydrates as the remainder, and the resulting total. */
@Composable
private fun MacroSection(state: ResultsUiState.Ready, onProteinChange: (Int) -> Unit, onFatChange: (Int) -> Unit) {
    val plan = state.macroPlan
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(
            text = stringResource(R.string.results_macros_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        MacroCard(
            name = stringResource(R.string.macro_protein),
            grams = plan.proteinGrams,
            kcalPerGram = MacroPlan.KCAL_PER_GRAM_PROTEIN,
            targetKcal = state.targetKcal,
            maxGrams = state.targetKcal / MacroPlan.KCAL_PER_GRAM_PROTEIN,
            onGramsChange = onProteinChange,
        )
        MacroCard(
            name = stringResource(R.string.macro_fat),
            grams = plan.fatGrams,
            kcalPerGram = MacroPlan.KCAL_PER_GRAM_FAT,
            targetKcal = state.targetKcal,
            maxGrams = state.targetKcal / MacroPlan.KCAL_PER_GRAM_FAT,
            onGramsChange = onFatChange,
        )
        MacroCard(
            name = stringResource(R.string.macro_carbs),
            grams = plan.carbGrams,
            kcalPerGram = MacroPlan.KCAL_PER_GRAM_CARB,
            targetKcal = state.targetKcal,
        )
        Text(
            text = stringResource(R.string.results_macro_total, plan.totalKcal, state.targetKcal),
            style = MaterialTheme.typography.bodyMedium.withTabularFigures(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Fixed example states for previews and screenshots. */
internal object ResultsPreviewStates {

    /** Man of 30 years, 180 cm, 80 kg, neck 38 cm and waist 85 cm, moderately active, at −15 %. */
    val manAtModerateHighDeficit: ResultsUiState.Ready by lazy {
        val measurements = BodyMeasurements(Sex.MALE, ageYears = 30, heightCm = 180.0, weightKg = 80.0, neckCm = 38.0, waistCm = 85.0)
        val tdee = checkNotNull(TdeeCalculator.calculate(measurements, ActivityLevel.MODERATE))
        val modifier = GoalModifier(-15)
        val plan = MacroPlan.forGoal(modifier.applyTo(tdee.tdeeKcal), modifier.goal)
        ResultsUiState.Ready(tdee, modifier, plan.targetKcal, plan, SaveStatus.Idle)
    }
}

@PreviewLightDark
@Composable
private fun ResultsContentPreview() {
    KaiFitTheme {
        ResultsContent(
            state = ResultsPreviewStates.manAtModerateHighDeficit,
            onBack = {},
            onGoalChange = {},
            onProteinChange = {},
            onFatChange = {},
            onSave = {},
            onAskAssistant = {},
        )
    }
}
