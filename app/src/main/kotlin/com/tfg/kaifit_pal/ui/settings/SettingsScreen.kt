package com.tfg.kaifit_pal.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tfg.kaifit_pal.BuildConfig
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.data.settings.ThemeMode
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import com.tfg.kaifit_pal.ui.theme.Spacing

/** Settings tab: appearance, the assistant model (placeholder) and information about the app. */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        state = uiState,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDynamicColorChange = viewModel::onDynamicColorChange,
        modifier = modifier,
    )
}

/**
 * Stateless settings screen.
 *
 * @param versionName version shown in the "Acerca de" section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = BuildConfig.VERSION_NAME,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.settings_title), modifier = Modifier.semantics { heading() })
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = Spacing.s),
        ) {
            SectionTitle(R.string.settings_section_appearance)
            Text(
                text = stringResource(R.string.settings_theme),
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    ThemeModeRow(
                        mode = mode,
                        selected = mode == state.settings.themeMode,
                        onClick = { onThemeModeChange(mode) },
                    )
                }
            }
            DynamicColorRow(
                checked = state.dynamicColorAvailable && state.settings.dynamicColor,
                available = state.dynamicColorAvailable,
                onCheckedChange = onDynamicColorChange,
            )

            HorizontalDivider(modifier = Modifier.padding(top = Spacing.s))
            SectionTitle(R.string.settings_section_model)
            Card(
                modifier = Modifier
                    .padding(horizontal = Spacing.screenHorizontal)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.settings_model_coming_soon),
                    modifier = Modifier.padding(Spacing.l),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(top = Spacing.l))
            SectionTitle(R.string.settings_section_about)
            Column(
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.settings_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.settings_about_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.m)
            .semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** One option of the theme radio group; the whole row selects it. */
@Composable
private fun ThemeModeRow(mode: ThemeMode, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = Spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The row handles the click, so the radio button itself is not clickable.
        RadioButton(selected = selected, onClick = null)
        Text(text = stringResource(mode.labelRes), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Dynamic colour switch; the whole row toggles it. It is disabled before Android 12. */
@Composable
private fun DynamicColorRow(checked: Boolean, available: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(value = checked, enabled = available, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.l),
        ) {
            Text(text = stringResource(R.string.settings_dynamic_color), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(
                    if (available) {
                        R.string.settings_dynamic_color_description
                    } else {
                        R.string.settings_dynamic_color_unavailable
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The row handles the click, so the switch itself is not clickable.
        Switch(checked = checked, onCheckedChange = null, enabled = available)
    }
}

@get:StringRes
private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

@PreviewLightDark
@Composable
private fun SettingsContentPreview() {
    KaiFitTheme {
        SettingsContent(
            state = SettingsUiState(AppSettings(themeMode = ThemeMode.DARK), dynamicColorAvailable = true),
            onThemeModeChange = {},
            onDynamicColorChange = {},
        )
    }
}
