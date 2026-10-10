package com.tfg.kaifit_pal.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tfg.kaifit_pal.BuildConfig
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.data.llm.DeviceCheck
import com.tfg.kaifit_pal.data.llm.DownloadWait
import com.tfg.kaifit_pal.data.llm.ModelSpec
import com.tfg.kaifit_pal.data.llm.ModelState
import com.tfg.kaifit_pal.data.llm.UriImportSource
import com.tfg.kaifit_pal.data.settings.AppSettings
import com.tfg.kaifit_pal.data.settings.InferenceBackend
import com.tfg.kaifit_pal.data.settings.ThemeMode
import com.tfg.kaifit_pal.ui.components.ModelPreviewStates
import com.tfg.kaifit_pal.ui.components.formatGigabytes
import com.tfg.kaifit_pal.ui.components.labelRes
import com.tfg.kaifit_pal.ui.components.messageRes
import com.tfg.kaifit_pal.ui.components.progressPercent
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import com.tfg.kaifit_pal.ui.theme.Spacing
import java.util.Locale

/**
 * Settings tab: appearance, the model of Kai-Q (state, download, import, deletion, processor
 * and Wi-Fi-only download) and information about the app with its licences.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleStartEffect(viewModel) {
        viewModel.onScreenStarted()
        onStopOrDispose {}
    }
    val contentResolver = LocalContext.current.contentResolver
    // ".litertlm" has no registered MIME type, so every file is offered; the header is checked.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onImportModel(UriImportSource(contentResolver, uri))
    }
    SettingsContent(
        state = uiState,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDynamicColorChange = viewModel::onDynamicColorChange,
        onInferenceBackendChange = viewModel::onInferenceBackendChange,
        onDownloadOnWifiOnlyChange = viewModel::onDownloadOnWifiOnlyChange,
        onDownloadModel = viewModel::onDownloadModel,
        onImportModel = { importLauncher.launch(arrayOf("*/*")) },
        onCancelModelTransfer = viewModel::onCancelModelTransfer,
        onDeleteModel = viewModel::onDeleteModel,
        onDismissModelError = viewModel::onDismissModelError,
        modifier = modifier,
    )
}

/**
 * Stateless settings screen. While the settings load it shows only an indicator, never the
 * default values. "Borrar modelo" asks for confirmation before calling [onDeleteModel].
 *
 * @param onImportModel opens the file picker.
 * @param versionName version shown in the "Acerca de" section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onInferenceBackendChange: (InferenceBackend) -> Unit,
    onDownloadOnWifiOnlyChange: (Boolean) -> Unit,
    onDownloadModel: () -> Unit,
    onImportModel: () -> Unit,
    onCancelModelTransfer: () -> Unit,
    onDeleteModel: () -> Unit,
    onDismissModelError: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = BuildConfig.VERSION_NAME,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]

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
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        when (state) {
            SettingsUiState.Loading -> {
                val loadingDescription = stringResource(R.string.settings_loading)
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loadingDescription })
                }
            }

            is SettingsUiState.Ready -> Column(
                modifier = contentModifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.s),
            ) {
                SectionTitle(R.string.settings_section_appearance)
                GroupLabel(R.string.settings_theme)
                Column(modifier = Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        RadioRow(
                            label = stringResource(mode.labelRes),
                            selected = mode == state.settings.themeMode,
                            onClick = { onThemeModeChange(mode) },
                        )
                    }
                }
                SwitchRow(
                    title = stringResource(R.string.settings_dynamic_color),
                    description = stringResource(
                        if (state.dynamicColorAvailable) {
                            R.string.settings_dynamic_color_description
                        } else {
                            R.string.settings_dynamic_color_unavailable
                        },
                    ),
                    checked = state.dynamicColorAvailable && state.settings.dynamicColor,
                    enabled = state.dynamicColorAvailable,
                    onCheckedChange = onDynamicColorChange,
                )

                HorizontalDivider(modifier = Modifier.padding(top = Spacing.s))
                SectionTitle(R.string.settings_section_model)
                ModelCard(
                    model = state.model,
                    device = state.device,
                    locale = locale,
                    onDownload = onDownloadModel,
                    onImport = onImportModel,
                    onCancel = onCancelModelTransfer,
                    onDelete = { showDeleteDialog = true },
                    onDismissError = onDismissModelError,
                )
                GroupLabel(R.string.settings_backend, modifier = Modifier.padding(top = Spacing.l))
                Column(modifier = Modifier.selectableGroup()) {
                    InferenceBackend.entries.forEach { backend ->
                        RadioRow(
                            label = stringResource(backend.labelRes),
                            selected = backend == state.settings.inferenceBackend,
                            onClick = { onInferenceBackendChange(backend) },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_backend_help),
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_wifi_only),
                    description = stringResource(
                        R.string.settings_wifi_only_description,
                        formatGigabytes(ModelSpec.SIZE_BYTES, locale),
                    ),
                    checked = state.settings.downloadOnWifiOnly,
                    enabled = true,
                    onCheckedChange = onDownloadOnWifiOnlyChange,
                )

                HorizontalDivider(modifier = Modifier.padding(top = Spacing.s))
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
                    Text(
                        text = stringResource(R.string.settings_license_model),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.settings_license_engine),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showDeleteDialog && state is SettingsUiState.Ready && state.model is ModelState.Downloaded) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteModel()
                    },
                ) {
                    Text(stringResource(R.string.settings_model_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.settings_model_delete_cancel))
                }
            },
            title = { Text(stringResource(R.string.settings_model_delete_title)) },
            text = {
                Text(stringResource(R.string.settings_model_delete_message, formatGigabytes(ModelSpec.SIZE_BYTES, locale)))
            },
        )
    }
}

/** State, free space and the actions that fit the state of the model. */
@Composable
private fun ModelCard(
    model: ModelState,
    device: DeviceCheck?,
    locale: Locale,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onDismissError: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = Spacing.screenHorizontal)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Text(
                text = stringResource(R.string.settings_model_name),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = model.statusText(locale),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
                color = if (model is ModelState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            when (model) {
                is ModelState.Downloading -> LinearProgressIndicator(
                    progress = { progressPercent(model.downloadedBytes, model.totalBytes) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )

                is ModelState.Importing -> {
                    val total = model.totalBytes
                    if (total != null && total > 0) {
                        LinearProgressIndicator(
                            progress = { progressPercent(model.copiedBytes, total) / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                else -> Unit
            }
            if (device != null) {
                Text(
                    text = stringResource(R.string.model_free_space, formatGigabytes(device.availableStorageBytes, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (model) {
                ModelState.Checking -> Unit

                ModelState.NotDownloaded -> NotDownloadedActions(device, locale, onDownload, onImport)

                is ModelState.Downloading, is ModelState.Importing -> CardButton(R.string.model_cancel, outlined = true, onClick = onCancel)

                is ModelState.Downloaded -> OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.settings_model_delete))
                }

                is ModelState.Failed -> CardButton(R.string.model_ok, outlined = false, onClick = onDismissError)
            }
        }
    }
}

@Composable
private fun NotDownloadedActions(device: DeviceCheck?, locale: Locale, onDownload: () -> Unit, onImport: () -> Unit) {
    if (device != null && !device.supports64Bit) {
        Text(
            text = stringResource(R.string.model_unsupported_device),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        return
    }
    if (device != null && !device.enoughStorageForDownload) {
        Text(
            text = stringResource(R.string.model_free_space_missing, formatGigabytes(device.missingStorageBytes, locale)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
    CardButton(
        textRes = R.string.model_download,
        outlined = false,
        enabled = device?.enoughStorageForDownload ?: true,
        onClick = onDownload,
    )
    CardButton(R.string.model_import, outlined = true, onClick = onImport)
}

@Composable
private fun CardButton(@StringRes textRes: Int, outlined: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val buttonModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
    if (outlined) {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = buttonModifier) { Text(stringResource(textRes)) }
    } else {
        Button(onClick = onClick, enabled = enabled, modifier = buttonModifier) { Text(stringResource(textRes)) }
    }
}

/** One line on the state of the model: "No descargado", "Descargando 42 %", "Descargado · 2,6 GB"… */
@Composable
private fun ModelState.statusText(locale: Locale): String = when (this) {
    ModelState.Checking -> stringResource(R.string.settings_model_checking)
    ModelState.NotDownloaded -> stringResource(R.string.settings_model_not_downloaded)
    is ModelState.Downloading -> when (waiting) {
        null -> stringResource(R.string.settings_model_downloading, progressPercent(downloadedBytes, totalBytes))
        DownloadWait.WIFI -> stringResource(R.string.settings_model_waiting_wifi)
        else -> stringResource(waiting.labelRes)
    }
    is ModelState.Importing -> stringResource(R.string.settings_model_importing)
    is ModelState.Downloaded -> stringResource(R.string.model_downloaded, formatGigabytes(sizeBytes, locale))
    is ModelState.Failed -> stringResource(error.messageRes)
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

@Composable
private fun GroupLabel(@StringRes textRes: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(textRes),
        modifier = modifier.padding(horizontal = Spacing.screenHorizontal),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** One option of a radio group; the whole row selects it. */
@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
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
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** A switch with its title and description; the whole row toggles it. */
@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.l),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The row handles the click, so the switch itself is not clickable.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@get:StringRes
private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

@get:StringRes
private val InferenceBackend.labelRes: Int
    get() = when (this) {
        InferenceBackend.GPU -> R.string.settings_backend_gpu
        InferenceBackend.CPU -> R.string.settings_backend_cpu
    }

/** Fixed states for previews and screenshots: the model downloaded on a phone with room for it. */
internal object SettingsPreviewStates {

    val modelDownloaded = SettingsUiState.Ready(
        settings = AppSettings.DEFAULT,
        dynamicColorAvailable = true,
        model = ModelPreviewStates.downloaded,
        device = ModelPreviewStates.roomyDevice,
    )

    val modelNotDownloaded = modelDownloaded.copy(model = ModelState.NotDownloaded)

    val modelDownloading = modelDownloaded.copy(model = ModelPreviewStates.downloading)
}

/** [SettingsContent] with no-op callbacks, for previews and screenshots. */
@Composable
internal fun SettingsPreview(state: SettingsUiState) {
    SettingsContent(
        state = state,
        onThemeModeChange = {},
        onDynamicColorChange = {},
        onInferenceBackendChange = {},
        onDownloadOnWifiOnlyChange = {},
        onDownloadModel = {},
        onImportModel = {},
        onCancelModelTransfer = {},
        onDeleteModel = {},
        onDismissModelError = {},
    )
}

@PreviewLightDark
@Composable
private fun SettingsContentPreview() {
    KaiFitTheme {
        SettingsPreview(SettingsPreviewStates.modelDownloaded)
    }
}
