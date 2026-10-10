package com.tfg.kaifit_pal.ui.help

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.ui.components.ExpandableCard
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import com.tfg.kaifit_pal.ui.theme.Spacing

/** A frequently asked question and its answer. */
private data class HelpEntry(@StringRes val questionRes: Int, @StringRes val answerRes: Int)

/** The questions, in display order. */
private val HelpEntries = listOf(
    HelpEntry(R.string.help_q_waist, R.string.help_a_waist),
    HelpEntry(R.string.help_q_neck, R.string.help_a_neck),
    HelpEntry(R.string.help_q_hip, R.string.help_a_hip),
    HelpEntry(R.string.help_q_tdee, R.string.help_a_tdee),
    HelpEntry(R.string.help_q_formulas, R.string.help_a_formulas),
    HelpEntry(R.string.help_q_macros, R.string.help_a_macros),
    HelpEntry(R.string.help_q_assistant, R.string.help_a_assistant),
    HelpEntry(R.string.help_q_privacy, R.string.help_a_privacy),
    HelpEntry(R.string.help_q_disclaimer, R.string.help_a_disclaimer),
)

/** Help tab: frequently asked questions. Its content is static, so it needs no ViewModel. */
@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    HelpContent(modifier = modifier)
}

/**
 * The list of questions; each one shows its answer when tapped. Which answers are shown is
 * saved, so it survives rotation and switching tabs.
 *
 * @param initiallyExpanded string ids of the questions shown with their answer at first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpContent(
    modifier: Modifier = Modifier,
    initiallyExpanded: Set<Int> = emptySet(),
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title), modifier = Modifier.semantics { heading() }) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            items(HelpEntries, key = { it.questionRes }) { entry ->
                var expanded by rememberSaveable { mutableStateOf(entry.questionRes in initiallyExpanded) }
                ExpandableCard(
                    question = stringResource(entry.questionRes),
                    answer = stringResource(entry.answerRes),
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                )
            }
        }
    }
}

/** String id of the first question, shown expanded in previews and screenshots. */
internal val FirstHelpQuestion: Int = R.string.help_q_waist

@PreviewLightDark
@Composable
private fun HelpContentPreview() {
    KaiFitTheme {
        HelpContent(initiallyExpanded = setOf(FirstHelpQuestion))
    }
}
